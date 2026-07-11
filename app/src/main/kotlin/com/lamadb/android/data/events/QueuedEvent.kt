package com.lamadb.android.data.events

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class QueuedEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val type: String,
    val severity: String,
    val title: String,
    val body: String?,
    val metadata: String,
    val createdAt: Long,
    val retryCount: Int = 0
)
