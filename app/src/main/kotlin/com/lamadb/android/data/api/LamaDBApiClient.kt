package com.lamadb.android.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class LamaDBApiClient(
    serverUrl: String,
    apiKey: String? = null,
    engine: HttpClientEngine = Android.create()
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(engine) {
        defaultRequest {
            url(serverUrl.trimEnd('/'))
            header(HttpHeaders.ContentType, "application/json")
            apiKey?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        install(ContentNegotiation) { json(json) }
        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : Logger {
                // Never log bodies or auth values.
                override fun log(message: String) = Unit
            }
        }
    }

    suspend fun getMe(): Result<MeResponse> = apiCall {
        client.get("/api/me").body()
    }

    suspend fun provision(request: ProvisionRequest): Result<ProvisionResponse> = apiCall {
        client.post("/api/android/provision") { setBody(request) }.body()
    }

    suspend fun postEvent(event: EventRequest): Result<Unit> = apiCall {
        client.post("/api/events") { setBody(event) }.body()
    }

    suspend fun getEvents(limit: Int = 3): Result<List<EventResponse>> = apiCall {
        client.get("/api/events") {
            url { parameters.append("limit", limit.toString()) }
        }.body()
    }

    suspend fun getWikiPages(limit: Int = 500): Result<List<WikiPageResponse>> = apiCall {
        client.get("/api/wiki/pages") {
            url { parameters.append("limit", limit.toString()) }
        }.body()
    }

    suspend fun getWikiPage(path: String): Result<WikiPageDetailResponse> = apiCall {
        client.get("/api/wiki/page/${path.trimStart('/')}") {
            url { parameters.append("source", "db") }
        }.body()
    }

    suspend fun getDashboardHeader(): Result<DashboardHeaderResponse> = apiCall {
        client.get("/api/dashboard/header").body()
    }

    private suspend inline fun <T> apiCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: ClientRequestException) {
        Result.failure(e)
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
