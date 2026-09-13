package com.cochelper.app.data.sync

import com.cochelper.app.data.local.ChaseParticipantEntity
import com.cochelper.app.data.local.ChasePointEntity
import com.cochelper.app.data.local.ClueEntity
import com.cochelper.app.data.local.CombatantEntity
import com.cochelper.app.data.local.FileEntity
import com.cochelper.app.data.local.LocationEntity
import com.cochelper.app.data.local.ModuleEntity
import com.cochelper.app.data.local.NpcEntity
import com.cochelper.app.data.local.PcEntity
import com.cochelper.app.data.local.TimelineNodeEntity
import kotlinx.serialization.Serializable

/** 完整的云端备份快照（web 端文件内容以 base64 一并入库）。 */
@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val modules: List<ModuleEntity> = emptyList(),
    val timelineNodes: List<TimelineNodeEntity> = emptyList(),
    val locations: List<LocationEntity> = emptyList(),
    val npcs: List<NpcEntity> = emptyList(),
    val pcs: List<PcEntity> = emptyList(),
    val clues: List<ClueEntity> = emptyList(),
    val combatants: List<CombatantEntity> = emptyList(),
    val chaseParticipants: List<ChaseParticipantEntity> = emptyList(),
    val chasePoints: List<ChasePointEntity> = emptyList(),
    val files: List<FileEntity> = emptyList(),
)
