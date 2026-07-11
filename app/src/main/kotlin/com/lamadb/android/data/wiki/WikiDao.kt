package com.lamadb.android.data.wiki

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WikiDao {

    @Query("SELECT * FROM wiki_pages ORDER BY section ASC, title ASC")
    suspend fun getAll(): List<WikiPageEntity>

    @Query("SELECT * FROM wiki_pages WHERE path = :path LIMIT 1")
    suspend fun getPage(path: String): WikiPageEntity?

    @Query("SELECT COUNT(*) FROM wiki_pages")
    suspend fun count(): Int

    @Query("DELETE FROM wiki_pages")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<WikiPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: WikiPageEntity)

    @Transaction
    suspend fun replaceAll(pages: List<WikiPageEntity>) {
        deleteAll()
        insertAll(pages)
    }
}
