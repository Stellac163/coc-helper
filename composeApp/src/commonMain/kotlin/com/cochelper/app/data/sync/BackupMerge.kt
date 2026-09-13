package com.cochelper.app.data.sync

import com.cochelper.app.platform.currentTimeMillis

/**
 * 把本地与云端两份快照「对账」成一致状态（last-write-wins）。
 *
 * 合并规则（与 life-manager 的同步引擎一致）：
 * - 按 [id] 取并集：两边各有的实体都保留，不会互相覆盖丢失；
 * - 同 id 冲突时，取 [updatedAt] 较新的一条（后写胜出），时间相同则保留本地；
 * - version 取较大者，exportedAt 刷新为当前时间。
 *
 * 注意：删除目前是硬删除（不产生墓碑），因此在一台设备上删除的条目，
 * 若另一台设备仍保留该条目，合并后可能「复活」。多设备间如需可靠同步删除，
 * 需要引入软删除（deletedAt 墓碑），暂未实现。
 */
fun mergeBackups(local: BackupPayload, remote: BackupPayload): BackupPayload {
    fun <T> mergeById(
        localList: List<T>,
        remoteList: List<T>,
        id: (T) -> Long,
        updatedAt: (T) -> Long,
    ): List<T> {
        val map = LinkedHashMap<Long, T>()
        for (e in remoteList) map[id(e)] = e
        for (e in localList) {
            val existing = map[id(e)]
            if (existing == null || updatedAt(e) >= updatedAt(existing)) {
                map[id(e)] = e
            }
        }
        return map.values.toList()
    }

    return BackupPayload(
        version = maxOf(local.version, remote.version),
        exportedAt = currentTimeMillis(),
        modules = mergeById(local.modules, remote.modules, { it.id }, { it.updatedAt }),
        timelineNodes = mergeById(local.timelineNodes, remote.timelineNodes, { it.id }, { it.updatedAt }),
        locations = mergeById(local.locations, remote.locations, { it.id }, { it.updatedAt }),
        npcs = mergeById(local.npcs, remote.npcs, { it.id }, { it.updatedAt }),
        pcs = mergeById(local.pcs, remote.pcs, { it.id }, { it.updatedAt }),
        clues = mergeById(local.clues, remote.clues, { it.id }, { it.updatedAt }),
        combatants = mergeById(local.combatants, remote.combatants, { it.id }, { it.updatedAt }),
        chaseParticipants = mergeById(local.chaseParticipants, remote.chaseParticipants, { it.id }, { it.updatedAt }),
        chasePoints = mergeById(local.chasePoints, remote.chasePoints, { it.id }, { it.updatedAt }),
        files = mergeById(local.files, remote.files, { it.id }, { it.updatedAt }),
    )
}
