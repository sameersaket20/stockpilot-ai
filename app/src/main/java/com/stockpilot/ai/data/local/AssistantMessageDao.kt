package com.stockpilot.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistantMessageDao {
    @Query("SELECT * FROM assistant_messages ORDER BY createdAtMs ASC, id ASC")
    fun observeAll(): Flow<List<AssistantMessageEntity>>

    @Query(
        """
        SELECT * FROM (
            SELECT * FROM assistant_messages
            ORDER BY createdAtMs DESC, id DESC
            LIMIT :limit
        ) ORDER BY createdAtMs ASC, id ASC
        """,
    )
    fun observeLastN(limit: Int): Flow<List<AssistantMessageEntity>>

    @Query("SELECT COUNT(*) FROM assistant_messages")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM assistant_messages")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: AssistantMessageEntity): Long
}