package com.cochelper.app.data.local

import kotlinx.coroutines.flow.Flow

interface ModuleDao {
    fun observeAll(): Flow<List<ModuleEntity>>
    fun observeById(id: Long): Flow<ModuleEntity?>
    suspend fun getById(id: Long): ModuleEntity?
    suspend fun getActive(): ModuleEntity?
    suspend fun getAll(): List<ModuleEntity>
    suspend fun insert(m: ModuleEntity): Long
    suspend fun update(m: ModuleEntity)
    suspend fun delete(m: ModuleEntity)
    suspend fun clearActive()
    suspend fun setActive(id: Long)
    suspend fun deleteAll()
}

interface TimelineDao {
    fun observeForModule(moduleId: Long): Flow<List<TimelineNodeEntity>>
    suspend fun getAll(moduleId: Long): List<TimelineNodeEntity>
    suspend fun getAllForExport(): List<TimelineNodeEntity>
    suspend fun insert(n: TimelineNodeEntity): Long
    suspend fun update(n: TimelineNodeEntity)
    suspend fun delete(n: TimelineNodeEntity)
    suspend fun deleteForModule(moduleId: Long)
    suspend fun deleteAllForAll()
}

interface LocationDao {
    fun observeForModule(moduleId: Long): Flow<List<LocationEntity>>
    fun observeById(id: Long): Flow<LocationEntity?>
    suspend fun getById(id: Long): LocationEntity?
    suspend fun insert(l: LocationEntity): Long
    suspend fun update(l: LocationEntity)
    suspend fun delete(l: LocationEntity)
    suspend fun deleteForModule(moduleId: Long)
    suspend fun getAll(): List<LocationEntity>
    suspend fun deleteAllForAll()
}

interface NpcDao {
    fun observeForModule(moduleId: Long): Flow<List<NpcEntity>>
    fun observeById(id: Long): Flow<NpcEntity?>
    suspend fun getById(id: Long): NpcEntity?
    suspend fun insert(n: NpcEntity): Long
    suspend fun update(n: NpcEntity)
    suspend fun delete(n: NpcEntity)
    suspend fun deleteForModule(moduleId: Long)
    suspend fun getAll(): List<NpcEntity>
    suspend fun deleteAllForAll()
}

interface PcDao {
    fun observeForModule(moduleId: Long): Flow<List<PcEntity>>
    fun observeAll(): Flow<List<PcEntity>>
    fun observePlayerPcs(): Flow<List<PcEntity>>
    fun observeById(id: Long): Flow<PcEntity?>
    suspend fun getById(id: Long): PcEntity?
    suspend fun insert(p: PcEntity): Long
    suspend fun update(p: PcEntity)
    suspend fun delete(p: PcEntity)
    suspend fun deleteForModule(moduleId: Long)
    suspend fun getAll(): List<PcEntity>
    suspend fun deleteAllForAll()
}

interface ClueDao {
    fun observeAll(): Flow<List<ClueEntity>>
    suspend fun getAll(): List<ClueEntity>
    suspend fun insert(c: ClueEntity): Long
    suspend fun update(c: ClueEntity)
    suspend fun delete(c: ClueEntity)
    suspend fun deleteAll()
}

interface FileDao {
    fun observeForModule(moduleId: Long): Flow<List<FileEntity>>
    fun observeForModuleKind(moduleId: Long, kind: Int): Flow<List<FileEntity>>
    suspend fun getAll(): List<FileEntity>
    suspend fun insert(f: FileEntity): Long
    suspend fun delete(f: FileEntity)
    suspend fun deleteForModule(moduleId: Long)
    suspend fun deleteAllForAll()
}

interface CombatantDao {
    fun observeAll(): Flow<List<CombatantEntity>>
    suspend fun getAll(): List<CombatantEntity>
    suspend fun insert(c: CombatantEntity): Long
    suspend fun update(c: CombatantEntity)
    suspend fun delete(c: CombatantEntity)
    suspend fun deleteAll()
}

interface ChaseParticipantDao {
    fun observeAll(): Flow<List<ChaseParticipantEntity>>
    suspend fun getAll(): List<ChaseParticipantEntity>
    suspend fun insert(c: ChaseParticipantEntity): Long
    suspend fun update(c: ChaseParticipantEntity)
    suspend fun delete(c: ChaseParticipantEntity)
    suspend fun deleteAll()
}

interface ChasePointDao {
    fun observeAll(): Flow<List<ChasePointEntity>>
    suspend fun getAll(): List<ChasePointEntity>
    suspend fun insert(p: ChasePointEntity): Long
    suspend fun update(p: ChasePointEntity)
    suspend fun delete(p: ChasePointEntity)
    suspend fun deleteAll()
}
