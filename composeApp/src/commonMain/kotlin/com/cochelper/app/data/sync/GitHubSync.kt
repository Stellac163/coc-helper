package com.cochelper.app.data.sync

import com.cochelper.app.platform.base64Decode
import com.cochelper.app.platform.base64Encode
import com.cochelper.app.platform.httpRequest
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
)

@Serializable
data class GithubPutBody(
    val message: String,
    val content: String,
    val sha: String? = null,
    val branch: String = "main",
)

class GitHubSync {

    private val json = Json { ignoreUnknownKeys = true }

    private fun headers(token: String): Map<String, String> = mapOf(
        "Authorization" to "Bearer $token",
        "Accept" to "application/vnd.github+json",
        "X-GitHub-Api-Version" to "2022-11-28",
        "Content-Type" to "application/json",
    )

    /** 校验 token 并返回登录用户信息。 */
    suspend fun getUser(token: String): Result<GithubUser> = runCatching {
        val resp = httpRequest("GET", "https://api.github.com/user", headers(token))
        if (resp.status !in 200..299) error("HTTP ${resp.status}")
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

    suspend fun pushBackup(token: String, repoFullName: String, payload: BackupPayload): Result<Unit> =
        runCatching {
            val content = base64Encode(json.encodeToString(BackupPayload.serializer(), payload).encodeToByteArray())
            val existing = fetchContent(token, repoFullName, backupPath)
            val body = json.encodeToString(
                GithubPutBody.serializer(),
                GithubPutBody(
                    message = "sync: CocHelper backup",
                    content = content,
                    sha = existing?.sha,
                )
            )
            val resp = httpRequest(
                "PUT",
                "https://api.github.com/repos/$repoFullName/contents/$backupPath",
                headers(token),
                body
            )
            if (resp.status !in 200..299) error("上传失败 HTTP ${resp.status}")
        }

    suspend fun pullBackup(token: String, repoFullName: String): Result<BackupPayload> = runCatching {
        val content = fetchContent(token, repoFullName, backupPath)
            ?: error("云端尚未有备份数据")
        val decoded = base64Decode(content.content.orEmpty()).decodeToString()
        json.decodeFromString(BackupPayload.serializer(), decoded)
    }

    private suspend fun fetchContent(token: String, repoFullName: String, path: String): GithubContent? {
        val resp = httpRequest("GET", "https://api.github.com/repos/$repoFullName/contents/$path", headers(token))
        if (resp.status == 404) return null
        if (resp.status !in 200..299) error("读取失败 HTTP ${resp.status}")
        return json.decodeFromString(GithubContent.serializer(), resp.body)
    }
}
