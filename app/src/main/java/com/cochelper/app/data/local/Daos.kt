package com.cochelper.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {
    @Query("SELECT * FROM modules ORDER BY createdAt DESC") fun observeAll(): Flow<List<ModuleEntity>>
    @Query("SELECT * FROM modules WHERE id = :id") fun observeById(id: Long): Flow<ModuleEntity?>
    @Query("SELECT * FROM modules WHERE id = :id") suspend fun getById(id: Long): ModuleEntity?
    @Query("SELECT * FROM modules WHERE isActive = 1 LIMIT 1") suspend fun getActive(): ModuleEntity?
    @Query("SELECT * FROM modules") suspend fun getAll(): List<ModuleEntity>
    @Insert suspend fun insert(m: ModuleEntity): Long
    @Update suspend fun update(m: ModuleEntity)
    @Delete suspend fun delete(m: ModuleEntity)
    @Query("UPDATE modules SET isActive = 0") suspend fun clearActive()
    @Query("UPDATE modules SET isActive = 1 WHERE id = :id") suspend fun setActive(id: Long)
    @Query("DELETE FROM modules") suspend fun deleteAll()
}

@Dao
interface TimelineDao {
    @Query("SELECT * FROM timeline_nodes WHERE moduleId = :moduleId ORDER BY parentId, [order], createdAt")
    fun observeForModule(moduleId: Long): Flow<List<TimelineNodeEntity>>
    @Query("SELECT * FROM timeline_nodes WHERE moduleId = :moduleId") suspend fun getAll(moduleId: Long): List<TimelineNodeEntity>
    @Query("SELECT * FROM timeline_nodes") suspend fun getAllForExport(): List<TimelineNodeEntity>
    @Insert suspend fun insert(n: TimelineNodeEntity): Long
    @Update suspend fun update(n: TimelineNodeEntity)
    @Delete suspend fun delete(n: TimelineNodeEntity)
    @Query("DELETE FROM timeline_nodes WHERE moduleId = :moduleId") suspend fun deleteForModule(moduleId: Long)
    @Query("DELETE FROM timeline_nodes") suspend fun deleteAllForAll()
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations WHERE moduleId = :moduleId ORDER BY createdAt") fun observeForModule(moduleId: Long): Flow<List<LocationEntity>>
    @Query("SELECT * FROM locations WHERE id = :id") fun observeById(id: Long): Flow<LocationEntity?>
    @Query("SELECT * FROM locations WHERE id = :id") suspend fun getById(id: Long): LocationEntity?
    @Insert suspend fun insert(l: LocationEntity): Long
    @Update suspend fun update(l: LocationEntity)
    @Delete suspend fun delete(l: LocationEntity)
    @Query("DELETE FROM locations WHERE moduleId = :moduleId") suspend fun deleteForModule(moduleId: Long)
    @Query("SELECT * FROM locations") suspend fun getAll(): List<LocationEntity>
    @Query("DELETE FROM locations") suspend fun deleteAllForAll()
}

@Dao
interface NpcDao {
    @Query("SELECT * FROM npcs WHERE moduleId = :moduleId ORDER BY createdAt") fun observeForModule(moduleId: Long): Flow<List<NpcEntity>>
    @Query("SELECT * FROM npcs WHERE id = :id") fun observeById(id: Long): Flow<NpcEntity?>
    @Query("SELECT * FROM npcs WHERE id = :id") suspend fun getById(id: Long): NpcEntity?
    @Insert suspend fun insert(n: NpcEntity): Long
    @Update suspend fun update(n: NpcEntity)
    @Delete suspend fun delete(n: NpcEntity)
    @Query("DELETE FROM npcs WHERE moduleId = :moduleId") suspend fun deleteForModule(moduleId: Long)
    @Query("SELECT * FROM npcs") suspend fun getAll(): List<NpcEntity>
    @Query("DELETE FROM npcs") suspend fun deleteAllForAll()
}

@Dao
interface PcDao {
    @Query("SELECT * FROM pcs WHERE moduleId = :moduleId ORDER BY createdAt") fun observeForModule(moduleId: Long): Flow<List<PcEntity>>
    @Query("SELECT * FROM pcs ORDER BY createdAt DESC") fun observeAll(): Flow<List<PcEntity>>
    @Query("SELECT * FROM pcs WHERE id = :id") fun observeById(id: Long): Flow<PcEntity?>
    @Query("SELECT * FROM pcs WHERE id = :id") suspend fun getById(id: Long): PcEntity?
    @Insert suspend fun insert(p: PcEntity): Long
    @Update suspend fun update(p: PcEntity)
    @Delete suspend fun delete(p: PcEntity)
    @Query("DELETE FROM pcs WHERE moduleId = :moduleId") suspend fun deleteForModule(moduleId: Long)
    @Query("SELECT * FROM pcs") suspend fun getAll(): List<PcEntity>
    @Query("DELETE FROM pcs") suspend fun deleteAllForAll()
}

@Dao
interface ClueDao {
    @Query("SELECT * FROM clues ORDER BY [order], createdAt") fun observeAll(): Flow<List<ClueEntity>>
    @Query("SELECT * FROM clues") suspend fun getAll(): List<ClueEntity>
    @Insert suspend fun insert(c: ClueEntity): Long
    @Update suspend fun update(c: ClueEntity)
    @Delete suspend fun delete(c: ClueEntity)
    @Query("DELETE FROM clues") suspend fun deleteAll()
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE moduleId = :moduleId ORDER BY createdAt DESC") fun observeForModule(moduleId: Long): Flow<List<FileEntity>>
    @Query("SELECT * FROM files WHERE moduleId = :moduleId AND kind = :kind ORDER BY createdAt DESC") fun observeForModuleKind(moduleId: Long, kind: Int): Flow<List<FileEntity>>
    @Query("SELECT * FROM files") suspend fun getAll(): List<FileEntity>
    @Insert suspend fun insert(f: FileEntity): Long
    @Delete suspend fun delete(f: FileEntity)
    @Query("DELETE FROM files WHERE moduleId = :moduleId") suspend fun deleteForModule(moduleId: Long)
    @Query("DELETE FROM files") suspend fun deleteAllForAll()
}

@Dao
interface CombatantDao {
    @Query("SELECT * FROM combatants ORDER BY dex DESC, createdAt") fun observeAll(): Flow<List<CombatantEntity>>
    @Query("SELECT * FROM combatants") suspend fun getAll(): List<CombatantEntity>
    @Insert suspend fun insert(c: CombatantEntity): Long
    @Update suspend fun update(c: CombatantEntity)
    @Delete suspend fun delete(c: CombatantEntity)
    @Query("DELETE FROM combatants") suspend fun deleteAll()
}

@Dao
interface ChaseParticipantDao {
    @Query("SELECT * FROM chase_participants ORDER BY createdAt") fun observeAll(): Flow<List<ChaseParticipantEntity>>
    @Query("SELECT * FROM chase_participants") suspend fun getAll(): List<ChaseParticipantEntity>
    @Insert suspend fun insert(c: ChaseParticipantEntity): Long
    @Update suspend fun update(c: ChaseParticipantEntity)
    @Delete suspend fun delete(c: ChaseParticipantEntity)
    @Query("DELETE FROM chase_participants") suspend fun deleteAll()
}
