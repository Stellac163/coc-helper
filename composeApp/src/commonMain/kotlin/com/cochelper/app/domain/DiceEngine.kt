package com.cochelper.app.domain

import kotlin.math.max
import kotlin.random.Random

enum class SuccessLevel(val rank: Int, val label: String) {
    CRITICAL(5, "大成功"),
    EXTREME(4, "极难成功"),
    HARD(3, "困难成功"),
    SUCCESS(2, "成功"),
    FAILURE(1, "失败"),
    FUMBLE(0, "大失败"),
}

enum class OpposedOutcome { WIN, LOSE, DRAW }

data class DiceResult(
    val rolls: List<Int> = emptyList(),
    val total: Int = 0,
    val level: SuccessLevel? = null,
    val rollValue: Int? = null,
    val skill: Int? = null,
    val bonusDice: Int = 0,
    val penaltyDice: Int = 0,
    val tensDetail: String = "",
    val opponentRoll: Int? = null,
    val opponentLevel: SuccessLevel? = null,
    val opponentSkill: Int? = null,
    val outcome: OpposedOutcome? = null,
    val summary: String = "",
)

object DiceEngine {

    /** 掷 count 个 faces 面骰，返回结果列表。 */
    fun rollDice(faces: Int, count: Int): List<Int> =
        (0 until count).map { Random.nextInt(1, faces + 1) }

    /**
     * 掷 d100，支持奖励骰 / 惩罚骰（COC 7th）。
     * 返回 (最终值, 十位骰详情)。
     */
    fun rollD100(bonus: Int, penalty: Int): Pair<Int, String> {
        require(bonus == 0 || penalty == 0) { "奖励骰与惩罚骰不能同时存在" }
        val ones = Random.nextInt(0, 10) // 0..9
        val extra = max(bonus, penalty)
        val tensPool = (0..extra).map { Random.nextInt(0, 10) }
        val tens = when {
            bonus > 0 -> tensPool.min()
            penalty > 0 -> tensPool.max()
            else -> tensPool.first()
        }
        val raw = tens * 10 + ones
        val value = if (raw == 0) 100 else raw
        val detail = if (extra > 0) {
            val type = if (bonus > 0) "奖励" else "惩罚"
            "${type}骰：十位 ${tensPool.joinToString("/")}，取 $tens；个位 $ones"
        } else ""
        return value to detail
    }

    /** COC 7th 判定等级。 */
    fun successLevel(roll: Int, skill: Int): SuccessLevel {
        val critThreshold = if (skill >= 50) skill / 5 else 1
        val extremeThreshold = skill / 5
        val hardThreshold = skill / 2
        val fumbleThreshold = if (skill >= 50) 100 else 96
        return when {
            roll <= critThreshold -> SuccessLevel.CRITICAL
            roll <= extremeThreshold -> SuccessLevel.EXTREME
            roll <= hardThreshold -> SuccessLevel.HARD
            roll <= skill -> SuccessLevel.SUCCESS
            roll >= fumbleThreshold -> SuccessLevel.FUMBLE
            else -> SuccessLevel.FAILURE
        }
    }

    /** 对抗：先比等级，同级比点数，点数相同则平局。 */
    fun opposed(aLevel: SuccessLevel, aRoll: Int, bLevel: SuccessLevel, bRoll: Int): OpposedOutcome =
        when {
            aLevel.rank > bLevel.rank -> OpposedOutcome.WIN
            aLevel.rank < bLevel.rank -> OpposedOutcome.LOSE
            aRoll > bRoll -> OpposedOutcome.WIN
            aRoll < bRoll -> OpposedOutcome.LOSE
            else -> OpposedOutcome.DRAW
        }

    /**
     * 综合投掷入口。
     * @param faces 骰子面数
     * @param count 骰子数目
     * @param skill 技能数值（可为 null）
     * @param bonus 奖励骰
     * @param penalty 惩罚骰
     * @param opponentSkill 对抗者技能数值（可为 null）
     */
    fun roll(
        faces: Int,
        count: Int,
        skill: Int?,
        bonus: Int,
        penalty: Int,
        opponentSkill: Int?,
    ): DiceResult {
        val safeFaces = faces.coerceIn(1, 10000)
        val safeCount = count.coerceIn(1, 100)

        // 单个 d100 且提供了技能值 → 技能判定
        val isSkillCheck = safeFaces == 100 && safeCount == 1 && skill != null

        val rolls: List<Int>
        var tensDetail = ""
        var primaryRoll: Int? = null
        var primaryLevel: SuccessLevel? = null

        if (isSkillCheck) {
            val (v, detail) = rollD100(bonus, penalty)
            rolls = listOf(v)
            tensDetail = detail
            primaryRoll = v
            primaryLevel = successLevel(v, skill)
        } else {
            rolls = rollDice(safeFaces, safeCount)
        }

        val total = rolls.sum()

        var opponentRoll: Int? = null
        var opponentLevel: SuccessLevel? = null
        var outcome: OpposedOutcome? = null

        if (isSkillCheck && opponentSkill != null) {
            val (v, _) = rollD100(0, 0)
            opponentRoll = v
            opponentLevel = successLevel(v, opponentSkill)
            outcome = opposed(primaryLevel!!, primaryRoll!!, opponentLevel, v)
        }

        val summary = buildString {
            if (isSkillCheck) {
                append("${safeCount}D100 = $primaryRoll")
                if (tensDetail.isNotEmpty()) append("（$tensDetail）")
                append(" → ${primaryLevel?.label}（技能 $skill）")
            } else {
                append("${safeCount}D$safeFaces = ${rolls.joinToString(" + ")}")
                if (safeCount > 1) append(" = $total")
            }
            if (opponentLevel != null) {
                append("\n对抗者：$opponentRoll → ${opponentLevel.label}（技能 $opponentSkill）")
                append("\n结果：${outcomeLabel(outcome)}")
            }
        }

        return DiceResult(
            rolls = rolls,
            total = total,
            level = primaryLevel,
            rollValue = primaryRoll,
            skill = skill,
            bonusDice = bonus,
            penaltyDice = penalty,
            tensDetail = tensDetail,
            opponentRoll = opponentRoll,
            opponentLevel = opponentLevel,
            opponentSkill = opponentSkill,
            outcome = outcome,
            summary = summary,
        )
    }

    private fun outcomeLabel(outcome: OpposedOutcome?): String = when (outcome) {
        OpposedOutcome.WIN -> "胜出"
        OpposedOutcome.LOSE -> "落败"
        OpposedOutcome.DRAW -> "平局"
        null -> ""
    }
}
