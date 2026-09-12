package com.cochelper.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class SkillItem(val name: String = "", val value: Int = 0)

/** 模组 */
@Serializable
@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val hasOriginalDoc: Boolean = false,
    val introText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** 时间轴 / 大纲节点（支持最多三层嵌套） */
@Serializable
@Entity(tableName = "timeline_nodes")
data class TimelineNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val title: String,
    val content: String = "",
    val parentId: Long? = null,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 重要地点 */
@Serializable
@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** 重要 NPC */
@Serializable
@Entity(tableName = "npcs")
data class NpcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** PC 调查员档案 */
@Serializable
@Entity(tableName = "pcs")
data class PcEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val player: String = "",
    val gender: String = "",
    val age: String = "",
    val skills: List<SkillItem> = defaultSkills(),
    val appearance: String = "",
    val beliefs: String = "",
    val importantPlaces: String = "",
    val valuables: String = "",
    val traits: String = "",
    val wounds: String = "",
    val phobias: String = "",
    val background: String = "",
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun defaultSkills(): List<SkillItem> = listOf(
            "侦查", "聆听", "图书馆使用", "心理学", "话术", "说服", "魅惑",
            "闪避", "格斗", "射击", "急救", "潜行", "追踪", "历史", "神秘学",
            "博物学", "汽车驾驶", "会计", "估价", "法律", "医学", "外语"
        ).map { SkillItem(it, 0) }
    }
}

/** 线索板线索 */
@Serializable
@Entity(tableName = "clues")
data class ClueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/** 模组配套 / 原文文件 */
@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val mimeType: String = "",
    val localPath: String = "",
    val sizeBytes: Long = 0,
    val kind: Int = FILE_COMPANION,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val FILE_ORIGINAL = 0
        const val FILE_COMPANION = 1
    }
}

/** 战斗轮参与者 */
@Serializable
@Entity(tableName = "combatants")
data class CombatantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dex: Int = 50,
    val hp: Int = 10,
    val maxHp: Int = 10,
    val status: Int = COMBATANT_ALIVE,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val COMBATANT_ALIVE = 0
        const val COMBATANT_UNCONSCIOUS = 1
        const val COMBATANT_DEAD = 2
    }
}

/** 追逐战参与者 */
@Serializable
@Entity(tableName = "chase_participants")
data class ChaseParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mov: Int = 8,
    val position: Int = 0,
    val role: Int = CHASE_QUARRY,
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val CHASE_QUARRY = 0
        const val CHASE_PURSUER = 1
    }
}
