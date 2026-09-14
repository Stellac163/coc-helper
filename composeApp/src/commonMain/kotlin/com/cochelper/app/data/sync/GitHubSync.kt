package com.cochelper.app.data.sync

import com.cochelper.app.platform.HttpPhase
import com.cochelper.app.platform.HttpProgress
import com.cochelper.app.platform.HttpResult
import com.cochelper.app.platform.base64Decode
import com.cochelper.app.platform.base64Encode
import com.cochelper.app.platform.httpRequest
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GithubUser(
    val login: String = "",
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String = "",
)

@Serializable
data class GithubRepo(
    val name: String = "",
    val private: Boolean = false,
    val auto_init: Boolean = false,
    val description: String = "",
)

@Serializable
data class GithubContent(
    val sha: String? = null,
    val content: String? = null,
    val size: Long? = null,
)

@Serializable
data class GithubPutBody(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String = "main",
)

// —— Git Data API（用于 >1MB 大文件的读写，绕过 Contents API 的 1MB 限制）——
@Serializable
data class GitShaResponse(val sha: String)

@Serializable
data class GitBlobResponse(
    val content: String,
    val encoding: String = "base64",
)

@Serializable
data class GitRefResponse(val `object`: GitShaResponse)

@Serializable
data class GitCommitInfoResponse(val sha: String, val tree: GitShaResponse)

@Serializable
data class GitBlobBody(
    val content: String,
    // encoding 默认值也必须发出去：GitHub 默认按 utf-8 存，会损坏 base64 内容
    @EncodeDefault val encoding: String = "base64",
)

@Serializable
data class GitTreeEntry(
    val path: String,
    // mode/type 默认值也必须发出去，否则 GitHub 报 "Must supply a valid tree.mode"
    @EncodeDefault val mode: String = "100644",
    @EncodeDefault val type: String = "blob",
    val sha: String,
)

@Serializable
data class GitTreeBody(
    @SerialName("base_tree") val baseTree: String,
    val tree: List<GitTreeEntry>,
)

@Serializable
data class GitCommitBody(val message: String, val tree: String, val parents: List<String>)

@Serializable
data class GitUpdateRefBody(val sha: String, val force: Boolean = false)

class GitHubSync {

    private val json = Json { ignoreUnknownKeys = true }

    private fun headers(token: String): Map<String, String> {
        // 令牌只能是可见 ASCII；剔除误粘入的空格/换行/全角/不可见字符，
        // 否则浏览器 Headers.append 会抛 "non ISO-8859-1 code point"
        val clean = token.filter { it.code in 0x21..0x7E }
        return mapOf(
            "Authorization" to "Bearer $clean",
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
            "Content-Type" to "application/json",
        )
    }

    /** 校验 token 并返回登录用户信息。 */
    suspend fun getUser(token: String): Result<GithubUser> = runCatching {
        val resp = httpRequest("GET", "https://api.github.com/user", headers(token))
        if (resp.status !in 200..299) {
            val hint = if (resp.status == 401) "令牌无效或已过期，请重新生成并粘贴" else "HTTP ${resp.status}"
            error("$hint：${resp.body.take(200)}")
        }
        json.decodeFromString(GithubUser.serializer(), resp.body)
    }

    /** 确保私有仓库存在，返回 owner/repo 形式。 */
    suspend fun ensureRepo(token: String, repoName: String): Result<String> = runCatching {
        val user = getUser(token).getOrThrow()
        val owner = user.login
        val getResp = httpRequest("GET", "https://api.github.com/repos/$owner/$repoName", headers(token))
        if (getResp.status !in 200..299) {
            val createBody = json.encodeToString(
                GithubRepo.serializer(),
                GithubRepo(
                    name = repoName,
                    private = true,
                    auto_init = true,
                    description = "CocHelper 跑团助手同步数据"
                )
            )
            val createResp = httpRequest("POST", "https://api.github.com/user/repos", headers(token), createBody)
            if (createResp.status !in 200..299) error("创建仓库失败 HTTP ${createResp.status}")
        }
        "$owner/$repoName"
    }

    private val backupPath = "cochelper_backup.json"

    /** 只保留指定阶段（上传/下载）的进度回调，避免把响应的微小下载进度混进上传进度条（反之亦然）。 */
    private fun ((HttpProgress) -> Unit)?.only(phase: HttpPhase): ((HttpProgress) -> Unit)? =
        this?.let { cb -> { p -> if (p.phase == phase) cb(p) } }

    suspend fun pushBackup(
        token: String,
        repoFullName: String,
        payload: BackupPayload,
        onProgress: ((HttpProgress) -> Unit)? = null,
    ): Result<Unit> =
        runCatching {
            val encoded = base64Encode(json.encodeToString(BackupPayload.serializer(), payload).encodeToByteArray())
            val up = onProgress.only(HttpPhase.UPLOAD)
            // Contents API 的 content（base64）上限约 1MB；超过则改走 Git Data API。
            if (encoded.length < 900_000) pushSmall(token, repoFullName, encoded, up)
            else pushLargeWithRetry(token, repoFullName, encoded, up)
        }

    /** 把非 2xx 的响应连同响应体一起抛出，便于定位（如 tree 422 的具体原因）。 */
    private fun fail(step: String, resp: HttpResult): Nothing =
        error("$step HTTP ${resp.status}：${resp.body.take(200)}")

    /** 小文件（≤1MB）直接走 Contents API。 */
    private suspend fun pushSmall(token: String, repoFullName: String, encoded: String, onProgress: ((HttpProgress) -> Unit)?) {
        val existing = fetchContent(token, repoFullName, backupPath)
        val body = json.encodeToString(
            GithubPutBody.serializer(),
            GithubPutBody(
                message = "sync: CocHelper backup",
                content = encoded,
                sha = existing?.sha,
            )
        )
        val resp = httpRequest(
            "PUT",
            "https://api.github.com/repos/$repoFullName/contents/$backupPath",
            headers(token),
            body,
            onProgress,
        )
        if (resp.status !in 200..299) fail("上传", resp)
    }

    /** 大文件推送带重试：多端并发 push 时 ref 会被抢先推进，重试会重新拉取最新 ref 再提交。 */
    private suspend fun pushLargeWithRetry(token: String, repoFullName: String, encoded: String, onProgress: ((HttpProgress) -> Unit)?) {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                pushLarge(token, repoFullName, encoded, onProgress)
                return
            } catch (t: Throwable) {
                lastError = t
                println("[pushLarge] 第 ${attempt + 1} 次尝试失败：${t.message}")
                if (attempt < 2) kotlinx.coroutines.delay(1200L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("pushLarge 失败")
    }

    /** 大文件（>1MB）走 Git Data API：blob → tree → commit → 更新 main 分支。 */
    private suspend fun pushLarge(token: String, repoFullName: String, encoded: String, onProgress: ((HttpProgress) -> Unit)?) {
        // 1. 创建 blob
        val blobResp = httpRequest(
            "POST",
            "https://api.github.com/repos/$repoFullName/git/blobs",
            headers(token),
            json.encodeToString(GitBlobBody.serializer(), GitBlobBody(content = encoded)),
            onProgress,
        )
        if (blobResp.status !in 200..299) fail("创建 blob", blobResp)
        val blobSha = json.decodeFromString(GitShaResponse.serializer(), blobResp.body).sha

        // 2. 取 main 分支当前 commit
        val refResp = httpRequest("GET", "https://api.github.com/repos/$repoFullName/git/refs/heads/main", headers(token))
        if (refResp.status !in 200..299) fail("读取分支", refResp)
        val parentSha = json.decodeFromString(GitRefResponse.serializer(), refResp.body).`object`.sha

        // 3. 取 commit 的 tree
        val commitResp = httpRequest("GET", "https://api.github.com/repos/$repoFullName/git/commits/$parentSha", headers(token))
        if (commitResp.status !in 200..299) fail("读取提交", commitResp)
        val baseTree = json.decodeFromString(GitCommitInfoResponse.serializer(), commitResp.body).tree.sha

        // 4. 建 tree（替换/新增 backupPath）
        val treeResp = httpRequest(
            "POST",
            "https://api.github.com/repos/$repoFullName/git/trees",
            headers(token),
            json.encodeToString(
                GitTreeBody.serializer(),
                GitTreeBody(baseTree = baseTree, tree = listOf(GitTreeEntry(path = backupPath, sha = blobSha)))
            )
        )
        if (treeResp.status !in 200..299) fail("创建 tree（blob=$blobSha baseTree=$baseTree）", treeResp)
        val treeSha = json.decodeFromString(GitShaResponse.serializer(), treeResp.body).sha

        // 5. 建 commit
        val newCommitResp = httpRequest(
            "POST",
            "https://api.github.com/repos/$repoFullName/git/commits",
            headers(token),
            json.encodeToString(GitCommitBody.serializer(), GitCommitBody(message = "sync: CocHelper backup", tree = treeSha, parents = listOf(parentSha)))
        )
        if (newCommitResp.status !in 200..299) fail("创建提交", newCommitResp)
        val newCommitSha = json.decodeFromString(GitShaResponse.serializer(), newCommitResp.body).sha

        // 6. 更新 main 分支 ref
        val updateResp = httpRequest(
            "PATCH",
            "https://api.github.com/repos/$repoFullName/git/refs/heads/main",
            headers(token),
            json.encodeToString(GitUpdateRefBody.serializer(), GitUpdateRefBody(sha = newCommitSha))
        )
        if (updateResp.status !in 200..299) fail("更新分支", updateResp)
    }

    suspend fun pullBackup(
        token: String,
        repoFullName: String,
        onProgress: ((HttpProgress) -> Unit)? = null,
    ): Result<BackupPayload> = runCatching {
        readBackup(token, repoFullName, onProgress.only(HttpPhase.DOWNLOAD)) ?: error("云端尚未有备份数据")
    }

    /** 拉取云端快照；云端还没有备份文件时返回 null（供合并用，而非报错）。 */
    suspend fun fetchBackup(token: String, repoFullName: String): BackupPayload? =
        readBackup(token, repoFullName)

    /**
     * 读取并解析云端备份。content 字段为空时有两种情况：
     * - 空文件（0 字节）→ 视为「无备份」，返回 null；
     * - 超过 1MB 的大文件（GitHub 不在 content 里返回内容）→ 用 Git Data API 的 blob 端点取内容。
     */
    private suspend fun readBackup(token: String, repoFullName: String, onProgress: ((HttpProgress) -> Unit)? = null): BackupPayload? {
        val file = fetchContent(token, repoFullName, backupPath, onProgress) ?: return null
        val raw = if (file.content.isNullOrBlank()) {
            val sha = file.sha
            if (sha == null || file.size == null || file.size == 0L) return null
            fetchBlobContent(token, repoFullName, sha, onProgress)
        } else {
            base64Decode(file.content!!).decodeToString()
        }
        if (raw.isBlank()) return null
        return try {
            json.decodeFromString(BackupPayload.serializer(), raw)
        } catch (t: Throwable) {
            throw IllegalArgumentException("云端备份解析失败（size=${file.size}）：${t.message}")
        }
    }

    /**
     * 用 Git Data API 的 blob 端点读大文件：content 字段始终以 base64 返回（≤100MB）。
     * 不走 Contents API 的 raw 媒体类型——那对 >1MB 文件会随 Accept 头是否被识别而退回
     * JSON 元数据，元数据被 ignoreUnknownKeys 静默解析成空 BackupPayload，导致本地被抹空。
     */
    private suspend fun fetchBlobContent(token: String, repoFullName: String, sha: String, onProgress: ((HttpProgress) -> Unit)? = null): String {
        val resp = httpRequest(
            "GET",
            "https://api.github.com/repos/$repoFullName/git/blobs/$sha",
            headers(token),
            onProgress = onProgress,
        )
        if (resp.status !in 200..299) error("读取云端备份 blob 失败 HTTP ${resp.status}")
        val blob = json.decodeFromString(GitBlobResponse.serializer(), resp.body)
        return base64Decode(blob.content).decodeToString()
    }

    private suspend fun fetchContent(
        token: String,
        repoFullName: String,
        path: String,
        onProgress: ((HttpProgress) -> Unit)? = null,
    ): GithubContent? {
        val resp = httpRequest("GET", "https://api.github.com/repos/$repoFullName/contents/$path", headers(token), onProgress = onProgress)
        if (resp.status == 404) return null
        if (resp.status !in 200..299) error("读取失败 HTTP ${resp.status}")
        return json.decodeFromString(GithubContent.serializer(), resp.body)
    }
}
