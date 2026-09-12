package com.cochelper.app.di

import android.content.Context
import androidx.room.withTransaction
import com.cochelper.app.data.SettingsRepository
import com.cochelper.app.data.local.AppDatabase
import com.cochelper.app.data.sync.BackupPayload
import com.cochelper.app.data.sync.GitHubSync

/** 简易手动依赖注入容器。 */
class AppContainer(context: Context) {

    val database: AppDatabase = AppDatabase.get(context)
    val settings: SettingsRepository = SettingsRepository(context)
    val github: GitHubSync = GitHubSync()

    val moduleDao get() = database.moduleDao()
    val timelineDao get() = database.timelineDao()
    val locationDao get() = database.locationDao()
    val npcDao get() = database.npcDao()
    val pcDao get() = database.pcDao()
    val clueDao get() = database.clueDao()
    val fileDao get() = database.fileDao()
    val combatantDao get() = database.combatantDao()
    val chaseDao get() = database.chaseParticipantDao()

    /** 导出本地全部数据为云端快照。 */
    suspend fun exportBackup(): BackupPayload = BackupPayload(
        modules = moduleDao.getAll(),
        timelineNodes = timelineDao.getAllForExport(),
        locations = locationDao.getAll(),
        npcs = npcDao.getAll(),
        pcs = pcDao.getAll(),
        clues = clueDao.getAll(),
        combatants = combatantDao.getAll(),
        chaseParticipants = chaseDao.getAll(),
    )

    /** 用云端快照覆盖本地数据库。 */
    suspend fun importBackup(payload: BackupPayload) {
        database.withTransaction {
            clueDao.deleteAll()
            combatantDao.deleteAll()
            chaseDao.deleteAll()
            moduleDao.deleteAll()
            timelineDao.deleteAllForAll()
            locationDao.deleteAllForAll()
            npcDao.deleteAllForAll()
            pcDao.deleteAllForAll()
            fileDao.deleteAllForAll()

            payload.modules.forEach { moduleDao.insert(it) }
            payload.timelineNodes.forEach { timelineDao.insert(it) }
            payload.locations.forEach { locationDao.insert(it) }
            payload.npcs.forEach { npcDao.insert(it) }
            payload.pcs.forEach { pcDao.insert(it) }
            payload.clues.forEach { clueDao.insert(it) }
            payload.combatants.forEach { combatantDao.insert(it) }
            payload.chaseParticipants.forEach { chaseDao.insert(it) }
        }
    }
}
