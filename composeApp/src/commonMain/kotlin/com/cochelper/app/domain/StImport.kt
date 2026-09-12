package com.cochelper.app.domain

import com.cochelper.app.data.local.SkillItem

/**
 * `.st` 骰娘指令解析器：把一段一次性粘贴的指令解析为「属性 + 技能」两部分。
 *
 * 兼容常见写法：
 *  - `.st 力量50敏捷60`（名称数值无空格直接拼接）
 *  - `.st 力量 50 敏捷 60` / `.st 力量:50` / `力量=50`
 *  - 条目间用 `|` / `,` / `、` / `;` 分隔
 *  - `名字--力量30`（带角色卡名前缀）
 *  - 数值支持整数或骰式 `3d6`、`3d6*5`、`3d6+5`
 *  - 属性名中英文均识别（力量/STR、敏捷/DEX、意志/POW…）
 */
object StImport {

    data class Result(
        /** 规范中文属性名 → 数值（如 "力量" → 50），已含派生属性（生命/理智/魔法）。 */
        val attributes: Map<String, Int>,
        val skills: List<SkillItem>,
    )

    /** 中文 + 英文属性名 → 规范中文名（键统一转大写后匹配）。 */
    private val attributeAliases: Map<String, String> = buildMap {
        listOf(
            "生命" to "生命", "理智" to "理智", "魔法" to "魔法", "移动力" to "移动力",
            "力量" to "力量", "敏捷" to "敏捷", "意志" to "意志", "体质" to "体质",
            "外貌" to "外貌", "教育" to "教育", "体型" to "体型", "智力" to "智力", "幸运" to "幸运",
            "HP" to "生命", "SAN" to "理智", "MP" to "魔法", "MOV" to "移动力",
            "STR" to "力量", "DEX" to "敏捷", "POW" to "意志", "CON" to "体质",
            "APP" to "外貌", "EDU" to "教育", "SIZ" to "体型", "INT" to "智力", "LUCK" to "幸运",
        ).forEach { (alias, canonical) -> put(alias.uppercase(), canonical) }
    }

    // 名称（非数字、非空白）+ 数值（整数，或 XdY 骰式可带 *k / +k / -k）
    private val pairRegex = Regex("""([^\d\s]+?)(\d+[dD]\d+(?:[*+\-]\d+)?|\d+)""")

    fun parse(raw: String): Result {
        var text = raw.trim()

        // 1) 去掉指令前缀 .st / /st / !st
        val lower = text.lowercase()
        text = when {
            lower.startsWith(".st") || lower.startsWith("/st") || lower.startsWith("!st") -> text.drop(3)
            else -> text
        }

        // 2) 去掉角色卡名前缀：名字--属性
        val dd = text.indexOf("--")
        if (dd >= 0) text = text.substring(dd + 2)

        // 3) 归一化分隔符：| , 、 ; 分隔条目；: = 分隔名称与数值；最后移除所有空白
        text = text
            .replace('|', ' ').replace(',', ' ').replace('，', ' ').replace('、', ' ')
            .replace(';', ' ').replace('；', ' ')
            .replace(':', ' ').replace('：', ' ').replace('=', ' ')
            .replace(Regex("""\s+"""), "")

        val attributes = mutableMapOf<String, Int>()
        val skillsByName = linkedMapOf<String, Int>()

        pairRegex.findAll(text).forEach { m ->
            val name = m.groupValues[1].trimEnd('+', '-', '*', '(', ')')
            val value = evaluateValue(m.groupValues[2])
            if (name.isEmpty() || value == null) return@forEach
            val canonical = attributeAliases[name.uppercase()]
            if (canonical != null) {
                attributes[canonical] = value
            } else {
                skillsByName[name] = value
            }
        }

        // 派生属性：仅当用户未显式给出时按公式补全。
        val pow = attributes["意志"] ?: 0
        val con = attributes["体质"] ?: 0
        val siz = attributes["体型"] ?: 0
        if ("生命" !in attributes && (con > 0 || siz > 0)) attributes["生命"] = (con + siz) / 10
        if ("理智" !in attributes && pow > 0) attributes["理智"] = pow
        if ("魔法" !in attributes && pow > 0) attributes["魔法"] = pow / 5

        return Result(attributes, skillsByName.map { (n, v) -> SkillItem(n, v) })
    }

    /** 解析数值：整数直接返回；`XdY` 掷骰求和（可带 *k / +k / -k）后返回；无法解析返回 null。 */
    private fun evaluateValue(s: String): Int? {
        s.toIntOrNull()?.let { return it }
        val m = Regex("""^(\d+)[dD](\d+)(?:([*+\-])(\d+))?$""").matchEntire(s) ?: return null
        val count = m.groupValues[1].toInt().coerceIn(1, 100)
        val faces = m.groupValues[2].toInt().coerceIn(1, 10000)
        var total = DiceEngine.rollDice(faces, count).sum()
        val op = m.groupValues[3]
        val k = m.groupValues[4].toIntOrNull()
        if (op.isNotEmpty() && k != null) {
            total = when (op) {
                "*" -> total * k
                "+" -> total + k
                "-" -> total - k
                else -> total
            }
        }
        return total
    }
}
