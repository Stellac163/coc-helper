export type SuccessLevel = 'CRITICAL' | 'EXTREME' | 'HARD' | 'SUCCESS' | 'FAILURE' | 'FUMBLE';

export const LEVEL_LABEL: Record<SuccessLevel, string> = {
  CRITICAL: '大成功',
  EXTREME: '极难成功',
  HARD: '困难成功',
  SUCCESS: '成功',
  FAILURE: '失败',
  FUMBLE: '大失败'
};

export const LEVEL_RANK: Record<SuccessLevel, number> = {
  CRITICAL: 5,
  EXTREME: 4,
  HARD: 3,
  SUCCESS: 2,
  FAILURE: 1,
  FUMBLE: 0
};

export type OpposedOutcome = 'WIN' | 'LOSE' | 'DRAW';

export interface DiceResult {
  rolls: number[];
  total: number;
  level: SuccessLevel | null;
  rollValue: number | null;
  skill: number | null;
  bonusDice: number;
  penaltyDice: number;
  tensDetail: string;
  opponentRoll: number | null;
  opponentLevel: SuccessLevel | null;
  opponentSkill: number | null;
  outcome: OpposedOutcome | null;
  summary: string;
}

function rnd(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

/** 掷 count 个 faces 面骰，返回结果列表。 */
export function rollDice(faces: number, count: number): number[] {
  const out: number[] = [];
  for (let i = 0; i < count; i++) out.push(rnd(1, faces));
  return out;
}

/** 掷 d100，支持奖励骰 / 惩罚骰（COC 7th）。 */
export function rollD100(bonus: number, penalty: number): { value: number; detail: string } {
  const ones = rnd(0, 9); // 0..9
  const extra = Math.max(bonus, penalty);
  const tensPool: number[] = [];
  for (let i = 0; i <= extra; i++) tensPool.push(rnd(0, 9));
  let tens: number;
  if (bonus > 0) tens = Math.min(...tensPool);
  else if (penalty > 0) tens = Math.max(...tensPool);
  else tens = tensPool[0];
  const raw = tens * 10 + ones;
  const value = raw === 0 ? 100 : raw;
  let detail = '';
  if (extra > 0) {
    const type = bonus > 0 ? '奖励' : '惩罚';
    detail = `${type}骰：十位 ${tensPool.join('/')}，取 ${tens}；个位 ${ones}`;
  }
  return { value, detail };
}

/** COC 7th 判定等级。 */
export function successLevel(roll: number, skill: number): SuccessLevel {
  const critThreshold = skill >= 50 ? Math.floor(skill / 5) : 1;
  const extremeThreshold = Math.floor(skill / 5);
  const hardThreshold = Math.floor(skill / 2);
  const fumbleThreshold = skill >= 50 ? 100 : 96;
  if (roll <= critThreshold) return 'CRITICAL';
  if (roll <= extremeThreshold) return 'EXTREME';
  if (roll <= hardThreshold) return 'HARD';
  if (roll <= skill) return 'SUCCESS';
  if (roll >= fumbleThreshold) return 'FUMBLE';
  return 'FAILURE';
}

/** 对抗：先比等级，同级比点数，点数相同则平局。 */
export function opposed(
  aLevel: SuccessLevel,
  aRoll: number,
  bLevel: SuccessLevel,
  bRoll: number
): OpposedOutcome {
  const ar = LEVEL_RANK[aLevel];
  const br = LEVEL_RANK[bLevel];
  if (ar > br) return 'WIN';
  if (ar < br) return 'LOSE';
  if (aRoll > bRoll) return 'WIN';
  if (aRoll < bRoll) return 'LOSE';
  return 'DRAW';
}

const OUTCOME_LABEL: Record<OpposedOutcome, string> = { WIN: '胜出', LOSE: '落败', DRAW: '平局' };

/** 综合投掷入口。 */
export function roll(
  faces: number,
  count: number,
  skill: number | null,
  bonus: number,
  penalty: number,
  opponentSkill: number | null
): DiceResult {
  const safeFaces = Math.min(Math.max(faces, 1), 10000);
  const safeCount = Math.min(Math.max(count, 1), 100);

  const isSkillCheck = safeFaces === 100 && safeCount === 1 && skill != null;

  let rolls: number[];
  let tensDetail = '';
  let primaryRoll: number | null = null;
  let primaryLevel: SuccessLevel | null = null;

  if (isSkillCheck) {
    const { value, detail } = rollD100(bonus, penalty);
    rolls = [value];
    tensDetail = detail;
    primaryRoll = value;
    primaryLevel = successLevel(value, skill as number);
  } else {
    rolls = rollDice(safeFaces, safeCount);
  }

  const total = rolls.reduce((a, b) => a + b, 0);

  let opponentRoll: number | null = null;
  let opponentLevel: SuccessLevel | null = null;
  let outcome: OpposedOutcome | null = null;

  if (isSkillCheck && opponentSkill != null) {
    const { value } = rollD100(0, 0);
    opponentRoll = value;
    opponentLevel = successLevel(value, opponentSkill);
    outcome = opposed(primaryLevel as SuccessLevel, primaryRoll as number, opponentLevel, value);
  }

  let summary = '';
  if (isSkillCheck) {
    summary = `${safeCount}D100 = ${primaryRoll}`;
    if (tensDetail) summary += `（${tensDetail}）`;
    summary += ` → ${LEVEL_LABEL[primaryLevel as SuccessLevel]}（技能 ${skill}）`;
  } else {
    summary = `${safeCount}D${safeFaces} = ${rolls.join(' + ')}`;
    if (safeCount > 1) summary += ` = ${total}`;
  }
  if (opponentLevel != null) {
    summary += `\n对抗者：${opponentRoll} → ${LEVEL_LABEL[opponentLevel]}（技能 ${opponentSkill}）`;
    summary += `\n结果：${OUTCOME_LABEL[outcome as OpposedOutcome]}`;
  }

  return {
    rolls,
    total,
    level: primaryLevel,
    rollValue: primaryRoll,
    skill,
    bonusDice: bonus,
    penaltyDice: penalty,
    tensDetail,
    opponentRoll,
    opponentLevel,
    opponentSkill,
    outcome,
    summary
  };
}
