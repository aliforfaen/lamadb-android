package com.lamadb.android.data.events

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lamadb.android.data.wiki.WikiDao
import com.lamadb.android.data.wiki.WikiPageEntity

@Database(
    entities = [QueuedEvent::class, WikiPageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun wikiDao(): WikiDao

    companion object {
        private const val DATABASE_NAME = "lamadb_events"

        @Volatile
        private var instance: EventDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS wiki_pages (
                        path TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        section TEXT NOT NULL,
                        size INTEGER NOT NULL,
                        content TEXT NOT NULL DEFAULT '',
                        syncedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): EventDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
