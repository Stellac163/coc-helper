// 实体工厂：对应 Kotlin 数据类的默认构造（id=0 由 insert 分配，createdAt=当前时间，updatedAt=0）。
import { now } from '../utils/id';
import {
  defaultSkills,
  defaultAttributes,
  FILE_COMPANION,
  COMBATANT_ALIVE,
  CHASE_QUARRY,
  type ModuleEntity,
  type TimelineNodeEntity,
  type LocationEntity,
  type NpcEntity,
  type PcEntity,
  type ClueEntity,
  type FileEntity,
  type CombatantEntity,
  type ChasePointEntity,
  type ChaseParticipantEntity
} from './types';

export function newModule(name: string, description = ''): ModuleEntity {
  return {
    id: 0, name, description,
    isActive: false, hasOriginalDoc: false, introText: '', photoUri: '',
    createdAt: now(), updatedAt: 0
  };
}

export function newTimeline(moduleId: number, title: string, parentId: number | null = null, order = 0): TimelineNodeEntity {
  return { id: 0, moduleId, title, content: '', parentId, order, createdAt: now(), updatedAt: 0 };
}

export function newLocation(moduleId: number, name: string): LocationEntity {
  return { id: 0, moduleId, name, content: '', createdAt: now(), updatedAt: 0 };
}

export function newNpc(moduleId: number, name: string): NpcEntity {
  return { id: 0, moduleId, name, content: '', createdAt: now(), updatedAt: 0 };
}

export function newPc(name: string, moduleId: number | null = null): PcEntity {
  return {
    id: 0, moduleId, name,
    player: '', gender: '', age: '',
    skills: defaultSkills(), attributes: defaultAttributes(),
    appearance: '', beliefs: '', importantPeople: '', importantPlaces: '',
    valuables: '', traits: '', wounds: '', phobias: '', background: '',
    imageUri: '', createdAt: now(), updatedAt: 0
  };
}

export function newClue(title: string, content = '', order = 0): ClueEntity {
  return { id: 0, title, content, order, createdAt: now(), updatedAt: 0 };
}

export function newFile(
  moduleId: number, name: string, mimeType: string,
  contentBase64: string, sizeBytes: number, kind = FILE_COMPANION
): FileEntity {
  return { id: 0, moduleId, name, mimeType, contentBase64, sizeBytes, kind, createdAt: now(), updatedAt: 0 };
}

export function newCombatant(name: string, dex = 50, hp = 10, maxHp = 10, ranged = false): CombatantEntity {
  return { id: 0, name, dex, hp, maxHp, status: COMBATANT_ALIVE, ranged, createdAt: now(), updatedAt: 0 };
}

export function newChasePoint(name: string, order = 0): ChasePointEntity {
  return { id: 0, name, order, createdAt: now(), updatedAt: 0 };
}

export function newChaseParticipant(name: string, mov = 8, role = CHASE_QUARRY): ChaseParticipantEntity {
  return { id: 0, name, mov, pointId: 0, role, createdAt: now(), updatedAt: 0 };
}
