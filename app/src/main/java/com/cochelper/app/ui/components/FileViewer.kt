package com.cochelper.app.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.cochelper.app.data.local.FileEntity
import java.io.File

/** 全屏查看文件：图片 / PDF / 音频 / 文本，其它类型用外部应用打开。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewer(file: FileEntity, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val context = LocalContext.current
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
                FileContent(file = file, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

/** 内嵌文件内容（图片 / PDF / 音频 / 文本 / 外部打开）。 */
@Composable
fun FileContent(file: FileEntity, modifier: Modifier = Modifier) {
    Box(modifier) {
        when {
            file.mimeType.startsWith("image/") -> AsyncImage(
                model = File(file.localPath),
                contentDescription = file.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
            file.mimeType == "application/pdf" -> PdfViewer(path = file.localPath)
            file.mimeType.startsWith("audio/") -> AudioPlayer(path = file.localPath)
            file.mimeType.startsWith("text/") -> TextFileView(path = file.localPath)
            else -> OpenExternally(file = file)
        }
    }
}

@Composable
private fun PdfViewer(path: String) {
    var pageCount by remember { mutableIntStateOf(0) }
    val renderer = remember(path) {
        runCatching {
            val fd = ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(fd)
        }.getOrNull()
    }
    DisposableEffect(renderer) {
        onDispose { renderer?.close() }
    }
    pageCount = renderer?.pageCount ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(pageCount) { index ->
            val bitmap by produceState<Bitmap?>(initialValue = null, renderer, index) {
                value = renderer?.openPage(index)?.use { page ->
                    val bmp = Bitmap.createBitmap(
                        (page.width * 1.0f).toInt(),
                        (page.height * 1.0f).toInt(),
                        Bitmap.Config.ARGB_8888,
                    )
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "第 ${index + 1} 页",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    }
}

@Composable
private fun AudioPlayer(path: String) {
    val context = LocalContext.current
    val player = remember(path) { runCatching { MediaPlayer() }.getOrNull() }
    var playing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IconButton(
            onClick = {
                if (playing) {
                    player?.pause()
                    playing = false
                } else {
                    runCatching {
                        if (player != null && !player!!.isPlaying) {
                            player.reset()
                            player.setDataSource(path)
                            player.prepare()
                            player.start()
                            playing = true
                        }
                    }
                }
            },
            modifier = Modifier.size(96.dp),
        ) {
            Icon(
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TextFileView(path: String) {
    val text = remember(path) { runCatching { File(path).readText() }.getOrDefault("无法读取文本内容") }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenExternally(file: FileEntity) {
    val context = LocalContext.current
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
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        Text(
            "此类型暂不支持内置预览",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        FilledTonalButton(
            onClick = {
                runCatching {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        val uri = com.cochelper.app.data.FileStorage.uriForFile(context, file.localPath)
                        setDataAndType(uri, file.mimeType)
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }
            },
            shape = RoundedCornerShape(28.dp),
        ) { Text("用外部应用打开") }
    }
}
