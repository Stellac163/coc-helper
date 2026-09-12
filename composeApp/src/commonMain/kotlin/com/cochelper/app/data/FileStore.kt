package com.cochelper.app.data

/** 用户通过文件选择器挑中的文件（dataUrl 为完整 data:...;base64,... 串）。 */
data class PickedFile(
    val name: String,
    val mimeType: String,
    val dataUrl: String,
    val sizeBytes: Long,
)

interface FileStore {
    /** 打开文件选择器，返回选中文件（取消返回 null）。allowedMimeTypes 为 null 表示不限类型。 */
    suspend fun pickFile(allowedMimeTypes: Array<String>? = null): PickedFile?

    /** 触发浏览器下载。base64 为不含 data: 前缀的纯 base64 内容。 */
    suspend fun downloadFile(name: String, mimeType: String, base64: String)

    /** 在新标签页中打开（用于预览 PDF 等）。 */
    suspend fun openInNewTab(name: String, mimeType: String, base64: String)
}
