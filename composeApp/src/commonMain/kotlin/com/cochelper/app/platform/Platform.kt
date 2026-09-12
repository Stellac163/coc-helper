package com.cochelper.app.platform

/** 当前时间戳（毫秒）。 */
expect fun currentTimeMillis(): Long

/** 生成新的全局唯一 id（正数）。 */
expect fun nextId(): Long

/** Base64 编码。 */
expect fun base64Encode(bytes: ByteArray): String

/** Base64 解码。 */
expect fun base64Decode(s: String): ByteArray

/** 把 base64 内容转为浏览器可直接预览/下载的 Blob URL（data: URL 在 iframe 与新标签页常被拦截）。 */
expect suspend fun toBlobUrl(mimeType: String, base64: String): String

/**
 * 把 data:image 图片压缩为 JPEG data URL（长边缩到 [maxDimension] 内，[quality] 0~1）。
 * 用于把本地选中的照片/头像压小后再写入 localStorage，避免整份快照超配额。
 * 失败、非图片或压缩无收益时原样返回。
 */
expect suspend fun compressImageDataUrl(dataUrl: String, maxDimension: Int, quality: Double): String

data class HttpResult(val status: Int, val body: String)

/** 发起一次 HTTP 请求，返回状态码与响应体。网络失败时抛异常。 */
expect suspend fun httpRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String? = null,
): HttpResult
