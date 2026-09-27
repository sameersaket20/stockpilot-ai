package com.stockpilot.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProductEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class StockPilotDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        fun build(context: Context): StockPilotDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                StockPilotDatabase::class.java,
                "stockpilot.db",
            ).fallbackToDestructiveMigration()
                .build()
    }
}