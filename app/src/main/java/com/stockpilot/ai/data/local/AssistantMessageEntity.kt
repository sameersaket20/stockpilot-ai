package com.stockpilot.ai.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assistant_messages",
    indices = [Index("createdAtMs")],
)
data class AssistantMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val productLinksJson: String = "[]",
    val referencedProductsJson: String = "[]",
    val provenanceJson: String? = null,
    val createdAtMs: Long,
    val responseDurationMs: Long? = null,
)