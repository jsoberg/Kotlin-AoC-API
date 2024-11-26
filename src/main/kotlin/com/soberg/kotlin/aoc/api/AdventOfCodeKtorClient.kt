package com.soberg.kotlin.aoc.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.time.Duration.Companion.seconds

internal object AdventOfCodeKtorClient {
    private val Timeout = 20.seconds

    fun create(
        engineFactory: HttpClientEngineFactory<HttpClientEngineConfig> = OkHttp,
    ) = HttpClient(engineFactory) { applyConfig() }

    private fun HttpClientConfig<*>.applyConfig() {
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