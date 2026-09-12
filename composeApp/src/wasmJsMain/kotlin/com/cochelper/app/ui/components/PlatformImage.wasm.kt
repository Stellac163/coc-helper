package com.cochelper.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.cochelper.app.platform.base64Decode
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.jetbrains.skia.Image as SkiaImage
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.fetch.Response

@Composable
actual fun PlatformImage(model: String, modifier: Modifier, contentScale: ContentScale) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, model) {
        value = runCatching {
            val bytes: ByteArray? = when {
                model.startsWith("data:") -> {
                    val idx = model.indexOf("base64,")
                    if (idx < 0) null else base64Decode(model.substring(idx + 7))
                }
                model.startsWith("http://") || model.startsWith("https://") -> {
                    val response: Response = window.fetch(model).await()
                    val ab = response.arrayBuffer().await<ArrayBuffer>()
                    Int8Array(ab).toByteArray()
                }
                else -> null
            }
            bytes?.let { SkiaImage.makeFromEncoded(it)?.toComposeImageBitmap() }
        }.getOrNull()
    }
    val b = bitmap
    if (b != null) {
        Image(bitmap = b, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}
