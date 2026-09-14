import {
  type BackupPayload,
  type ModuleEntity,
  type TimelineNodeEntity,
  type LocationEntity,
  type NpcEntity,
  type PcEntity,
  type ClueEntity,
  type FileEntity,
  type CombatantEntity,
  type ChaseParticipantEntity,
  type ChasePointEntity,
  type SkillItem,
  type AttributeItem,
  emptyPayload,
  defaultSkills,
  defaultAttributes
} from './types';
import { BlobStore } from './blobStore';
import { nextId, now } from '../utils/id';
import * as github from '../sync/github';
import type { HttpProgress } from '../sync/github';

const STORAGE_KEY = 'cochelper_data_v1';

const fileKey = (id: number) => `file_${id}`;
const modulePhotoKey = (id: number) => `modulePhoto_${id}`;
const pcImageKey = (id: number) => `pcImage_${id}`;

function blobMap(p: BackupPayload): Record<string, string> {
  const map: Record<string, string> = {};
  for (const f of p.files) if (f.contentBase64) map[fileKey(f.id)] = f.contentBase64;
  for (const m of p.modules) if (m.photoUri) map[modulePhotoKey(m.id)] = m.photoUri;
  for (const pc of p.pcs) if (pc.imageUri) map[pcImageKey(pc.id)] = pc.imageUri;
  return map;
}

function stripped(p: BackupPayload): BackupPayload {
  return {
    ...p,
    files: p.files.map((f) => (f.contentBase64 ? { ...f, contentBase64: '' } : f)),
    modules: p.modules.map((m) => (m.photoUri ? { ...m, photoUri: '' } : m)),
    pcs: p.pcs.map((pc) => (pc.imageUri ? { ...pc, imageUri: '' } : pc))
  };
}

function hydrated(p: BackupPayload, blobs: Record<string, string>): BackupPayload {
  return {
    ...p,
    files: p.files.map((f) => {
      const b = blobs[fileKey(f.id)];
      return b ? { ...f, contentBase64: b } : f;
    }),
    modules: p.modules.map((m) => {
      const b = blobs[modulePhotoKey(m.id)];
      return b ? { ...m, photoUri: b } : m;
    }),
    pcs: p.pcs.map((pc) => {
      const b = blobs[pcImageKey(pc.id)];
      return b ? { ...pc, imageUri: b } : pc;
    })
  };
}

// —— 载入/恢复时的容错清洗：字段缺失/类型不对时回退默认值，避免旧数据或损坏数据让界面崩掉 ——

const num = (v: unknown, d = 0): number => {
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string' && v.trim() !== '') {
    const n = Number(v);
    return Number.isFinite(n) ? n : d;
  }
  return d;
};
const str = (v: unknown, d = ''): string => (typeof v === 'string' ? v : d);
const bool = (v: unknown): boolean => v === true;
const numOrNull = (v: unknown): number | null => (v == null ? null : num(v, 0));

function sanitizeSkill(v: unknown): SkillItem {
  const o = (v ?? {}) as Record<string, unknown>;
  return { name: str(o.name), value: num(o.value) };
}
function sanitizeAttr(v: unknown): AttributeItem {
  const o = (v ?? {}) as Record<string, unknown>;
  return { name: str(o.name), value: num(o.value) };
}

function sanitizeModule(v: unknown): ModuleEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    name: str(o.name),
    description: str(o.description),
    isActive: bool(o.isActive),
    hasOriginalDoc: bool(o.hasOriginalDoc),
    introText: str(o.introText),
    photoUri: str(o.photoUri),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeTimeline(v: unknown): TimelineNodeEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    moduleId: num(o.moduleId),
    title: str(o.title),
    content: str(o.content),
    parentId: numOrNull(o.parentId),
    order: num(o.order),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeLocation(v: unknown): LocationEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    moduleId: num(o.moduleId),
    name: str(o.name),
    content: str(o.content),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeNpc(v: unknown): NpcEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    moduleId: num(o.moduleId),
    name: str(o.name),
    content: str(o.content),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizePc(v: unknown): PcEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  const skills = Array.isArray(o.skills)
    ? (o.skills as unknown[]).map(sanitizeSkill)
    : defaultSkills();
  const attributes = Array.isArray(o.attributes)
    ? (o.attributes as unknown[]).map(sanitizeAttr)
    : defaultAttributes();
  return {
    id: num(o.id),
    moduleId: numOrNull(o.moduleId),
    name: str(o.name),
    player: str(o.player),
    gender: str(o.gender),
    age: str(o.age),
    skills,
    attributes,
    appearance: str(o.appearance),
    beliefs: str(o.beliefs),
    importantPeople: str(o.importantPeople),
    importantPlaces: str(o.importantPlaces),
    valuables: str(o.valuables),
    traits: str(o.traits),
    wounds: str(o.wounds),
    phobias: str(o.phobias),
    background: str(o.background),
    imageUri: str(o.imageUri),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeClue(v: unknown): ClueEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    title: str(o.title),
    content: str(o.content),
    order: num(o.order),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeFile(v: unknown): FileEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    moduleId: num(o.moduleId),
    name: str(o.name),
    mimeType: str(o.mimeType),
    contentBase64: str(o.contentBase64),
    sizeBytes: num(o.sizeBytes),
    kind: num(o.kind),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeCombatant(v: unknown): CombatantEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    name: str(o.name),
    dex: num(o.dex),
    hp: num(o.hp),
    maxHp: num(o.maxHp),
    status: num(o.status),
    ranged: bool(o.ranged),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeChasePoint(v: unknown): ChasePointEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    name: str(o.name),
    order: num(o.order),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}
function sanitizeChaseParticipant(v: unknown): ChaseParticipantEntity {
  const o = (v ?? {}) as Record<string, unknown>;
  return {
    id: num(o.id),
    name: str(o.name),
    mov: num(o.mov),
    pointId: num(o.pointId),
    role: num(o.role),
    createdAt: num(o.createdAt),
    updatedAt: num(o.updatedAt)
  };
}

function sanitizePayload(raw: unknown): BackupPayload {
  const o = (raw ?? {}) as Record<string, unknown>;
  const list = (v: unknown) => (Array.isArray(v) ? (v as unknown[]) : []);
  return {
    version: num(o.version, 1),
    exportedAt: num(o.exportedAt),
    modules: list(o.modules).map(sanitizeModule),
    timelineNodes: list(o.timelineNodes).map(sanitizeTimeline),
    locations: list(o.locations).map(sanitizeLocation),
    npcs: list(o.npcs).map(sanitizeNpc),
    pcs: list(o.pcs).map(sanitizePc),
    clues: list(o.clues).map(sanitizeClue),
    combatants: list(o.combatants).map(sanitizeCombatant),
    chaseParticipants: list(o.chaseParticipants).map(sanitizeChaseParticipant),
    chasePoints: list(o.chasePoints).map(sanitizeChasePoint),
    files: list(o.files).map(sanitizeFile)
  };
}

function isEmptyData(p: BackupPayload): boolean {
  return (
    p.modules.length === 0 &&
    p.timelineNodes.length === 0 &&
    p.locations.length === 0 &&
    p.npcs.length === 0 &&
    p.pcs.length === 0 &&
    p.clues.length === 0 &&
    p.combatants.length === 0 &&
    p.chaseParticipants.length === 0 &&
    p.chasePoints.length === 0 &&
    p.files.length === 0
  );
}

function loadLocal(): BackupPayload {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return emptyPayload();
    return sanitizePayload(JSON.parse(raw));
  } catch {
    return emptyPayload();
  }
}

/**
 * 单一数据源：整份 BackupPayload 持于一个 $state，任何变更即时反映到 UI，
 * 落盘走防抖（400ms）异步写 IndexedDB + localStorage，保证交互丝滑。
 * 与原版 InMemoryRepository 一致：无自动同步（同步由登录页手动触发）。
 */
class Repository {
  data = $state<BackupPayload>(emptyPayload());
  ready = $state(false);

  private blobs = new BlobStore();
  private flushTimer: ReturnType<typeof setTimeout> | null = null;
  private writeChain: Promise<void> = Promise.resolve();
  private syncChain: Promise<unknown> = Promise.resolve();

  async init(): Promise<void> {
    const base = loadLocal();
    let full = base;
    try {
      const blobs = await this.blobs.getAll();
      if (Object.keys(blobs).length > 0) full = hydrated(base, blobs);
    } catch {
      // 读 IndexedDB 失败则退回「瘦」快照（大字段缺失，但结构化数据仍可用）
    }
    this.data = full;
    this.ready = true;
  }

  // —— 持久化 ——

  private schedulePersist() {
    if (this.flushTimer) clearTimeout(this.flushTimer);
    this.flushTimer = setTimeout(() => {
      this.flushTimer = null;
      this.enqueueWrite();
    }, 400);
  }

  /** 快照当前状态并入队落盘；按入队顺序串行执行，保证最终写盘为最新状态。 */
  private enqueueWrite() {
    const full = this.data;
    this.writeChain = this.writeChain
      .then(async () => {
        try {
          await this.blobs.putAll(blobMap(full));
          this.writeLocal(stripped(full));
        } catch {
          this.writeLocal(full); // IndexedDB 失败 → 整份回退 localStorage
        }
      })
      .catch(() => {});
  }

  private writeLocal(p: BackupPayload) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(p));
    } catch (e) {
      console.warn('[persist] localStorage 写入失败（本次变更仅在当前会话内生效）', e);
    }
  }

  async flushNow(): Promise<void> {
    if (this.flushTimer) {
      clearTimeout(this.flushTimer);
      this.flushTimer = null;
    }
    this.enqueueWrite();
    await this.writeChain;
  }

  private mutate(fn: (p: BackupPayload) => BackupPayload) {
    this.data = fn(this.data);
    this.schedulePersist();
  }

  // —— 容器级操作 ——

  /** 用云端快照整体覆盖本地（不触发任何自动同步）。 */
  async replaceAll(payload: BackupPayload): Promise<void> {
    this.data = sanitizePayload(payload);
    await this.flushNow();
  }

  exportBackup(): BackupPayload {
    return {
      version: 1,
      exportedAt: now(),
      modules: this.data.modules,
      timelineNodes: this.data.timelineNodes,
      locations: this.data.locations,
      npcs: this.data.npcs,
      pcs: this.data.pcs,
      clues: this.data.clues,
      combatants: this.data.combatants,
      chaseParticipants: this.data.chaseParticipants,
      chasePoints: this.data.chasePoints,
      files: this.data.files
    };
  }

  /** 删除模组及其全部关联数据（时间轴/地点/npc/pc/文件）。 */
  deleteModule(moduleId: number) {
    this.mutate((p) => ({
      ...p,
      timelineNodes: p.timelineNodes.filter((n) => n.moduleId !== moduleId),
      locations: p.locations.filter((l) => l.moduleId !== moduleId),
      npcs: p.npcs.filter((n) => n.moduleId !== moduleId),
      pcs: p.pcs.filter((pc) => pc.moduleId !== moduleId),
      files: p.files.filter((f) => f.moduleId !== moduleId),
      modules: p.modules.filter((m) => m.id !== moduleId)
    }));
  }

  importBackup(payload: BackupPayload): Promise<void> {
    return this.replaceAll(payload);
  }

  async uploadToCloud(
    token: string,
    repoName: string,
    onProgress?: (p: HttpProgress) => void
  ): Promise<string> {
    return this.withSyncLock(async () => {
      const full = await github.ensureRepo(token, repoName);
      const local = this.exportBackup();
      if (isEmptyData(local)) return '本地无数据，未上传（以免清空云端）';
      await github.pushBackup(token, full, local, onProgress);
      return '已上传，云端已被本地覆盖';
    });
  }

  async restoreFromCloud(
    token: string,
    repoName: string,
    onProgress?: (p: HttpProgress) => void
  ): Promise<string> {
    return this.withSyncLock(async () => {
      const full = await github.ensureRepo(token, repoName);
      const payload = await github.pullBackup(token, full, onProgress);
      await this.importBackup(payload);
      return '已从云端恢复，本地已被覆盖';
    });
  }

  private withSyncLock<T>(fn: () => Promise<T>): Promise<T> {
    const run = this.syncChain.then(fn);
    this.syncChain = run.catch(() => {});
    return run;
  }

  // —— DAO（分组方法，对应原版各 DaoImpl） ——

  modules = {
    all: (): ModuleEntity[] => [...this.data.modules].sort((a, b) => b.createdAt - a.createdAt),
    byId: (id: number): ModuleEntity | undefined => this.data.modules.find((m) => m.id === id),
    getById: (id: number): ModuleEntity | undefined => this.data.modules.find((m) => m.id === id),
    active: (): ModuleEntity | undefined => this.data.modules.find((m) => m.isActive),
    getAll: (): ModuleEntity[] => this.data.modules,
    insert: (m: ModuleEntity): number => {
      const id = m.id === 0 ? nextId() : m.id;
      this.mutate((p) => ({
        ...p,
        modules: [...p.modules, { ...m, id, updatedAt: m.updatedAt === 0 ? now() : m.updatedAt }]
      }));
      return id;
    },
    update: (m: ModuleEntity) =>
      this.mutate((p) => ({
        ...p,
        modules: p.modules.map((x) => (x.id === m.id ? { ...m, updatedAt: now() } : x))
      })),
    delete: (m: ModuleEntity) =>
      this.mutate((p) => ({ ...p, modules: p.modules.filter((x) => x.id !== m.id) })),
    clearActive: () =>
      this.mutate((p) => ({ ...p, modules: p.modules.map((x) => ({ ...x, isActive: false })) })),
    setActive: (id: number) =>
      this.mutate((p) => ({
        ...p,
        modules: p.modules.map((x) => ({ ...x, isActive: x.id === id }))
      })),
    deleteAll: () => this.mutate((p) => ({ ...p, modules: [] }))
  };

  timeline = {
    forModule: (moduleId: number): TimelineNodeEntity[] =>
      this.data.timelineNodes
        .filter((n) => n.moduleId === moduleId)
        .sort((a, b) => {
          const ap = a.parentId ?? Number.NEGATIVE_INFINITY;
          const bp = b.parentId ?? Number.NEGATIVE_INFINITY;
          if (ap !== bp) return ap < bp ? -1 : 1;
          if (a.order !== b.order) return a.order - b.order;
          return a.createdAt - b.createdAt;
        }),
    getAll: (moduleId: number): TimelineNodeEntity[] =>
      this.data.timelineNodes.filter((n) => n.moduleId === moduleId),
    getAllForExport: (): TimelineNodeEntity[] => this.data.timelineNodes,
    insert: (n: TimelineNodeEntity): number => {
      const id = n.id === 0 ? nextId() : n.id;
      this.mutate((p) => ({
        ...p,
        timelineNodes: [
          ...p.timelineNodes,
          { ...n, id, updatedAt: n.updatedAt === 0 ? now() : n.updatedAt }
        ]
      }));
      return id;
    },
    update: (n: TimelineNodeEntity) =>
      this.mutate((p) => ({
        ...p,
        timelineNodes: p.timelineNodes.map((x) => (x.id === n.id ? { ...n, updatedAt: now() } : x))
      })),
    delete: (n: TimelineNodeEntity) =>
      this.mutate((p) => ({ ...p, timelineNodes: p.timelineNodes.filter((x) => x.id !== n.id) })),
    deleteForModule: (moduleId: number) =>
      this.mutate((p) => ({
        ...p,
        timelineNodes: p.timelineNodes.filter((x) => x.moduleId !== moduleId)
      })),
    deleteAllForAll: () => this.mutate((p) => ({ ...p, timelineNodes: [] }))
  };

  locations = {
    forModule: (moduleId: number): LocationEntity[] =>
      this.data.locations
        .filter((l) => l.moduleId === moduleId)
        .sort((a, b) => b.createdAt - a.createdAt),
    byId: (id: number): LocationEntity | undefined => this.data.locations.find((l) => l.id === id),
    getById: (id: number): LocationEntity | undefined => this.data.locations.find((l) => l.id === id),
    insert: (l: LocationEntity): number => {
      const id = l.id === 0 ? nextId() : l.id;
      this.mutate((p) => ({
        ...p,
        locations: [...p.locations, { ...l, id, updatedAt: l.updatedAt === 0 ? now() : l.updatedAt }]
      }));
      return id;
    },
    update: (l: LocationEntity) =>
      this.mutate((p) => ({
        ...p,
        locations: p.locations.map((x) => (x.id === l.id ? { ...l, updatedAt: now() } : x))
      })),
    delete: (l: LocationEntity) =>
      this.mutate((p) => ({ ...p, locations: p.locations.filter((x) => x.id !== l.id) })),
    deleteForModule: (moduleId: number) =>
      this.mutate((p) => ({ ...p, locations: p.locations.filter((x) => x.moduleId !== moduleId) })),
    getAll: (): LocationEntity[] => this.data.locations,
    deleteAllForAll: () => this.mutate((p) => ({ ...p, locations: [] }))
  };

  npcs = {
    forModule: (moduleId: number): NpcEntity[] =>
      this.data.npcs.filter((n) => n.moduleId === moduleId).sort((a, b) => b.createdAt - a.createdAt),
    byId: (id: number): NpcEntity | undefined => this.data.npcs.find((n) => n.id === id),
    getById: (id: number): NpcEntity | undefined => this.data.npcs.find((n) => n.id === id),
    insert: (n: NpcEntity): number => {
      const id = n.id === 0 ? nextId() : n.id;
      this.mutate((p) => ({
        ...p,
        npcs: [...p.npcs, { ...n, id, updatedAt: n.updatedAt === 0 ? now() : n.updatedAt }]
      }));
      return id;
    },
    update: (n: NpcEntity) =>
      this.mutate((p) => ({
        ...p,
        npcs: p.npcs.map((x) => (x.id === n.id ? { ...n, updatedAt: now() } : x))
      })),
    delete: (n: NpcEntity) =>
      this.mutate((p) => ({ ...p, npcs: p.npcs.filter((x) => x.id !== n.id) })),
    deleteForModule: (moduleId: number) =>
      this.mutate((p) => ({ ...p, npcs: p.npcs.filter((x) => x.moduleId !== moduleId) })),
    getAll: (): NpcEntity[] => this.data.npcs,
    deleteAllForAll: () => this.mutate((p) => ({ ...p, npcs: [] }))
  };

  pcs = {
    forModule: (moduleId: number): PcEntity[] =>
      this.data.pcs.filter((pc) => pc.moduleId === moduleId).sort((a, b) => b.createdAt - a.createdAt),
    all: (): PcEntity[] => [...this.data.pcs].sort((a, b) => b.createdAt - a.createdAt),
    playerPcs: (): PcEntity[] =>
      this.data.pcs.filter((pc) => pc.moduleId === null).sort((a, b) => b.createdAt - a.createdAt),
    byId: (id: number): PcEntity | undefined => this.data.pcs.find((pc) => pc.id === id),
    getById: (id: number): PcEntity | undefined => this.data.pcs.find((pc) => pc.id === id),
    insert: (pc: PcEntity): number => {
      const id = pc.id === 0 ? nextId() : pc.id;
      this.mutate((p) => ({
        ...p,
        pcs: [...p.pcs, { ...pc, id, updatedAt: pc.updatedAt === 0 ? now() : pc.updatedAt }]
      }));
      return id;
    },
    update: (pc: PcEntity) =>
      this.mutate((p) => ({
        ...p,
        pcs: p.pcs.map((x) => (x.id === pc.id ? { ...pc, updatedAt: now() } : x))
      })),
    delete: (pc: PcEntity) =>
      this.mutate((p) => ({ ...p, pcs: p.pcs.filter((x) => x.id !== pc.id) })),
    deleteForModule: (moduleId: number) =>
      this.mutate((p) => ({ ...p, pcs: p.pcs.filter((x) => x.moduleId !== moduleId) })),
    getAll: (): PcEntity[] => this.data.pcs,
    deleteAllForAll: () => this.mutate((p) => ({ ...p, pcs: [] }))
  };

  clues = {
    all: (): ClueEntity[] => [...this.data.clues].sort((a, b) => a.order - b.order),
    getAll: (): ClueEntity[] => this.data.clues,
    insert: (c: ClueEntity): number => {
      const id = c.id === 0 ? nextId() : c.id;
      this.mutate((p) => ({
        ...p,
        clues: [...p.clues, { ...c, id, updatedAt: c.updatedAt === 0 ? now() : c.updatedAt }]
      }));
      return id;
    },
    update: (c: ClueEntity) =>
      this.mutate((p) => ({
        ...p,
        clues: p.clues.map((x) => (x.id === c.id ? { ...c, updatedAt: now() } : x))
      })),
    delete: (c: ClueEntity) =>
      this.mutate((p) => ({ ...p, clues: p.clues.filter((x) => x.id !== c.id) })),
    deleteAll: () => this.mutate((p) => ({ ...p, clues: [] }))
  };

  files = {
    forModule: (moduleId: number): FileEntity[] =>
      this.data.files.filter((f) => f.moduleId === moduleId).sort((a, b) => b.createdAt - a.createdAt),
    forModuleKind: (moduleId: number, kind: number): FileEntity[] =>
      this.data.files
        .filter((f) => f.moduleId === moduleId && f.kind === kind)
        .sort((a, b) => b.createdAt - a.createdAt),
    getAll: (): FileEntity[] => this.data.files,
    insert: (f: FileEntity): number => {
      const id = f.id === 0 ? nextId() : f.id;
      this.mutate((p) => ({
        ...p,
        files: [...p.files, { ...f, id, updatedAt: f.updatedAt === 0 ? now() : f.updatedAt }]
      }));
      return id;
    },
    delete: (f: FileEntity) =>
      this.mutate((p) => ({ ...p, files: p.files.filter((x) => x.id !== f.id) })),
    deleteForModule: (moduleId: number) =>
      this.mutate((p) => ({ ...p, files: p.files.filter((x) => x.moduleId !== moduleId) })),
    deleteAllForAll: () => this.mutate((p) => ({ ...p, files: [] }))
  };

  combat = {
    all: (): CombatantEntity[] =>
      [...this.data.combatants].sort((a, b) => {
        if (a.ranged !== b.ranged) return a.ranged ? -1 : 1;
        if (a.dex !== b.dex) return b.dex - a.dex;
        return a.createdAt - b.createdAt;
      }),
    getAll: (): CombatantEntity[] => this.data.combatants,
    insert: (c: CombatantEntity): number => {
      const id = c.id === 0 ? nextId() : c.id;
      this.mutate((p) => ({
        ...p,
        combatants: [...p.combatants, { ...c, id, updatedAt: c.updatedAt === 0 ? now() : c.updatedAt }]
      }));
      return id;
    },
    update: (c: CombatantEntity) =>
      this.mutate((p) => ({
        ...p,
        combatants: p.combatants.map((x) => (x.id === c.id ? { ...c, updatedAt: now() } : x))
      })),
    delete: (c: CombatantEntity) =>
      this.mutate((p) => ({ ...p, combatants: p.combatants.filter((x) => x.id !== c.id) })),
    deleteAll: () => this.mutate((p) => ({ ...p, combatants: [] }))
  };

  chase = {
    all: (): ChaseParticipantEntity[] => [...this.data.chaseParticipants].sort((a, b) => a.createdAt - b.createdAt),
    getAll: (): ChaseParticipantEntity[] => this.data.chaseParticipants,
    insert: (c: ChaseParticipantEntity): number => {
      const id = c.id === 0 ? nextId() : c.id;
      this.mutate((p) => ({
        ...p,
        chaseParticipants: [
          ...p.chaseParticipants,
          { ...c, id, updatedAt: c.updatedAt === 0 ? now() : c.updatedAt }
        ]
      }));
      return id;
    },
    update: (c: ChaseParticipantEntity) =>
      this.mutate((p) => ({
        ...p,
        chaseParticipants: p.chaseParticipants.map((x) =>
          x.id === c.id ? { ...c, updatedAt: now() } : x
        )
      })),
    delete: (c: ChaseParticipantEntity) =>
      this.mutate((p) => ({
        ...p,
        chaseParticipants: p.chaseParticipants.filter((x) => x.id !== c.id)
      })),
    deleteAll: () => this.mutate((p) => ({ ...p, chaseParticipants: [] }))
  };

  chasePoints = {
    all: (): ChasePointEntity[] =>
      [...this.data.chasePoints].sort((a, b) => (a.order !== b.order ? a.order - b.order : a.createdAt - b.createdAt)),
    getAll: (): ChasePointEntity[] => this.data.chasePoints,
    insert: (pt: ChasePointEntity): number => {
      const id = pt.id === 0 ? nextId() : pt.id;
      this.mutate((p) => ({
        ...p,
        chasePoints: [...p.chasePoints, { ...pt, id, updatedAt: pt.updatedAt === 0 ? now() : pt.updatedAt }]
      }));
      return id;
    },
    update: (pt: ChasePointEntity) =>
      this.mutate((p) => ({
        ...p,
        chasePoints: p.chasePoints.map((x) => (x.id === pt.id ? { ...pt, updatedAt: now() } : x))
      })),
    delete: (pt: ChasePointEntity) =>
      this.mutate((p) => ({ ...p, chasePoints: p.chasePoints.filter((x) => x.id !== pt.id) })),
    deleteAll: () => this.mutate((p) => ({ ...p, chasePoints: [] }))
  };
}

export const repo = new Repository();
