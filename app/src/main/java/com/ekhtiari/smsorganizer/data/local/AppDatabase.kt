package com.ekhtiari.smsorganizer.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageMetaEntity::class, ThreadMetaEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageMetaDao(): MessageMetaDao
    abstract fun threadMetaDao(): ThreadMetaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sms_organizer.db",
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
