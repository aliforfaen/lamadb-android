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
