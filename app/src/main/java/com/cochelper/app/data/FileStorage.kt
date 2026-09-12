package com.cochelper.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileStorage {

    private fun uploadDir(context: Context): File {
        val dir = File(context.filesDir, "uploads")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** 将 Uri 指向的文件复制到应用内部存储，返回绝对路径。 */
    fun copyToInternal(context: Context, uri: Uri, name: String): String {
        val safeName = sanitize(name)
        val target = File(uploadDir(context), safeName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
        return target.absolutePath
    }

    fun delete(path: String) {
        runCatching { File(path).delete() }
    }

    fun exists(path: String): Boolean = path.isNotBlank() && File(path).exists()

    fun uriForFile(context: Context, path: String): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))

    /** 将文件保存到系统下载目录（API 29+ 使用 MediaStore）。返回是否成功。 */
    fun downloadToDownloads(context: Context, path: String, name: String): Boolean {
        return runCatching {
            val source = File(path)
            if (!source.exists()) return false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeFor(name))
                    put(
                        android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                    )
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    source.inputStream().use { it.copyTo(out) }
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                dir.mkdirs()
                source.copyTo(File(dir, name), overwrite = true)
            }
            true
        }.getOrDefault(false)
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.').lowercase()) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "mp3" -> "audio/mpeg"
        "wav" -> "audio/wav"
        "mp4" -> "video/mp4"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }

    private fun sanitize(name: String): String {
        val base = name.substringAfterLast('/').substringAfterLast('\\').ifBlank { "file" }
        return base.replace(Regex("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]"), "_")
    }
}
