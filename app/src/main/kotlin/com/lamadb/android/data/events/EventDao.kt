package com.lamadb.android.data.events

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: QueuedEvent): Long

    @Query("SELECT * FROM events ORDER BY createdAt ASC LIMIT :limit")
    suspend fun oldest(limit: Int): List<QueuedEvent>

    @Query("SELECT COUNT(*) FROM events")
    suspend fun count(): Int

    @Query("DELETE FROM events WHERE createdAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int

    @Update
    suspend fun update(event: QueuedEvent)

    @Delete
    suspend fun delete(event: QueuedEvent)
}
