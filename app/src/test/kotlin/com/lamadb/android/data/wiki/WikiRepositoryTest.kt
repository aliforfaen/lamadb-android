package com.lamadb.android.data.wiki

import android.content.Context
import androidx.room.Room
import com.lamadb.android.data.api.LamaDBApiClient
import com.lamadb.android.data.api.WikiPageResponse
import com.lamadb.android.data.auth.SecureTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WikiRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: com.lamadb.android.data.events.EventDatabase
    private lateinit var repository: WikiRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, com.lamadb.android.data.events.EventDatabase::class.java)
            .build()

        val tokenStore = mockk<SecureTokenStore>(relaxed = true)
        every { tokenStore.load() } returns Result.success(
            SecureTokenStore.StoredCredentials("key", "https://lamadb.test", "user-1")
        )

        repository = WikiRepository(
            context = context,
            dao = database.wikiDao(),
            tokenStore = tokenStore
        ) { _, _ ->
            LamaDBApiClient("https://lamadb.test", "key", mockEngine())
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sync_replacesLocalCache() = runTest {
        val count = repository.sync().getOrThrow()

        assertEquals(2, count)
        val pages = repository.getAll()
        assertEquals(2, pages.size)
        assertEquals("Homelab", pages.first { it.path == "entities/homelab.md" }.title)
    }

    @Test
    fun sync_preservesExistingContent() = runTest {
        database.wikiDao().insertPage(
            WikiPageEntity(
                path = "entities/homelab.md",
                title = "Old title",
                section = "entities",
                size = 0,
                content = "cached body",
                syncedAt = 0
            )
        )

        repository.sync().getOrThrow()
        val page = repository.getPage("entities/homelab.md")!!

        assertEquals("Homelab", page.title)
        assertEquals("cached body", page.content)
    }

    @Test
    fun fetchPageContent_cachesFullPage() = runTest {
        val page = repository.fetchPageContent("entities/homelab.md").getOrThrow()

        assertEquals("Homelab", page.title)
        assertTrue(page.content.contains("# Homelab"))
        assertEquals("# Homelab\nNotes.", repository.getPage("entities/homelab.md")?.content)
    }

    private fun mockEngine() = MockEngine { request ->
        when {
            request.url.encodedPath == "/api/wiki/pages" -> {
                respond(
                    content = """[
                        {"path":"entities/homelab.md","title":"Homelab","section":"entities","size":1200},
                        {"path":"concepts/life-os.md","title":"Life OS","section":"concepts","size":800}
                    ]""".trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            request.url.encodedPath == "/api/wiki/page/entities/homelab.md" -> {
                respond(
                    content = """{"id":"p1","title":"Homelab","path":"entities/homelab.md","section":"entities","content":"# Homelab\nNotes.","size":1200}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            else -> respond("", HttpStatusCode.NotFound)
        }
    }
}
