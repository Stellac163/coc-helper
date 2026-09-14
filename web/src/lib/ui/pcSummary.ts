import type { PcEntity } from '$lib/data/types';

/** 读取指定属性名对应的数值，找不到返回 0。 */
export function attributeValue(pc: PcEntity, name: string): number {
  return pc.attributes.find((a) => a.name === name)?.value ?? 0;
}

/** 已填写的技能数量（数值 > 0）。 */
export function skilledCount(pc: PcEntity): number {
  return pc.skills.filter((s) => s.value > 0).length;
}

/** 列表卡片上的属性概览：生命/理智/魔法/移动力，按顺序只显示非空项。 */
export function pcStatsSummary(pc: PcEntity): string {
  const parts: string[] = [];
  const hp = attributeValue(pc, '生命');
  const san = attributeValue(pc, '理智');
  const mp = attributeValue(pc, '魔法');
  const mov = attributeValue(pc, '移动力');
  if (hp > 0) parts.push(`生命 ${hp}`);
  if (san > 0) parts.push(`理智 ${san}`);
  if (mp > 0) parts.push(`魔法 ${mp}`);
  if (mov > 0) parts.push(`移动力 ${mov}`);
  return parts.length === 0 ? '属性未填写' : parts.join(' · ');
}

/** 角色卡片副标题：玩家/性别/年龄。 */
export function pcSummary(pc: PcEntity): string {
  const parts: string[] = [];
  if (pc.player.trim()) parts.push(`玩家 ${pc.player}`);
  if (pc.gender.trim()) parts.push(pc.gender);
  if (pc.age.trim()) parts.push(`${pc.age} 岁`);
  return parts.length === 0 ? '人物基础属性及简介' : parts.join(' · ');
}
