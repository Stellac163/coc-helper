package com.cochelper.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cochelper.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val githubToken: String = "",
    val repoName: String = "coc-helper-backup",
    val nickname: String = "",
    val avatarUri: String = "",
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val REPO_NAME = stringPreferencesKey("repo_name")
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME_MODE] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            githubToken = p[Keys.GITHUB_TOKEN] ?: "",
            repoName = p[Keys.REPO_NAME] ?: "coc-helper-backup",
            nickname = p[Keys.NICKNAME] ?: "",
            avatarUri = p[Keys.AVATAR_URI] ?: "",
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setGithubToken(token: String) {
        context.dataStore.edit { it[Keys.GITHUB_TOKEN] = token }
    }

    suspend fun setRepoName(name: String) {
        context.dataStore.edit { it[Keys.REPO_NAME] = name }
    }

    suspend fun setNickname(nickname: String) {
        context.dataStore.edit { it[Keys.NICKNAME] = nickname }
    }

    suspend fun setAvatarUri(uri: String) {
        context.dataStore.edit { it[Keys.AVATAR_URI] = uri }
    }
}
