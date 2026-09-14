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
import com.cochelper.app.data.sync.isEmptyData
import com.cochelper.app.platform.HttpProgress
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

    /** 上传：用本地整体覆盖云端（单向，不做合并）。[onProgress] 可选，回传上传进度。 */
    suspend fun uploadToCloud(token: String, repoName: String, onProgress: ((HttpProgress) -> Unit)? = null): Result<String> = runCatching {
        syncMutex.withLock {
            val full = github.ensureRepo(token, repoName).getOrThrow()
            val local = exportBackup()
            // 本地无数据时不上传，避免把云端已有备份抹成空文件
            if (local.isEmptyData()) return@withLock "本地无数据，未上传（以免清空云端）"
            github.pushBackup(token, full, local, onProgress).getOrThrow()
            "已上传，云端已被本地覆盖"
        }
    }

    /** 恢复：用云端整体覆盖本地（单向，不做合并）。[onProgress] 可选，回传下载进度。 */
    suspend fun restoreFromCloud(token: String, repoName: String, onProgress: ((HttpProgress) -> Unit)? = null): Result<String> = runCatching {
        syncMutex.withLock {
            val full = github.ensureRepo(token, repoName).getOrThrow()
            val payload = github.pullBackup(token, full, onProgress).getOrThrow()
            importBackup(payload)
            "已从云端恢复，本地已被覆盖"
        }
    }

    /** 用云端快照整体覆盖本地数据。 */
    suspend fun importBackup(payload: BackupPayload) {
        backupStore.replaceAll(payload)
    }
}
