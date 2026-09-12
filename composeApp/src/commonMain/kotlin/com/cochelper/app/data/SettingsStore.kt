package com.cochelper.app.data

import com.cochelper.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val githubToken: String = "",
    val repoName: String = "coc-helper-backup",
    val nickname: String = "",
    val avatarUri: String = "",
)

interface SettingsStore {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setGithubToken(token: String)
    suspend fun setRepoName(name: String)
    suspend fun setNickname(nickname: String)
    suspend fun setAvatarUri(uri: String)
}
