package com.cochelper.app.data.local

import com.cochelper.app.data.sync.BackupPayload
import com.cochelper.app.platform.currentTimeMillis
import com.cochelper.app.platform.nextId
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * 全内存数据源：整份快照存于一个 StateFlow，每次变更同步写回 localStorage。
 * GitHub 同步作为主备份（见 LoginScreen 的 push/pull）。
 *
 * 由于各 DAO 接口存在同名方法（getAll/deleteAll/observeAll 等）但返回类型不同，
 * 无法用一个类同时实现全部接口，故拆成多个内部类，共享同一个 state。
 */
class InMemoryRepository private constructor(initial: BackupPayload) : BackupStore {

    private val state = MutableStateFlow(initial)
    private val json = Json { ignoreUnknownKeys = true }

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val changes: SharedFlow<Unit> = _changes

    /** 用云端快照整体覆盖本地（不发出 [changes]，避免同步写回时触发自动同步死循环）。 */
    override suspend fun replaceAll(payload: BackupPayload) {
        state.value = payload
        persist()
    }

    private fun persist() {
        val encoded = json.encodeToString(BackupPayload.serializer(), state.value)
        try {
            window.localStorage.setItem(STORAGE_KEY, encoded)
        } catch (e: Throwable) {
            // localStorage 配额满 / 隐私模式等导致的写入失败：只记录，不打断内存态，
            // 避免一次保存失败让整个操作（如换照片）抛异常崩溃。数据仍保留在本会话内。
            println("[persist] localStorage 写入失败（本次变更仅在当前会话内生效）：${e.message}")
        }
    }

    private fun mutate(block: (BackupPayload) -> BackupPayload) {
        state.update(block)
        persist()
        _changes.tryEmit(Unit)
    }

    private fun <T> observe(selector: (BackupPayload) -> List<T>): Flow<List<T>> = state.map(selector)

    val moduleDao: ModuleDao = ModuleDaoImpl()
    val timelineDao: TimelineDao = TimelineDaoImpl()
    val locationDao: LocationDao = LocationDaoImpl()
    val npcDao: NpcDao = NpcDaoImpl()
    val pcDao: PcDao = PcDaoImpl()
    val clueDao: ClueDao = ClueDaoImpl()
    val fileDao: FileDao = FileDaoImpl()
    val combatantDao: CombatantDao = CombatantDaoImpl()
    val chaseDao: ChaseParticipantDao = ChaseDaoImpl()
    val chasePointDao: ChasePointDao = ChasePointDaoImpl()

    private inner class ModuleDaoImpl : ModuleDao {
        override fun observeAll(): Flow<List<ModuleEntity>> =
            observe { it.modules.sortedByDescending { m -> m.createdAt } }
        override fun observeById(id: Long): Flow<ModuleEntity?> =
            state.map { it.modules.firstOrNull { m -> m.id == id } }
        override suspend fun getById(id: Long): ModuleEntity? = state.value.modules.firstOrNull { it.id == id }
        override suspend fun getActive(): ModuleEntity? = state.value.modules.firstOrNull { it.isActive }
        override suspend fun getAll(): List<ModuleEntity> = state.value.modules
        override suspend fun insert(m: ModuleEntity): Long {
            val id = if (m.id == 0L) nextId() else m.id
            mutate { it.copy(modules = it.modules + m.copy(id = id, updatedAt = if (m.updatedAt == 0L) currentTimeMillis() else m.updatedAt)) }
            return id
        }
        override suspend fun update(m: ModuleEntity) {
            mutate { it.copy(modules = it.modules.map { x -> if (x.id == m.id) m.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(m: ModuleEntity) {
            mutate { it.copy(modules = it.modules.filterNot { x -> x.id == m.id }) }
        }
        override suspend fun clearActive() {
            mutate { it.copy(modules = it.modules.map { x -> x.copy(isActive = false) }) }
        }
        override suspend fun setActive(id: Long) {
            mutate { it.copy(modules = it.modules.map { x -> x.copy(isActive = x.id == id) }) }
        }
        override suspend fun deleteAll() { mutate { it.copy(modules = emptyList()) } }
    }

    private inner class TimelineDaoImpl : TimelineDao {
        override fun observeForModule(moduleId: Long): Flow<List<TimelineNodeEntity>> =
            observe {
                it.timelineNodes.filter { n -> n.moduleId == moduleId }
                    .sortedWith(compareBy({ n -> n.parentId ?: Long.MIN_VALUE }, { n -> n.order }, { n -> n.createdAt }))
            }
        override suspend fun getAll(moduleId: Long): List<TimelineNodeEntity> =
            state.value.timelineNodes.filter { it.moduleId == moduleId }
        override suspend fun getAllForExport(): List<TimelineNodeEntity> = state.value.timelineNodes
        override suspend fun insert(n: TimelineNodeEntity): Long {
            val id = if (n.id == 0L) nextId() else n.id
            mutate { it.copy(timelineNodes = it.timelineNodes + n.copy(id = id, updatedAt = if (n.updatedAt == 0L) currentTimeMillis() else n.updatedAt)) }
            return id
        }
        override suspend fun update(n: TimelineNodeEntity) {
            mutate { it.copy(timelineNodes = it.timelineNodes.map { x -> if (x.id == n.id) n.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(n: TimelineNodeEntity) {
            mutate { it.copy(timelineNodes = it.timelineNodes.filterNot { x -> x.id == n.id }) }
        }
        override suspend fun deleteForModule(moduleId: Long) {
            mutate { it.copy(timelineNodes = it.timelineNodes.filterNot { x -> x.moduleId == moduleId }) }
        }
        override suspend fun deleteAllForAll() { mutate { it.copy(timelineNodes = emptyList()) } }
    }

    private inner class LocationDaoImpl : LocationDao {
        override fun observeForModule(moduleId: Long): Flow<List<LocationEntity>> =
            observe { it.locations.filter { l -> l.moduleId == moduleId }.sortedByDescending { l -> l.createdAt } }
        override fun observeById(id: Long): Flow<LocationEntity?> =
            state.map { it.locations.firstOrNull { l -> l.id == id } }
        override suspend fun getById(id: Long): LocationEntity? = state.value.locations.firstOrNull { it.id == id }
        override suspend fun insert(l: LocationEntity): Long {
            val id = if (l.id == 0L) nextId() else l.id
            mutate { it.copy(locations = it.locations + l.copy(id = id, updatedAt = if (l.updatedAt == 0L) currentTimeMillis() else l.updatedAt)) }
            return id
        }
        override suspend fun update(l: LocationEntity) {
            mutate { it.copy(locations = it.locations.map { x -> if (x.id == l.id) l.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(l: LocationEntity) {
            mutate { it.copy(locations = it.locations.filterNot { x -> x.id == l.id }) }
        }
        override suspend fun deleteForModule(moduleId: Long) {
            mutate { it.copy(locations = it.locations.filterNot { x -> x.moduleId == moduleId }) }
        }
        override suspend fun getAll(): List<LocationEntity> = state.value.locations
        override suspend fun deleteAllForAll() { mutate { it.copy(locations = emptyList()) } }
    }

    private inner class NpcDaoImpl : NpcDao {
        override fun observeForModule(moduleId: Long): Flow<List<NpcEntity>> =
            observe { it.npcs.filter { n -> n.moduleId == moduleId }.sortedByDescending { n -> n.createdAt } }
        override fun observeById(id: Long): Flow<NpcEntity?> =
            state.map { it.npcs.firstOrNull { n -> n.id == id } }
        override suspend fun getById(id: Long): NpcEntity? = state.value.npcs.firstOrNull { it.id == id }
        override suspend fun insert(n: NpcEntity): Long {
            val id = if (n.id == 0L) nextId() else n.id
            mutate { it.copy(npcs = it.npcs + n.copy(id = id, updatedAt = if (n.updatedAt == 0L) currentTimeMillis() else n.updatedAt)) }
            return id
        }
        override suspend fun update(n: NpcEntity) {
            mutate { it.copy(npcs = it.npcs.map { x -> if (x.id == n.id) n.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(n: NpcEntity) {
            mutate { it.copy(npcs = it.npcs.filterNot { x -> x.id == n.id }) }
        }
        override suspend fun deleteForModule(moduleId: Long) {
            mutate { it.copy(npcs = it.npcs.filterNot { x -> x.moduleId == moduleId }) }
        }
        override suspend fun getAll(): List<NpcEntity> = state.value.npcs
        override suspend fun deleteAllForAll() { mutate { it.copy(npcs = emptyList()) } }
    }

    private inner class PcDaoImpl : PcDao {
        override fun observeForModule(moduleId: Long): Flow<List<PcEntity>> =
            observe { it.pcs.filter { p -> p.moduleId == moduleId }.sortedByDescending { p -> p.createdAt } }
        override fun observeAll(): Flow<List<PcEntity>> =
            observe { it.pcs.sortedByDescending { p -> p.createdAt } }
        override fun observePlayerPcs(): Flow<List<PcEntity>> =
            observe { it.pcs.filter { p -> p.moduleId == null }.sortedByDescending { p -> p.createdAt } }
        override fun observeById(id: Long): Flow<PcEntity?> =
            state.map { it.pcs.firstOrNull { p -> p.id == id } }
        override suspend fun getById(id: Long): PcEntity? = state.value.pcs.firstOrNull { it.id == id }
        override suspend fun insert(p: PcEntity): Long {
            val id = if (p.id == 0L) nextId() else p.id
            mutate { it.copy(pcs = it.pcs + p.copy(id = id, updatedAt = if (p.updatedAt == 0L) currentTimeMillis() else p.updatedAt)) }
            return id
        }
        override suspend fun update(p: PcEntity) {
            mutate { it.copy(pcs = it.pcs.map { x -> if (x.id == p.id) p.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(p: PcEntity) {
            mutate { it.copy(pcs = it.pcs.filterNot { x -> x.id == p.id }) }
        }
        override suspend fun deleteForModule(moduleId: Long) {
            mutate { it.copy(pcs = it.pcs.filterNot { x -> x.moduleId == moduleId }) }
        }
        override suspend fun getAll(): List<PcEntity> = state.value.pcs
        override suspend fun deleteAllForAll() { mutate { it.copy(pcs = emptyList()) } }
    }

    private inner class ClueDaoImpl : ClueDao {
        override fun observeAll(): Flow<List<ClueEntity>> =
            observe { it.clues.sortedBy { c -> c.order } }
        override suspend fun getAll(): List<ClueEntity> = state.value.clues
        override suspend fun insert(c: ClueEntity): Long {
            val id = if (c.id == 0L) nextId() else c.id
            mutate { it.copy(clues = it.clues + c.copy(id = id, updatedAt = if (c.updatedAt == 0L) currentTimeMillis() else c.updatedAt)) }
            return id
        }
        override suspend fun update(c: ClueEntity) {
            mutate { it.copy(clues = it.clues.map { x -> if (x.id == c.id) c.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(c: ClueEntity) {
            mutate { it.copy(clues = it.clues.filterNot { x -> x.id == c.id }) }
        }
        override suspend fun deleteAll() { mutate { it.copy(clues = emptyList()) } }
    }

    private inner class FileDaoImpl : FileDao {
        override fun observeForModule(moduleId: Long): Flow<List<FileEntity>> =
            observe { it.files.filter { f -> f.moduleId == moduleId }.sortedByDescending { f -> f.createdAt } }
        override fun observeForModuleKind(moduleId: Long, kind: Int): Flow<List<FileEntity>> =
            observe {
                it.files.filter { f -> f.moduleId == moduleId && f.kind == kind }
                    .sortedByDescending { f -> f.createdAt }
            }
        override suspend fun getAll(): List<FileEntity> = state.value.files
        override suspend fun insert(f: FileEntity): Long {
            val id = if (f.id == 0L) nextId() else f.id
            mutate { it.copy(files = it.files + f.copy(id = id, updatedAt = if (f.updatedAt == 0L) currentTimeMillis() else f.updatedAt)) }
            return id
        }
        override suspend fun delete(f: FileEntity) {
            mutate { it.copy(files = it.files.filterNot { x -> x.id == f.id }) }
        }
        override suspend fun deleteForModule(moduleId: Long) {
            mutate { it.copy(files = it.files.filterNot { x -> x.moduleId == moduleId }) }
        }
        override suspend fun deleteAllForAll() { mutate { it.copy(files = emptyList()) } }
    }

    private inner class CombatantDaoImpl : CombatantDao {
        override fun observeAll(): Flow<List<CombatantEntity>> =
            observe {
                // 行动先后：携带远程武器者优先；再按 DEX 降序（敏捷高者先行动）；同敏捷按加入先后稳定排序
                it.combatants.sortedWith(
                    compareByDescending<CombatantEntity> { c -> c.ranged }
                        .thenByDescending { c -> c.dex }
                        .thenBy { c -> c.createdAt }
                )
            }
        override suspend fun getAll(): List<CombatantEntity> = state.value.combatants
        override suspend fun insert(c: CombatantEntity): Long {
            val id = if (c.id == 0L) nextId() else c.id
            mutate { it.copy(combatants = it.combatants + c.copy(id = id, updatedAt = if (c.updatedAt == 0L) currentTimeMillis() else c.updatedAt)) }
            return id
        }
        override suspend fun update(c: CombatantEntity) {
            mutate { it.copy(combatants = it.combatants.map { x -> if (x.id == c.id) c.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(c: CombatantEntity) {
            mutate { it.copy(combatants = it.combatants.filterNot { x -> x.id == c.id }) }
        }
        override suspend fun deleteAll() { mutate { it.copy(combatants = emptyList()) } }
    }

    private inner class ChaseDaoImpl : ChaseParticipantDao {
        override fun observeAll(): Flow<List<ChaseParticipantEntity>> =
            observe { it.chaseParticipants.sortedBy { c -> c.createdAt } }
        override suspend fun getAll(): List<ChaseParticipantEntity> = state.value.chaseParticipants
        override suspend fun insert(c: ChaseParticipantEntity): Long {
            val id = if (c.id == 0L) nextId() else c.id
            mutate { it.copy(chaseParticipants = it.chaseParticipants + c.copy(id = id, updatedAt = if (c.updatedAt == 0L) currentTimeMillis() else c.updatedAt)) }
            return id
        }
        override suspend fun update(c: ChaseParticipantEntity) {
            mutate { it.copy(chaseParticipants = it.chaseParticipants.map { x -> if (x.id == c.id) c.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(c: ChaseParticipantEntity) {
            mutate { it.copy(chaseParticipants = it.chaseParticipants.filterNot { x -> x.id == c.id }) }
        }
        override suspend fun deleteAll() { mutate { it.copy(chaseParticipants = emptyList()) } }
    }

    private inner class ChasePointDaoImpl : ChasePointDao {
        override fun observeAll(): Flow<List<ChasePointEntity>> =
            observe { it.chasePoints.sortedWith(compareBy({ p -> p.order }, { p -> p.createdAt })) }
        override suspend fun getAll(): List<ChasePointEntity> = state.value.chasePoints
        override suspend fun insert(p: ChasePointEntity): Long {
            val id = if (p.id == 0L) nextId() else p.id
            mutate { it.copy(chasePoints = it.chasePoints + p.copy(id = id, updatedAt = if (p.updatedAt == 0L) currentTimeMillis() else p.updatedAt)) }
            return id
        }
        override suspend fun update(p: ChasePointEntity) {
            mutate { it.copy(chasePoints = it.chasePoints.map { x -> if (x.id == p.id) p.copy(updatedAt = currentTimeMillis()) else x }) }
        }
        override suspend fun delete(p: ChasePointEntity) {
            mutate { it.copy(chasePoints = it.chasePoints.filterNot { x -> x.id == p.id }) }
        }
        override suspend fun deleteAll() { mutate { it.copy(chasePoints = emptyList()) } }
    }

    companion object {
        private const val STORAGE_KEY = "cochelper_data_v1"

        fun load(): InMemoryRepository {
            val initial = runCatching {
                val raw = window.localStorage.getItem(STORAGE_KEY)
                if (raw.isNullOrBlank()) {
                    BackupPayload()
                } else {
                    Json { ignoreUnknownKeys = true }.decodeFromString(BackupPayload.serializer(), raw)
                }
            }.getOrElse { BackupPayload() }
            return InMemoryRepository(initial)
        }
    }
}
