package com.cochelper.app.ui.screens.original

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cochelper.app.data.FileStorage
import com.cochelper.app.data.local.FileEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.FileContent
import com.cochelper.app.ui.components.SectionTopBar
import com.cochelper.app.ui.navigation.Routes
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalDocScreen(moduleId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    val originals by container.fileDao.observeForModuleKind(moduleId, FileEntity.FILE_ORIGINAL)
        .collectAsStateWithLifecycle(emptyList())
    val original = originals.firstOrNull()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val (name, mime) = com.cochelper.app.ui.components.queryFileMeta(context, uri)
                val path = FileStorage.copyToInternal(context, uri, name)
                val size = File(path).length()
                // 替换旧原文
                originals.forEach { container.fileDao.delete(it) }
                container.fileDao.insert(
                    FileEntity(
                        moduleId = moduleId,
                        name = name,
                        mimeType = mime,
                        localPath = path,
                        sizeBytes = size,
                        kind = FileEntity.FILE_ORIGINAL,
                    )
                )
                container.moduleDao.getById(moduleId)?.let { m ->
                    container.moduleDao.update(m.copy(hasOriginalDoc = true))
                }
                snackbar.showSnackbar("已上传原文：$name")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SectionTopBar(
                title = "原文",
                leftIcon = Icons.Filled.Home,
                onLeftClick = { navController.navigateUp() },
                rightIcon = Icons.Filled.CloudUpload,
                onRightClick = {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        )
                    )
                },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = { navController.navigate(Routes.files(moduleId)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) { Icon(Icons.Filled.FolderOpen, contentDescription = "配套组件") }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (original == null) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    EmptyState("尚未上传原文，点击右上角上传 PDF 或 Word 文档", icon = Icons.Filled.Description)
                }
            } else {
                FileContent(file = original, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
