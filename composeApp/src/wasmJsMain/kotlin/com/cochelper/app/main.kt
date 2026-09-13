package com.cochelper.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cochelper.app.data.WebFileStore
import com.cochelper.app.data.WebSettingsStore
import com.cochelper.app.data.local.InMemoryRepository
import com.cochelper.app.data.sync.GitHubSync
import com.cochelper.app.di.AppContainer
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // 启动时先异步从 IndexedDB 回填文件/照片等大字段，再渲染，避免首帧闪空。
    MainScope().launch {
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
            chasePointDao = repo.chasePointDao,
            settings = WebSettingsStore(),
            github = GitHubSync(),
            files = WebFileStore(),
            backupStore = repo,
        )
        ComposeViewport(document.body!!) {
            App(container)
        }
    }
}
