package com.cochelper.app.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64

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

    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(this@GitHubSync.json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
    }

    private fun authHeader(token: String) = "Bearer $token"

    /** 校验 token 并返回登录用户信息。 */
    suspend fun getUser(token: String): Result<GithubUser> = runCatching {
        client.get("https://api.github.com/user") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
            header("X-GitHub-Api-Version", "2022-11-28")
        }.let { resp ->
            if (!resp.status.isSuccess()) error("HTTP ${resp.status.value}")
            resp.body<GithubUser>()
        }
    }

    /** 确保私有仓库存在，返回 owner/repo 形式。 */
    suspend fun ensureRepo(token: String, repoName: String): Result<String> = runCatching {
        val user = getUser(token).getOrThrow()
        val owner = user.login
        val getResp = client.get("https://api.github.com/repos/$owner/$repoName") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
        }
        if (!getResp.status.isSuccess()) {
            // 不存在则创建私有仓库
            val createResp = client.post("https://api.github.com/user/repos") {
                header("Authorization", authHeader(token))
                header("Accept", "application/vnd.github+json")
                contentType(ContentType.Application.Json)
                setBody(
                    GithubRepo(
                        name = repoName,
                        private = true,
                        auto_init = true,
                        description = "CocHelper 跑团助手同步数据"
                    )
                )
            }
            if (!createResp.status.isSuccess()) error("创建仓库失败 HTTP ${createResp.status.value}")
        }
        "$owner/$repoName"
    }

    private val backupPath = "cochelper_backup.json"

    suspend fun pushBackup(token: String, repoFullName: String, payload: BackupPayload): Result<Unit> =
        runCatching {
            val encoded = Base64.getEncoder().encodeToString(
                json.encodeToString(BackupPayload.serializer(), payload).toByteArray()
            )
            // Contents API 的 content（base64）上限约 1MB；超过则改走 Git Data API。
            if (encoded.length < 900_000) pushSmall(token, repoFullName, encoded)
            else pushLargeWithRetry(token, repoFullName, encoded)
        }

    /** 把非 2xx 的响应连同响应体一起抛出，便于定位（如 tree 422 的具体原因）。 */
    private suspend fun fail(step: String, resp: HttpResponse): Nothing =
        error("$step HTTP ${resp.status.value}：${resp.bodyAsText().take(200)}")

    /** 小文件（≤1MB）直接走 Contents API。 */
    private suspend fun pushSmall(token: String, repoFullName: String, encoded: String) {
        val existing = fetchContent(token, repoFullName, backupPath)
        val resp = client.put("https://api.github.com/repos/$repoFullName/contents/$backupPath") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
            contentType(ContentType.Application.Json)
            setBody(
                GithubPutBody(
                    message = "sync: CocHelper backup",
                    content = encoded,
                    sha = existing?.sha,
                )
            )
        }
        if (!resp.status.isSuccess()) fail("上传", resp)
    }

    /** 大文件推送带重试：多端并发 push 时 ref 会被抢先推进，重试会重新拉取最新 ref 再提交。 */
    private suspend fun pushLargeWithRetry(token: String, repoFullName: String, encoded: String) {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                pushLarge(token, repoFullName, encoded)
                return
            } catch (t: Throwable) {
                lastError = t
                println("[pushLarge] 第 ${attempt + 1} 次尝试失败：${t.message}")
                if (attempt < 2) delay(1200L * (attempt + 1))
            }
        }
        throw lastError ?: IllegalStateException("pushLarge 失败")
    }

    /** 大文件（>1MB）走 Git Data API：blob → tree → commit → 更新 main 分支。 */
    private suspend fun pushLarge(token: String, repoFullName: String, encoded: String) {
        // 1. 创建 blob
        val blobResp = client.post("https://api.github.com/repos/$repoFullName/git/blobs") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
            contentType(ContentType.Application.Json)
            setBody(GitBlobBody(content = encoded))
        }
        if (!blobResp.status.isSuccess()) fail("创建 blob", blobResp)
        val blobSha = blobResp.body<GitShaResponse>().sha

        // 2. 取 main 分支当前 commit
        val refResp = client.get("https://api.github.com/repos/$repoFullName/git/refs/heads/main") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
        }
        if (!refResp.status.isSuccess()) fail("读取分支", refResp)
        val parentSha = refResp.body<GitRefResponse>().`object`.sha

        // 3. 取 commit 的 tree
        val commitResp = client.get("https://api.github.com/repos/$repoFullName/git/commits/$parentSha") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
        }
        if (!commitResp.status.isSuccess()) fail("读取提交", commitResp)
        val baseTree = commitResp.body<GitCommitInfoResponse>().tree.sha

        // 4. 建 tree（替换/新增 backupPath）
        val treeResp = client.post("https://api.github.com/repos/$repoFullName/git/trees") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
            contentType(ContentType.Application.Json)
            setBody(
                GitTreeBody(
                    baseTree = baseTree,
                    tree = listOf(GitTreeEntry(path = backupPath, sha = blobSha))
                )
            )
        }
        if (!treeResp.status.isSuccess()) fail("创建 tree（blob=$blobSha baseTree=$baseTree）", treeResp)
        val treeSha = treeResp.body<GitShaResponse>().sha

        // 5. 建 commit
        val newCommitResp = client.post("https://api.github.com/repos/$repoFullName/git/commits") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
            contentType(ContentType.Application.Json)
            setBody(GitCommitBody(message = "sync: CocHelper backup", tree = treeSha, parents = listOf(parentSha)))
        }
        if (!newCommitResp.status.isSuccess()) fail("创建提交", newCommitResp)
        val newCommitSha = newCommitResp.body<GitShaResponse>().sha

        // 6. 更新 main 分支 ref
        val updateResp = client.patch("https://api.github.com/repos/$repoFullName/git/refs/heads/main") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
            contentType(ContentType.Application.Json)
            setBody(GitUpdateRefBody(sha = newCommitSha))
        }
        if (!updateResp.status.isSuccess()) fail("更新分支", updateResp)
    }

    suspend fun pullBackup(token: String, repoFullName: String): Result<BackupPayload> = runCatching {
        readBackup(token, repoFullName) ?: error("云端尚未有备份数据")
    }

    /**
     * 读取并解析云端备份。content 字段为空时有两种情况：
     * - 空文件（0 字节）→ 视为「无备份」，返回 null；
     * - 超过 1MB 的大文件（GitHub 不在 content 里返回内容）→ 用 Git Data API 的 blob 端点取内容。
     */
    private suspend fun readBackup(token: String, repoFullName: String): BackupPayload? {
        val file = fetchContent(token, repoFullName, backupPath) ?: return null
        val raw = if (file.content.isNullOrBlank()) {
            val sha = file.sha
            if (sha == null || file.size == null || file.size == 0L) return null
            fetchBlobContent(token, repoFullName, sha)
        } else {
            String(Base64.getDecoder().decode(file.content!!))
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
     * 不走 Contents API 的 raw 媒体类型——那对 >1MB 文件会退回 JSON 元数据，导致解析成空备份。
     */
    private suspend fun fetchBlobContent(token: String, repoFullName: String, sha: String): String {
        val resp: HttpResponse = client.get("https://api.github.com/repos/$repoFullName/git/blobs/$sha") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
        }
        if (!resp.status.isSuccess()) error("读取云端备份 blob 失败 HTTP ${resp.status.value}")
        val blob = resp.body<GitBlobResponse>()
        return String(Base64.getDecoder().decode(blob.content))
    }

    private suspend fun fetchContent(token: String, repoFullName: String, path: String): GithubContent? {
        val resp: HttpResponse = client.get("https://api.github.com/repos/$repoFullName/contents/$path") {
            header("Authorization", authHeader(token))
            header("Accept", "application/vnd.github+json")
        }
        if (resp.status == HttpStatusCode.NotFound) return null
        if (!resp.status.isSuccess()) error("读取失败 HTTP ${resp.status.value}")
        return resp.body<GithubContent>()
    }

    fun close() {
        client.close()
    }
}
