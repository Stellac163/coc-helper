package com.cochelper.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cochelper.app.data.WebFileStore
import com.cochelper.app.data.WebSettingsStore
import com.cochelper.app.data.local.InMemoryRepository
import com.cochelper.app.data.sync.GitHubSync
import com.cochelper.app.di.AppContainer
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val repo = InMemoryRepository.load()
    val container = AppContainer(
        moduleDao = repo.moduleDao,
        timelineDao = repo.timelineDao,
        locationDao = repo.locationDao,
        npcDao = repo.npcDao,
        pcDao = repo.pcDao,
        clueDao = repo.clueDao,
        fileDao = repo.fileDao,
        combatantDao = repo.combatantDao,
        chaseDao = repo.chaseDao,
        settings = WebSettingsStore(),
        github = GitHubSync(),
        files = WebFileStore(),
    )
    ComposeViewport(document.body!!) {
        App(container)
    }
}
