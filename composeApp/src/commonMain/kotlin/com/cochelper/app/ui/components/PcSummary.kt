package com.cochelper.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cochelper.app.data.local.PcEntity

/** 读取指定属性名对应的数值，找不到返回 0。 */
fun attributeValue(pc: PcEntity, name: String): Int =
    pc.attributes.firstOrNull { it.name == name }?.value ?: 0

/** 已填写的技能数量（数值 > 0）。 */
fun skilledCount(pc: PcEntity): Int = pc.skills.count { it.value > 0 }

/** 列表卡片上的属性概览：生命/理智/魔法/移动力，按顺序只显示非空项。 */
fun pcStatsSummary(pc: PcEntity): String {
    val parts = buildList {
        val hp = attributeValue(pc, "生命")
        val san = attributeValue(pc, "理智")
        val mp = attributeValue(pc, "魔法")
        val mov = attributeValue(pc, "移动力")
        if (hp > 0) add("生命 $hp")
        if (san > 0) add("理智 $san")
        if (mp > 0) add("魔法 $mp")
        if (mov > 0) add("移动力 $mov")
    }
    return if (parts.isEmpty()) "属性未填写" else parts.joinToString(" · ")
}

/** 「N 技能」小徽标。 */
@Composable
fun SkillCountBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = "$count 技能",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
