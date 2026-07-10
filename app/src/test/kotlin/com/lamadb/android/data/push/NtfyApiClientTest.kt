package com.lamadb.android.data.push

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NtfyApiClientTest {

    @Test
    fun poll_parsesNdjsonMessages() = runTest {
        val ndjson = """
            {"id":"m1","time":1700000001,"topic":"alerts","title":"Disk full","message":"/ is 95% full","priority":5,"tags":["warning"]}
            {"id":"m2","time":1700000002,"topic":"alerts","message":"Backup completed","priority":3}
        """.trimIndent()

        val mockEngine = MockEngine { request ->
            assertTrue(request.url.encodedPath.endsWith("/alerts/json"))
            respond(
                content = ndjson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = NtfyApiClient("https://ntfy.test", "alerts", mockEngine)
        val messages = client.poll("15m")

        assertEquals(2, messages.size)
        assertEquals("Disk full", messages[0].title)
        assertEquals(5, messages[0].priority)
        assertEquals("Backup completed", messages[1].message)
        assertEquals(3, messages[1].priority)
    }

    @Test
    fun poll_returnsEmptyList_onError() = runTest {
        val mockEngine = MockEngine {
            respond("", HttpStatusCode.ServiceUnavailable)
        }

        val client = NtfyApiClient("https://ntfy.test", "alerts", mockEngine)
        val messages = client.poll()

        assertTrue(messages.isEmpty())
    }

    @Test
    fun poll_returnsEmptyList_forBlankBody() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "   ",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = NtfyApiClient("https://ntfy.test", "alerts", mockEngine)
        val messages = client.poll()

        assertTrue(messages.isEmpty())
    }
}
