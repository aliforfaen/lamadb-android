package com.lamadb.android.data.wiki

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached LamaDB wiki page.
 *
 * The index sync fetches summaries for all pages; viewing a page caches its
 * full markdown content locally so it can be read offline afterwards.
 */
@Entity(tableName = "wiki_pages")
data class WikiPageEntity(
    @PrimaryKey val path: String,
    val title: String,
    val section: String,
    val size: Int,
    val content: String = "",
    val syncedAt: Long
)
