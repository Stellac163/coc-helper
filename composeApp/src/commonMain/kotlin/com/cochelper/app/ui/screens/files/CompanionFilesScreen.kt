package com.cochelper.app.ui.screens.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cochelper.app.data.local.FileEntity
import com.cochelper.app.ui.LocalContainer
import com.cochelper.app.ui.components.EmptyState
import com.cochelper.app.ui.components.FileListRow
import com.cochelper.app.ui.components.FileViewer
import com.cochelper.app.ui.components.SectionTopBar
import com.cochelper.app.ui.components.groupedCornerShape
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanionFilesScreen(moduleId: Long, navController: NavHostController) {
    val container = LocalContainer.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val files by container.fileDao.observeForModuleKind(moduleId, FileEntity.FILE_COMPANION)
        .collectAsState(emptyList())

    var viewing by remember { mutableStateOf<FileEntity?>(null) }
    var deleting by remember { mutableStateOf<FileEntity?>(null) }

    fun pick() {
        scope.launch {
            val picked = container.files.pickFile(arrayOf("*/*"))
            if (picked != null) {
                val base64 = picked.dataUrl.substringAfter("base64,", "")
                container.fileDao.insert(
                    FileEntity(
                        moduleId = moduleId,
                        name = picked.name,
                        mimeType = picked.mimeType,
                        contentBase64 = base64,
                        sizeBytes = picked.sizeBytes,
                        kind = FileEntity.FILE_COMPANION,
                    )
                )
                snackbar.showSnackbar("已添加：${picked.name}")
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            SectionTopBar(
                title = "配套组件",
                leftIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onLeftClick = { navController.navigateUp() },
                rightIcon = Icons.Filled.CloudUpload,
                onRightClick = { pick() },
            )
        },
    ) { padding ->
        if (files.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                EmptyState("还没有配套文件，点击右上角上传", icon = Icons.Filled.InsertDriveFile)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                files.forEachIndexed { index, file ->
                    FileListRow(
                        file = file,
                        shape = groupedCornerShape(index, files.size),
                        onOpen = { viewing = file },
                        onDownload = {
                            scope.launch { container.files.downloadFile(file.name, file.mimeType, file.contentBase64) }
                        },
                        onDelete = { deleting = file },
                    )
                }
            }
        }
    }

    viewing?.let { file -> FileViewer(file = file, fileStore = container.files, onDismiss = { viewing = null }) }

    deleting?.let { file ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("删除文件") },
            text = { Text("确定要删除“${file.name}”吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { container.fileDao.delete(file) }
                    deleting = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }
}
