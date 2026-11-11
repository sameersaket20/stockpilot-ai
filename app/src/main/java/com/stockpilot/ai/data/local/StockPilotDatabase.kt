package com.stockpilot.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductEntity::class, AssistantMessageEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class StockPilotDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun assistantMessageDao(): AssistantMessageDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE assistant_messages ADD COLUMN responseDurationMs INTEGER",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE assistant_messages ADD COLUMN referencedProductsJson TEXT NOT NULL DEFAULT '[]'",
                )
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assistant_messages (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        role TEXT NOT NULL,
                        text TEXT NOT NULL,
                        productLinksJson TEXT NOT NULL,
                        provenanceJson TEXT,
                        createdAtMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_assistant_messages_createdAtMs " +
                        "ON assistant_messages(createdAtMs)",
                )
            }
        }

        fun build(context: Context): StockPilotDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                StockPilotDatabase::class.java,
                "stockpilot.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
    }
}