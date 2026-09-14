// 与 Kotlin @Serializable 数据类字段名严格一致，保证 localStorage/IndexedDB/GitHub 备份零迁移。

export interface SkillItem {
  name: string;
  value: number;
}

export interface AttributeItem {
  name: string;
  value: number;
}

export interface ModuleEntity {
  id: number;
  name: string;
  description: string;
  isActive: boolean;
  hasOriginalDoc: boolean;
  introText: string;
  photoUri: string;
  createdAt: number;
  updatedAt: number;
}

export interface TimelineNodeEntity {
  id: number;
  moduleId: number;
  title: string;
  content: string;
  parentId: number | null;
  order: number;
  createdAt: number;
  updatedAt: number;
}

export interface LocationEntity {
  id: number;
  moduleId: number;
  name: string;
  content: string;
  createdAt: number;
  updatedAt: number;
}

export interface NpcEntity {
  id: number;
  moduleId: number;
  name: string;
  content: string;
  createdAt: number;
  updatedAt: number;
}

export interface PcEntity {
  id: number;
  moduleId: number | null;
  name: string;
  player: string;
  gender: string;
  age: string;
  skills: SkillItem[];
  attributes: AttributeItem[];
  appearance: string;
  beliefs: string;
  importantPeople: string;
  importantPlaces: string;
  valuables: string;
  traits: string;
  wounds: string;
  phobias: string;
  background: string;
  imageUri: string;
  createdAt: number;
  updatedAt: number;
}

export interface ClueEntity {
  id: number;
  title: string;
  content: string;
  order: number;
  createdAt: number;
  updatedAt: number;
}

export interface FileEntity {
  id: number;
  moduleId: number;
  name: string;
  mimeType: string;
  contentBase64: string;
  sizeBytes: number;
  kind: number;
  createdAt: number;
  updatedAt: number;
}

export interface CombatantEntity {
  id: number;
  name: string;
  dex: number;
  hp: number;
  maxHp: number;
  status: number;
  ranged: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface ChasePointEntity {
  id: number;
  name: string;
  order: number;
  createdAt: number;
  updatedAt: number;
}

export interface ChaseParticipantEntity {
  id: number;
  name: string;
  mov: number;
  pointId: number;
  role: number;
  createdAt: number;
  updatedAt: number;
}

export interface BackupPayload {
  version: number;
  exportedAt: number;
  modules: ModuleEntity[];
  timelineNodes: TimelineNodeEntity[];
  locations: LocationEntity[];
  npcs: NpcEntity[];
  pcs: PcEntity[];
  clues: ClueEntity[];
  combatants: CombatantEntity[];
  chaseParticipants: ChaseParticipantEntity[];
  chasePoints: ChasePointEntity[];
  files: FileEntity[];
}

export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';

export interface AppSettings {
  themeMode: ThemeMode;
  githubToken: string;
  repoName: string;
  nickname: string;
  avatarUri: string;
}

export const FILE_ORIGINAL = 0;
export const FILE_COMPANION = 1;

export const COMBATANT_ALIVE = 0;
export const COMBATANT_UNCONSCIOUS = 1;
export const COMBATANT_DEAD = 2;

export const CHASE_QUARRY = 0;
export const CHASE_PURSUER = 1;

const DEFAULT_SKILL_NAMES = [
  '侦查', '聆听', '图书馆使用', '心理学', '话术', '说服', '魅惑',
  '闪避', '格斗', '射击', '急救', '潜行', '追踪', '历史', '神秘学',
  '博物学', '汽车驾驶', '会计', '估价', '法律', '医学', '外语'
];

export function defaultSkills(): SkillItem[] {
  return DEFAULT_SKILL_NAMES.map((name) => ({ name, value: 0 }));
}

export function defaultAttributes(): AttributeItem[] {
  return [
    { name: '生命', value: 0 },
    { name: '理智', value: 0 },
    { name: '魔法', value: 0 },
    { name: '移动力', value: 8 },
    { name: '力量', value: 0 },
    { name: '敏捷', value: 0 },
    { name: '意志', value: 0 },
    { name: '体质', value: 0 },
    { name: '外貌', value: 0 },
    { name: '教育', value: 0 },
    { name: '体型', value: 0 },
    { name: '智力', value: 0 },
    { name: '幸运', value: 0 }
  ];
}

export function emptyPayload(): BackupPayload {
  return {
    version: 1,
    exportedAt: 0,
    modules: [],
    timelineNodes: [],
    locations: [],
    npcs: [],
    pcs: [],
    clues: [],
    combatants: [],
    chaseParticipants: [],
    chasePoints: [],
    files: []
  };
}
