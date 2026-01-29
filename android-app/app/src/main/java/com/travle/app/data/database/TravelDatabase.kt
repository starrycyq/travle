package com.travle.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [CollectionItem::class, UserPreference::class],
    version = 2, // 更新版本号以包含新实体
    exportSchema = false
)
abstract class TravelDatabase : RoomDatabase() {
    abstract fun collectionDao(): CollectionDao
    abstract fun userPreferenceDao(): UserPreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: TravelDatabase? = null

        fun getDatabase(context: Context): TravelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TravelDatabase::class.java,
                    "travel_database"
                )
                .fallbackToDestructiveMigration() // 简化开发过程中的数据库迁移
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}