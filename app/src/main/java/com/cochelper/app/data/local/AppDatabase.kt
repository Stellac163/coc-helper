package com.cochelper.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ModuleEntity::class,
        TimelineNodeEntity::class,
        LocationEntity::class,
        NpcEntity::class,
        PcEntity::class,
        ClueEntity::class,
        FileEntity::class,
        CombatantEntity::class,
        ChaseParticipantEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun timelineDao(): TimelineDao
    abstract fun locationDao(): LocationDao
    abstract fun npcDao(): NpcDao
    abstract fun pcDao(): PcDao
    abstract fun clueDao(): ClueDao
    abstract fun fileDao(): FileDao
    abstract fun combatantDao(): CombatantDao
    abstract fun chaseParticipantDao(): ChaseParticipantDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cochelper.db"
                ).build().also { INSTANCE = it }
            }
    }
}
