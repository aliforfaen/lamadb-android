package com.lamadb.android.data.wiki

import android.content.Context
import com.lamadb.android.data.api.LamaDBApiClient
import com.lamadb.android.data.auth.SecureTokenStore
import com.lamadb.android.data.events.EventDatabase

class WikiRepository(
    context: Context,
    private val dao: WikiDao = EventDatabase.getInstance(context).wikiDao(),
    private val tokenStore: SecureTokenStore = SecureTokenStore(context),
    private val apiClientFactory: (serverUrl: String, apiKey: String) -> LamaDBApiClient = { url, key ->
        LamaDBApiClient(url, key)
    }
) {

    /**
     * Fetch wiki page summaries from the backend and replace the local cache.
     * Existing page content is preserved during the index refresh.
     */
    suspend fun sync(): Result<Int> {
        val credentials = tokenStore.load().getOrNull()
            ?: return Result.failure(NotAuthenticatedException())
        val client = apiClientFactory(credentials.serverUrl, credentials.apiKey)

        return client.getWikiPages().map { pages ->
            val now = System.currentTimeMillis()
            val existingContent = dao.getAll().associate { it.path to it.content }
            val entities = pages.map { page ->
                WikiPageEntity(
                    path = page.path,
                    title = page.title,
                    section = page.section,
                    size = page.size,
                    content = existingContent[page.path] ?: "",
                    syncedAt = now
                )
            }
            dao.replaceAll(entities)
            entities.size
        }
    }

    /**
     * Fetch the full content for a single wiki page and cache it locally.
     */
    suspend fun fetchPageContent(path: String): Result<WikiPageEntity> {
        val credentials = tokenStore.load().getOrNull()
            ?: return Result.failure(NotAuthenticatedException())
        val client = apiClientFactory(credentials.serverUrl, credentials.apiKey)

        return client.getWikiPage(path).map { page ->
            val entity = WikiPageEntity(
                path = page.path,
                title = page.title,
                section = page.section,
                size = page.size,
                content = page.content,
                syncedAt = System.currentTimeMillis()
            )
            dao.insertPage(entity)
            entity
        }
    }

    suspend fun getAll(): List<WikiPageEntity> = dao.getAll()

    suspend fun getPage(path: String): WikiPageEntity? = dao.getPage(path)

    suspend fun count(): Int = dao.count()

    class NotAuthenticatedException : Exception("Not authenticated")

    companion object {
        fun createDefault(context: Context): WikiRepository = WikiRepository(context)
    }
}
