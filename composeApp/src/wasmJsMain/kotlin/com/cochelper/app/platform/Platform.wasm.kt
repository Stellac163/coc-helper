package com.cochelper.app.platform

import kotlin.js.JsArray
import kotlin.js.JsAny
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.fetch.Response
import org.w3c.xhr.XMLHttpRequest

@OptIn(ExperimentalEncodingApi::class)
actual fun base64Encode(bytes: ByteArray): String = Base64.Default.encode(bytes)

@OptIn(ExperimentalEncodingApi::class)
actual fun base64Decode(s: String): ByteArray =
    // GitHub Contents API 返回的 content 是带换行的 base64（每 60 字符折行一次），
    // 严格解码器遇到换行会报 "prohibited after the pad character"；
    // 先剔除所有空白再解码（对齐 JS 的 atob 行为，空白会被忽略）。
    Base64.Default.decode(s.filterNot { it.isWhitespace() })

@OptIn(kotlin.time.ExperimentalTime::class)
actual fun currentTimeMillis(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()

actual fun nextId(): Long {
    while (true) {
        val id = kotlin.random.Random.nextLong() and Long.MAX_VALUE
        if (id != 0L) return id
    }
}

actual suspend fun compressImageDataUrl(dataUrl: String, maxDimension: Int, quality: Double): String {
    if (!dataUrl.startsWith("data:image/")) return dataUrl
    return try {
        val idx = dataUrl.indexOf("base64,")
        if (idx < 0) return dataUrl
        val bytes = base64Decode(dataUrl.substring(idx + 7))
        val image = Image.makeFromEncoded(bytes) ?: return dataUrl
        val w = image.width
        val h = image.height
        if (w <= 0 || h <= 0) return dataUrl
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(w, h).toFloat())
        val nw = maxOf(1, (w * scale).toInt())
        val nh = maxOf(1, (h * scale).toInt())
        // 已足够小且本身就是 JPEG 时才保留原图；否则（PNG 等、或需缩放）一律重编码为 JPEG 压体积
        if (nw == w && nh == h && dataUrl.startsWith("data:image/jpeg")) return dataUrl
        val surface = Surface.makeRasterN32Premul(nw, nh)
        surface.canvas.drawImageRect(image, Rect.makeWH(nw.toFloat(), nh.toFloat()))
        val data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, (quality * 100).toInt().coerceIn(0, 100)) ?: return dataUrl
        val out = "data:image/jpeg;base64," + Base64.Default.encode(data.bytes)
        // 压缩后反而更大（极小图等）则保留原图
        if (out.length >= dataUrl.length) dataUrl else out
    } catch (t: Throwable) {
        println("[compressImage] 压缩失败，返回原图：${t.message}")
        dataUrl
    }
}

actual suspend fun httpRequest(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?,
    onProgress: ((HttpProgress) -> Unit)?,
): HttpResult {
    // 用 XMLHttpRequest 而非 fetch：fetch 拿不到上传进度（upload.onprogress），
    // 而 XHR 同时提供上传（upload.onprogress）与下载（onprogress）进度事件。
    println("[http] $method $url")
    return suspendCancellableCoroutine { cont ->
        val xhr = XMLHttpRequest()
        xhr.open(method, url)
        headers.forEach { (k, v) -> xhr.setRequestHeader(k, v) }

        xhr.onload = {
            cont.resume(HttpResult(xhr.status.toInt(), xhr.responseText))
        }
        xhr.onerror = {
            // 断网 / CORS 拦截等网络级失败只在这里触发；HTTP 4xx/5xx 走 onload 由上层判断。
            cont.resumeWith(Result.failure(RuntimeException("fetch $method $url 失败（网络错误或被 CORS 拦截）")))
        }
        xhr.onabort = {
            cont.resumeWith(Result.failure(RuntimeException("fetch $method $url 已中止")))
        }

        if (onProgress != null) {
            xhr.upload.onprogress = { e ->
                if (e.lengthComputable) {
                    onProgress(HttpProgress(HttpPhase.UPLOAD, e.loaded.toDouble().toLong(), e.total.toDouble().toLong()))
                }
            }
            xhr.onprogress = { e ->
                if (e.lengthComputable) {
                    onProgress(HttpProgress(HttpPhase.DOWNLOAD, e.loaded.toDouble().toLong(), e.total.toDouble().toLong()))
                }
            }
        }

        cont.invokeOnCancellation { xhr.abort() }

        xhr.send(body ?: "")
    }
}

actual fun hideAppLoadingOverlay() {
    document.getElementById("app-loading")?.let { el ->
        el.parentNode?.removeChild(el)
    }
}

actual suspend fun toBlobUrl(mimeType: String, base64: String): String {
    val dataUrl = "data:$mimeType;base64,$base64"
    // 优先：base64 解码成字节后同步构造 Blob（字节不损坏、不依赖 fetch）。
    // Chrome 的 PDF 阅读器在 iframe 里只认 blob: URL，不认 data: URL。
    return try {
        val bytes = base64Decode(base64)
        val buffer = ArrayBuffer(bytes.size)
        val view = DataView(buffer)
        for (i in bytes.indices) {
            view.setUint8(i, bytes[i])
        }
        val parts = JsArray<JsAny?>()
        parts[0] = buffer
        val blob = Blob(parts, BlobPropertyBag(type = mimeType))
        val url = URL.createObjectURL(blob)
        println("[toBlobUrl] blob ok mime=$mimeType bytes=${bytes.size} url=${url.take(48)}")
        url
    } catch (t: Throwable) {
        // 兜底 1：fetch data: URL → blob（浏览器标准做法）
        try {
            val response: Response = window.fetch(dataUrl).await()
            val blob: Blob = response.blob().await()
            val url = URL.createObjectURL(blob)
            println("[toBlobUrl] fetch ok mime=$mimeType url=${url.take(48)}")
            url
        } catch (t2: Throwable) {
            // 兜底 2：退回 data: URL（下载仍可用，个别浏览器内嵌预览会受限）
            println("[toBlobUrl] fallback to data url (blob failed: ${t.message}; fetch failed: ${t2.message})")
            dataUrl
        }
    }
}
