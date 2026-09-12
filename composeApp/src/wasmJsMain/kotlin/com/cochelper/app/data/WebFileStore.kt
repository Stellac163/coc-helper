package com.cochelper.app.data

import com.cochelper.app.platform.toBlobUrl
import kotlin.js.JsString
import kotlin.js.toDouble
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume

class WebFileStore : FileStore {

    override suspend fun pickFile(allowedMimeTypes: Array<String>?): PickedFile? {
        val file = suspendCancellableCoroutine<File?> { cont ->
            val input = window.document.createElement("input").unsafeCast<HTMLInputElement>()
            input.type = "file"
            if (allowedMimeTypes != null) input.accept = allowedMimeTypes.joinToString(",")

            var settled = false
            val finish: (File?) -> Unit = { f ->
                if (!settled && cont.isActive) {
                    settled = true
                    cont.resume(f)
                }
            }
            input.addEventListener("change", { finish(input.files?.item(0)) })
            input.addEventListener("cancel", { finish(null) })
            input.click()
        }
        file ?: return null

        val dataUrl = file.readAsDataUrl()
        return PickedFile(
            name = file.name,
            mimeType = file.type.ifBlank { "application/octet-stream" },
            dataUrl = dataUrl,
            sizeBytes = file.size.toDouble().toLong(),
        )
    }

    private suspend fun File.readAsDataUrl(): String = suspendCancellableCoroutine { cont ->
        val reader = FileReader()
        reader.onload = { cont.resume(reader.result?.unsafeCast<JsString>()?.toString() ?: "") }
        reader.onerror = { cont.resumeWith(Result.failure(IllegalStateException("读取文件失败"))) }
        reader.readAsDataURL(this)
    }

    override suspend fun downloadFile(name: String, mimeType: String, base64: String) {
        val a = window.document.createElement("a").unsafeCast<HTMLAnchorElement>()
        a.href = toBlobUrl(mimeType, base64)
        a.download = name
        a.click()
    }

    override suspend fun openInNewTab(name: String, mimeType: String, base64: String) {
        val url = toBlobUrl(mimeType, base64)
        // 桌面浏览器：新标签页由内置 PDF 阅读器打开。
        // 手机浏览器常拦截 window.open（弹出窗口被拦 / blob 在新标签页打不开），
        // 返回 null 时退回当前页跳转，手机也能看，看完按返回键回到应用。
        val opened = window.open(url, "_blank")
        if (opened == null) {
            window.location.href = url
        }
    }
}
