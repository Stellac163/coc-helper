package com.cochelper.app.data.local

import com.cochelper.app.platform.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
data class SkillItem(val name: String = "", val value: Int = 0)

/** 属性（生命/理智/魔法/移动力/力量/敏捷/意志/体质/外貌/教育/体型/智力/幸运）。 */
@Serializable
data class AttributeItem(val name: String = "", val value: Int = 0)

/** 模组 */
@Serializable
data class ModuleEntity(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = false,
    val hasOriginalDoc: Boolean = false,
    val introText: String = "",
    val photoUri: String = "",
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
)

/** 时间轴 / 大纲节点（支持最多三层嵌套） */
@Serializable
data class TimelineNodeEntity(
    val id: Long = 0,
    val moduleId: Long,
    val title: String,
    val content: String = "",
    val parentId: Long? = null,
    val order: Int = 0,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
)

/** 重要地点 */
@Serializable
data class LocationEntity(
    val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val content: String = "",
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
)

/** 重要 NPC */
@Serializable
data class NpcEntity(
    val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val content: String = "",
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
)

/** PC 调查员档案。moduleId 为 null 表示玩家自己的角色（角色栏），非空表示某模组下的调查员（pc档案）。 */
@Serializable
data class PcEntity(
    val id: Long = 0,
    val moduleId: Long? = null,
    val name: String,
    val player: String = "",
    val gender: String = "",
    val age: String = "",
    val skills: List<SkillItem> = defaultSkills(),
    val attributes: List<AttributeItem> = defaultAttributes(),
    val appearance: String = "",
    val beliefs: String = "",
    val importantPlaces: String = "",
    val valuables: String = "",
    val traits: String = "",
    val wounds: String = "",
    val phobias: String = "",
    val background: String = "",
    val imageUri: String = "",
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
) {
    companion object {
        fun defaultSkills(): List<SkillItem> = listOf(
            "侦查", "聆听", "图书馆使用", "心理学", "话术", "说服", "魅惑",
            "闪避", "格斗", "射击", "急救", "潜行", "追踪", "历史", "神秘学",
            "博物学", "汽车驾驶", "会计", "估价", "法律", "医学", "外语"
        ).map { SkillItem(it, 0) }

        fun defaultAttributes(): List<AttributeItem> = listOf(
            AttributeItem("生命", 0),
            AttributeItem("理智", 0),
            AttributeItem("魔法", 0),
            AttributeItem("移动力", 8),
            AttributeItem("力量", 0),
            AttributeItem("敏捷", 0),
            AttributeItem("意志", 0),
            AttributeItem("体质", 0),
            AttributeItem("外貌", 0),
            AttributeItem("教育", 0),
            AttributeItem("体型", 0),
            AttributeItem("智力", 0),
            AttributeItem("幸运", 0),
        )
    }
}

/** 线索板线索 */
@Serializable
data class ClueEntity(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val order: Int = 0,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
)

/** 模组配套 / 原文文件。web 端文件内容以 base64 存储（contentBase64）。 */
@Serializable
data class FileEntity(
    val id: Long = 0,
    val moduleId: Long,
    val name: String,
    val mimeType: String = "",
    val contentBase64: String = "",
    val sizeBytes: Long = 0,
    val kind: Int = FILE_COMPANION,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
) {
    companion object {
        const val FILE_ORIGINAL = 0
        const val FILE_COMPANION = 1
    }
}

/** 战斗轮参与者 */
@Serializable
data class CombatantEntity(
    val id: Long = 0,
    val name: String,
    val dex: Int = 50,
    val hp: Int = 10,
    val maxHp: Int = 10,
    val status: Int = COMBATANT_ALIVE,
    val ranged: Boolean = false,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
) {
    companion object {
        const val COMBATANT_ALIVE = 0
        const val COMBATANT_UNCONSCIOUS = 1
        const val COMBATANT_DEAD = 2
    }
}

/** 追逐战参与者 */
@Serializable
data class ChaseParticipantEntity(
    val id: Long = 0,
    val name: String,
    val mov: Int = 8,
    val position: Int = 0,
    val role: Int = CHASE_QUARRY,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = 0,
) {
    companion object {
        const val CHASE_QUARRY = 0
        const val CHASE_PURSUER = 1
    }
}
