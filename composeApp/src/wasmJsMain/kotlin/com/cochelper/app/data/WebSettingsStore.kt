package com.cochelper.app.data

import com.cochelper.app.ui.theme.ThemeMode
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

class WebSettingsStore : SettingsStore {

    private val state = MutableStateFlow(load())
    override val settings: Flow<AppSettings> = state

    private fun persist() {
        window.localStorage.setItem(KEY, Json.encodeToString(AppSettings.serializer(), state.value))
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        state.update { it.copy(themeMode = mode) }
        persist()
    }

    override suspend fun setGithubToken(token: String) {
        state.update { it.copy(githubToken = token) }
        persist()
    }

    override suspend fun setRepoName(name: String) {
        state.update { it.copy(repoName = name) }
        persist()
    }

    override suspend fun setNickname(nickname: String) {
        state.update { it.copy(nickname = nickname) }
        persist()
    }

    override suspend fun setAvatarUri(uri: String) {
        state.update { it.copy(avatarUri = uri) }
        persist()
    }

    companion object {
        private const val KEY = "cochelper_settings_v1"

        private fun load(): AppSettings = runCatching {
            val raw = window.localStorage.getItem(KEY)
            if (raw.isNullOrBlank()) {
                AppSettings()
            } else {
                Json { ignoreUnknownKeys = true }.decodeFromString(AppSettings.serializer(), raw)
            }
        }.getOrElse { AppSettings() }
    }
}
