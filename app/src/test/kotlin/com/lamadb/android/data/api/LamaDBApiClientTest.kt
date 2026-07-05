package com.lamadb.android.data.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LamaDBApiClientTest {

    @Test
    fun getMe_sendsAuthHeaderAndParsesResponse() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
            respond(
                content = """{"id":"user-1","name":"Ali","email":"a@leksander.no"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = LamaDBApiClient("https://lamadb.test", "test-key", mockEngine)
        val result = client.getMe()

        assertTrue(result.isSuccess)
        assertEquals("user-1", result.getOrNull()?.id)
        assertEquals("Ali", result.getOrNull()?.name)
    }

    @Test
    fun getMe_returnsFailure_on401() = runTest {
        val mockEngine = MockEngine {
            respond("", HttpStatusCode.Unauthorized)
        }

        val client = LamaDBApiClient("https://lamadb.test", "bad-key", mockEngine)
        val result = client.getMe()

        assertTrue(result.isFailure)
    }

    @Test
    fun provision_parsesResponse() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/android/provision", request.url.encodedPath)
            respond(
                content = """{"api_key":"new-key","user_id":"user-1","expires_at":"2026-07-06T12:00:00Z"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = LamaDBApiClient("https://lamadb.test", engine = mockEngine)
        val result = client.provision(ProvisionRequest("qr-token"))

        assertTrue(result.isSuccess)
        assertEquals("new-key", result.getOrNull()?.apiKey)
        assertEquals("user-1", result.getOrNull()?.userId)
    }
}
