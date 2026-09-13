package com.cochelper.app.platform

import kotlin.js.JsArray
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.toJsString
import kotlinx.browser.window
import kotlinx.coroutines.await
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
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestCache
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode
import org.w3c.fetch.RequestRedirect
import org.w3c.fetch.Response

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
        // 无需缩放时保留原图，避免重编码画质劣化
        if (nw == w && nh == h) return dataUrl
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
): HttpResult {
    val h = Headers()
    headers.forEach { (k, v) -> h.append(k, v) }
    // Kotlin/Wasm 用 RequestInit(...) 构造时会把未指定的枚举字段（cache 等）置为 null，
    // 浏览器 fetch 会拒绝 null 的枚举值（"null is not a valid enum value of type RequestCache"）。
    // 显式补上各枚举字段的合法默认值即可。
    val init = RequestInit(
        method = method,
        headers = h,
        body = body?.toJsString(),
        cache = "default".toJsString().unsafeCast<RequestCache>(),
        credentials = "same-origin".toJsString().unsafeCast<RequestCredentials>(),
        mode = "cors".toJsString().unsafeCast<RequestMode>(),
        redirect = "follow".toJsString().unsafeCast<RequestRedirect>(),
        // referrerPolicy 在 kotlinx-browser 0.5.0 里是 JsAny?（无枚举类），用 JsString 传合法枚举值
        referrerPolicy = "strict-origin-when-cross-origin".toJsString(),
    )
    val response: Response = window.fetch(url, init).await()
    val text = response.text().await<JsString>().toString()
    return HttpResult(response.status.toInt(), text)
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
