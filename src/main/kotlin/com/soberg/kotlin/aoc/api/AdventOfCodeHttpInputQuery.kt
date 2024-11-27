package com.soberg.kotlin.aoc.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Duration.Companion.seconds

internal object AdventOfCodeHttpInputQuery {

    private val Timeout = 20.seconds

    suspend fun runQuery(
        year: Int,
        day: Int,
        sessionToken: String,
    ) = createClient().use { client ->
        client.get("https://adventofcode.com/$year/day/$day/input") {
            header("Cookie", "session=$sessionToken")
        }
    }

    fun createClient(
        engine: HttpClientEngineFactory<HttpClientEngineConfig> = OkHttp,
    ) = HttpClient(engine) {
        install(HttpTimeout) {
            socketTimeoutMillis = Timeout.inWholeMilliseconds
            requestTimeoutMillis = Timeout.inWholeMilliseconds
            connectTimeoutMillis = Timeout.inWholeMilliseconds
        }
        defaultRequest {
            contentType(ContentType.Text.Plain)
        }
    }
}