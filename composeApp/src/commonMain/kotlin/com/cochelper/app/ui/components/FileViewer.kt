package com.cochelper.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cochelper.app.data.FileStore
import com.cochelper.app.data.local.FileEntity
import com.cochelper.app.platform.base64Decode
import kotlinx.coroutines.launch

/** 全屏查看文件：图片 / 文本内置预览，其它类型提供下载或新标签页打开。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewer(file: FileEntity, fileStore: FileStore, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    title = { Text(file.name, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "关闭")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                FileContent(file = file, fileStore = fileStore, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** 内嵌文件内容（图片 / 文本 / 其它类型）。按扩展名兜底识别，避免浏览器给出空 mimeType 时误判。 */
@Composable
fun FileContent(file: FileEntity, fileStore: FileStore, modifier: Modifier = Modifier) {
    val name = file.name.lowercase()
    val isImage = file.mimeType.startsWith("image/") ||
        name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
        name.endsWith(".gif") || name.endsWith(".webp") || name.endsWith(".bmp")
    val isText = file.mimeType.startsWith("text/") ||
        name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".json") || name.endsWith(".csv")
    val isPdf = file.mimeType == "application/pdf" || name.endsWith(".pdf")

    Box(modifier) {
        when {
            isImage -> PlatformImage(
                model = dataUri(file),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            isText -> TextFileView(file)
            isPdf -> PdfContent(file, fileStore)
            else -> UnsupportedView(file, fileStore)
        }
    }
}

/** PDF：用浏览器原生阅读器打开（新标签页）+ 下载。 */
@Composable
private fun PdfContent(file: FileEntity, fileStore: FileStore) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            file.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { scope.launch { fileStore.openInNewTab(file.name, "application/pdf", file.contentBase64) } },
                shape = RoundedCornerShape(28.dp),
            ) { Text("新标签页打开") }
            FilledTonalButton(
                onClick = { scope.launch { fileStore.downloadFile(file.name, "application/pdf", file.contentBase64) } },
                shape = RoundedCornerShape(28.dp),
            ) { Text("下载") }
        }
    }
}

private fun dataUri(file: FileEntity) = "data:${file.mimeType};base64,${file.contentBase64}"

@Composable
private fun TextFileView(file: FileEntity) {
    val text = remember(file.contentBase64) {
        runCatching { base64Decode(file.contentBase64).decodeToString() }.getOrDefault("无法读取文本内容")
    }
    Text(
        text = text,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun UnsupportedView(file: FileEntity, fileStore: FileStore) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        Text(
            "此类型暂不支持内置预览",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(
                onClick = { scope.launch { fileStore.openInNewTab(file.name, file.mimeType, file.contentBase64) } },
                shape = RoundedCornerShape(28.dp),
            ) { Text("新标签页打开") }
            FilledTonalButton(
                onClick = { scope.launch { fileStore.downloadFile(file.name, file.mimeType, file.contentBase64) } },
                shape = RoundedCornerShape(28.dp),
            ) { Text("下载") }
        }
    }
}
