package com.cochelper.app.di

import com.cochelper.app.data.FileStore
import com.cochelper.app.data.SettingsStore
import com.cochelper.app.data.local.BackupStore
import com.cochelper.app.data.local.ChaseParticipantDao
import com.cochelper.app.data.local.ChasePointDao
import com.cochelper.app.data.local.ClueDao
import com.cochelper.app.data.local.CombatantDao
import com.cochelper.app.data.local.FileDao
import com.cochelper.app.data.local.LocationDao
import com.cochelper.app.data.local.ModuleDao
import com.cochelper.app.data.local.NpcDao
import com.cochelper.app.data.local.PcDao
import com.cochelper.app.data.local.TimelineDao
import com.cochelper.app.data.sync.BackupPayload
import com.cochelper.app.data.sync.GitHubSync
import com.cochelper.app.data.sync.mergeBackups
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 简易手动依赖注入容器。 */
class AppContainer(
    val moduleDao: ModuleDao,
    val timelineDao: TimelineDao,
    val locationDao: LocationDao,
    val npcDao: NpcDao,
    val pcDao: PcDao,
    val clueDao: ClueDao,
    val fileDao: FileDao,
    val combatantDao: CombatantDao,
    val chaseDao: ChaseParticipantDao,
    val chasePointDao: ChasePointDao,
    val settings: SettingsStore,
    val github: GitHubSync,
    val files: FileStore,
    val backupStore: BackupStore,
) {

    private val syncMutex = Mutex()

    /** 导出本地全部数据为云端快照。 */
    suspend fun exportBackup(): BackupPayload = BackupPayload(
        exportedAt = com.cochelper.app.platform.currentTimeMillis(),
        modules = moduleDao.getAll(),
        timelineNodes = timelineDao.getAllForExport(),
        locations = locationDao.getAll(),
        npcs = npcDao.getAll(),
        pcs = pcDao.getAll(),
        clues = clueDao.getAll(),
        combatants = combatantDao.getAll(),
        chaseParticipants = chaseDao.getAll(),
        chasePoints = chasePointDao.getAll(),
        files = fileDao.getAll(),
    )

    /** 删除模组及其全部关联数据（时间轴/地点/npc/pc/文件）。 */
    suspend fun deleteModule(moduleId: Long) {
        timelineDao.deleteForModule(moduleId)
        locationDao.deleteForModule(moduleId)
        npcDao.deleteForModule(moduleId)
        pcDao.deleteForModule(moduleId)
        fileDao.deleteForModule(moduleId)
        moduleDao.getById(moduleId)?.let { moduleDao.delete(it) }
    }

    /** 与云端双向同步：先拉最新，再按 last-write-wins 合并，最后推回并落地本地。 */
    suspend fun syncWithCloud(token: String, repoName: String): Result<String> = runCatching {
        syncMutex.withLock {
            val full = github.ensureRepo(token, repoName).getOrThrow()
            val local = exportBackup()
            val remote = github.fetchBackup(token, full)
            val merged = if (remote == null) local else mergeBackups(local, remote)
            github.pushBackup(token, full, merged).getOrThrow()
            importBackup(merged)
            "已同步"
        }
    }

    /** 用云端快照整体覆盖本地数据（不触发自动同步）。 */
    suspend fun importBackup(payload: BackupPayload) {
        backupStore.replaceAll(payload)
    }

    /** 监听本地变更，防抖 2 秒后若已配置 token 则自动双向同步（与 life-manager 的「改动即同步」一致）。 */
    @OptIn(FlowPreview::class)
    suspend fun autoSyncLoop() {
        backupStore.changes
            .debounce(2000L)
            .collect {
                val s = settings.settings.first()
                val token = s.githubToken
                if (token.isBlank()) return@collect
                syncWithCloud(token, s.repoName.ifBlank { "coc-helper-backup" })
                    .onFailure { println("[autoSync] 自动同步失败：${it.message}") }
            }
    }
}
