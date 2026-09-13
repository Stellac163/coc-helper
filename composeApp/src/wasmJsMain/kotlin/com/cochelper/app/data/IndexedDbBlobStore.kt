@file:OptIn(ExperimentalWasmJsInterop::class)

package com.cochelper.app.data

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.JsString
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 极简 IndexedDB 封装：把「文件内容 / 照片」等大字段单独存到 IndexedDB，
 * 绕开 localStorage 约 5MB 的每站点配额限制。
 *
 * 所有 blob 打成一个 JSON 对象、存为单条记录（key="all"），读写都用 getAll 避免
 * 直接 get 缺失键时的 undefined 判断。kotlinx-browser 0.5.0 未绑定 IndexedDB，
 * 故用下方 external 声明直接对接浏览器原生 API。
 */
class IndexedDbBlobStore private constructor(private val db: IDBDatabase) {

    private val json = Json { ignoreUnknownKeys = true }

    /** 用整份映射覆盖式写入（clear + put 一条记录）。 */
    suspend fun putAll(entries: Map<String, String>) {
        val tx = db.transaction("blobs", "readwrite")
        val store = tx.objectStore("blobs")
        store.clear()
        if (entries.isNotEmpty()) {
            val encoded = json.encodeToString(MapSerializer(String.serializer(), String.serializer()), entries)
            store.put(encoded, "all")
        }
        awaitCompletion(tx)
    }

    /** 读回整份映射；从未写入过时返回空 map。 */
    suspend fun getAll(): Map<String, String> {
        val tx = db.transaction("blobs", "readonly")
        val store = tx.objectStore("blobs")
        val req = store.getAll()
        awaitCompletion(tx)
        val arr = req.result!!.unsafeCast<JsArray<JsString>>()
        if (arr.length == 0) return emptyMap()
        return json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), arr[0]!!.toString())
    }

    /** 等待事务完成；失败抛异常，由上层决定是否回退。 */
    private suspend fun awaitCompletion(tx: IDBTransaction) {
        suspendCancellableCoroutine<Unit> { cont ->
            tx.oncomplete = { cont.resume(Unit) }
            tx.onerror = { cont.resumeWithException(IllegalStateException("IndexedDB 事务失败")) }
            tx.onabort = { cont.resumeWithException(IllegalStateException("IndexedDB 事务中止")) }
        }
    }

    companion object {
        private const val DB_NAME = "cochelper_blobs"
        private const val DB_VERSION = 1

        /** 打开（必要时创建）数据库；不可用时返回 null，调用方回退到 localStorage 全量存储。 */
        suspend fun open(): IndexedDbBlobStore? = try {
            suspendCancellableCoroutine<IndexedDbBlobStore?> { cont ->
                val idb = window.unsafeCast<WindowWithIndexedDB>().indexedDB
                if (idb == null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                val req = idb.open(DB_NAME, DB_VERSION)
                req.onupgradeneeded = {
                    try {
                        req.result.createObjectStore("blobs")
                    } catch (_: Throwable) {
                        // 同名 store 已存在（仅应在版本号提升时走到这里）
                    }
                }
                req.onsuccess = { cont.resume(IndexedDbBlobStore(req.result)) }
                req.onerror = { cont.resume(null) }
            }
        } catch (t: Throwable) {
            println("[IndexedDb] 打开失败，回退 localStorage 全量存储：${t.message}")
            null
        }
    }
}

// —— 以下 external 声明只覆盖本文件用到的最小 IndexedDB 子集 ——

private external interface WindowWithIndexedDB : JsAny {
    val indexedDB: IDBFactory?
}

private external interface IDBFactory : JsAny {
    fun open(name: String, version: Int): IDBOpenDBRequest
}

private external interface IDBOpenDBRequest : JsAny {
    var onupgradeneeded: ((JsAny) -> Unit)?
    var onsuccess: ((JsAny) -> Unit)?
    var onerror: ((JsAny) -> Unit)?
    val result: IDBDatabase
}

private external interface IDBRequest : JsAny {
    val result: JsAny?
}

private external interface IDBDatabase : JsAny {
    fun createObjectStore(name: String): IDBObjectStore
    fun transaction(storeNames: String, mode: String): IDBTransaction
}

private external interface IDBTransaction : JsAny {
    fun objectStore(name: String): IDBObjectStore
    var oncomplete: ((JsAny) -> Unit)?
    var onerror: ((JsAny) -> Unit)?
    var onabort: ((JsAny) -> Unit)?
}

private external interface IDBObjectStore : JsAny {
    fun clear(): IDBRequest
    fun put(value: String, key: String): IDBRequest
    fun getAll(): IDBRequest
}
