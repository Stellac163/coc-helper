package com.cochelper.app.data.local

import com.cochelper.app.data.sync.BackupPayload
import kotlinx.coroutines.flow.SharedFlow

/**
 * 整份快照的「替换式写入」+「数据变更信号」抽象。
 *
 * 自动同步依赖两件事：
 * - [changes]：每次本地增删改后发出一个信号，供上层防抖后触发同步；
 * - [replaceAll]：用云端快照整体覆盖本地，**不发出 [changes]**，
 *   从而避免「同步 → 写回本地 → 又触发同步」的死循环。
 */
interface BackupStore {
    val changes: SharedFlow<Unit>
    suspend fun replaceAll(payload: BackupPayload)
}
