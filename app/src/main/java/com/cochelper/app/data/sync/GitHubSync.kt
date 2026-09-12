package com.cochelper.app.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
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
            val content = Base64.getEncoder().encodeToString(
                json.encodeToString(BackupPayload.serializer(), payload).toByteArray()
            )
            val existing = fetchContent(token, repoFullName, backupPath)
            val resp = client.put("https://api.github.com/repos/$repoFullName/contents/$backupPath") {
                header("Authorization", authHeader(token))
                header("Accept", "application/vnd.github+json")
                contentType(ContentType.Application.Json)
                setBody(
                    GithubPutBody(
                        message = "sync: CocHelper backup",
                        content = content,
                        sha = existing?.sha,
                    )
                )
            }
            if (!resp.status.isSuccess()) error("上传失败 HTTP ${resp.status.value}")
        }

    suspend fun pullBackup(token: String, repoFullName: String): Result<BackupPayload> = runCatching {
        val content = fetchContent(token, repoFullName, backupPath)
            ?: error("云端尚未有备份数据")
        val decoded = String(Base64.getDecoder().decode(content.content.orEmpty()))
        json.decodeFromString(BackupPayload.serializer(), decoded)
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
