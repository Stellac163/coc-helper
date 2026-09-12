package com.cochelper.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.cochelper.app.data.local.FileEntity

fun fileTypeIcon(mimeType: String): ImageVector = when {
    mimeType.startsWith("image/") -> Icons.Filled.Image
    mimeType.startsWith("audio/") -> Icons.Filled.AudioFile
    mimeType.startsWith("video/") -> Icons.Filled.InsertDriveFile
    mimeType == "application/pdf" || mimeType.contains("word") || mimeType.contains("document") -> Icons.Filled.Description
    mimeType.startsWith("text/") -> Icons.Filled.Description
    else -> Icons.Filled.InsertDriveFile
}

private fun round1(v: Double): String {
    val r = kotlin.math.round(v * 10) / 10.0
    val i = r.toLong()
    return if (r == i.toDouble()) "$i" else "$r"
}

fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${round1(bytes / 1024.0)} KB"
    else -> "${round1(bytes / (1024.0 * 1024.0))} MB"
}

@Composable
fun FileListRow(
    file: FileEntity,
    shape: androidx.compose.ui.graphics.Shape,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    GroupedListItem(
        shape = shape,
        icon = fileTypeIcon(file.mimeType),
        title = file.name,
        subtitle = humanSize(file.sizeBytes),
        trailing = {
            Row {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "下载",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        onClick = onOpen,
    )
}
