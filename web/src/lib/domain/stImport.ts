import { rollDice } from './dice';
import type { SkillItem } from '$lib/data/types';

export interface StResult {
  /** 规范中文属性名 → 数值（已含派生属性：生命/理智/魔法）。 */
  attributes: Record<string, number>;
  skills: SkillItem[];
}

const ALIASES: [string, string][] = [
  ['生命', '生命'], ['理智', '理智'], ['魔法', '魔法'], ['移动力', '移动力'],
  ['力量', '力量'], ['敏捷', '敏捷'], ['意志', '意志'], ['体质', '体质'],
  ['外貌', '外貌'], ['教育', '教育'], ['体型', '体型'], ['智力', '智力'], ['幸运', '幸运'],
  ['HP', '生命'], ['SAN', '理智'], ['MP', '魔法'], ['MOV', '移动力'],
  ['STR', '力量'], ['DEX', '敏捷'], ['POW', '意志'], ['CON', '体质'],
  ['APP', '外貌'], ['EDU', '教育'], ['SIZ', '体型'], ['INT', '智力'], ['LUCK', '幸运']
];

const attributeAliases = new Map<string, string>(
  ALIASES.map(([alias, canonical]) => [alias.toUpperCase(), canonical])
);

// 名称（非数字、非空白）+ 数值（整数，或 XdY 骰式可带 *k / +k / -k）
const pairRegex = /([^\d\s]+?)(\d+[dD]\d+(?:[*+\-]\d+)?|\d+)/g;

function evaluateValue(s: string): number | null {
  if (/^\d+$/.test(s)) return parseInt(s, 10);
  const m = /^(\d+)[dD](\d+)(?:([*+\-])(\d+))?$/.exec(s);
  if (!m) return null;
  const count = Math.min(Math.max(parseInt(m[1], 10), 1), 100);
  const faces = Math.min(Math.max(parseInt(m[2], 10), 1), 10000);
  let total = rollDice(faces, count).reduce((a, b) => a + b, 0);
  const op = m[3] ?? '';
  const k = m[4] != null ? parseInt(m[4], 10) : null;
  if (op && k != null) {
    if (op === '*') total = total * k;
    else if (op === '+') total = total + k;
    else if (op === '-') total = total - k;
  }
  return total;
}

export function parseSt(raw: string): StResult {
  let text = raw.trim();

  // 1) 去掉指令前缀 .st / /st / !st
  const lower = text.toLowerCase();
  if (lower.startsWith('.st') || lower.startsWith('/st') || lower.startsWith('!st')) {
    text = text.slice(3);
  }

  // 2) 去掉角色卡名前缀：名字--属性
  const dd = text.indexOf('--');
  if (dd >= 0) text = text.slice(dd + 2);

  // 3) 归一化分隔符
  text = text
    .replace(/\|/g, ' ').replace(/,/g, ' ').replace(/，/g, ' ').replace(/、/g, ' ')
    .replace(/;/g, ' ').replace(/；/g, ' ')
    .replace(/:/g, ' ').replace(/：/g, ' ').replace(/=/g, ' ')
    .replace(/\s+/g, '');

  const attributes: Record<string, number> = {};
  const skillsByName = new Map<string, number>();

  for (const m of text.matchAll(pairRegex)) {
    const name = m[1].replace(/[-+*()]+$/, '');
    const value = evaluateValue(m[2]);
    if (!name || value == null) continue;
    const canonical = attributeAliases.get(name.toUpperCase());
    if (canonical != null) {
      attributes[canonical] = value;
    } else {
      skillsByName.set(name, value);
    }
  }

  // 派生属性：仅当用户未显式给出时按公式补全。
  const pow = attributes['意志'] ?? 0;
  const con = attributes['体质'] ?? 0;
  const siz = attributes['体型'] ?? 0;
  if (!('生命' in attributes) && (con > 0 || siz > 0)) attributes['生命'] = Math.floor((con + siz) / 10);
  if (!('理智' in attributes) && pow > 0) attributes['理智'] = pow;
  if (!('魔法' in attributes) && pow > 0) attributes['魔法'] = Math.floor(pow / 5);

  return {
    attributes,
    skills: [...skillsByName].map(([name, value]) => ({ name, value }))
  };
}
