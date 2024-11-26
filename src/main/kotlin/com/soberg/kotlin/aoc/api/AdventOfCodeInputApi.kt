package com.soberg.kotlin.aoc.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

object AdventOfCodeInputApi {

    suspend fun readInputFromNetwork(
        cachingStrategy: CachingStrategy,
        year: Int,
        day: Int,
        sessionToken: String,
    ): Result<List<String>> {
        // If cache exists, read from it and return immediately.
        readFromCache(cachingStrategy, year, day)?.let { cachedLines ->
            return Result.success(cachedLines)
        }

        return runCatching {
            tryReadInputFromNetwork(year, day, sessionToken)
        }.onSuccess { lines ->
            // Side effect, store in cache.
            writeToCache(cachingStrategy, year, day, lines)
        }
    }

    private fun readFromCache(
        cachingStrategy: CachingStrategy,
        year: Int,
        day: Int,
    ): List<String>? = when (cachingStrategy) {
        CachingStrategy.None -> null
        is CachingStrategy.LocalTextFile -> {
            val path = Path(cachingStrategy.cacheDirPath, "$year", "$day.txt")
            if (path.exists()) {
                path.readLines()
            } else {
                null
            }
        }
    }

    private suspend fun tryReadInputFromNetwork(
        year: Int,
        day: Int,
        sessionToken: String,
    ): List<String> {
        val response: HttpResponse = AdventOfCodeKtorClient.create().use { client ->
            readInputFromNetwork(client, year, day, sessionToken)
        }
        if (response.status.value in 200..299) {
            return response.bodyAsText().lines()
        } else {
            error("Unexpected response code ${response.status.value}")
        }
    }

    private suspend fun readInputFromNetwork(
        client: HttpClient,
        year: Int,
        day: Int,
        sessionToken: String,
    ) = client.get("https://adventofcode.com/$year/day/$day/input") {
        header("Cookie", "session=$sessionToken")
    }

    private fun writeToCache(
        cachingStrategy: CachingStrategy,
        year: Int,
        day: Int,
        lines: List<String>,
    ) {
        when (cachingStrategy) {
            is CachingStrategy.LocalTextFile -> {
                val path = Path(cachingStrategy.cacheDirPath, "$year", "$day.txt")
                if (!path.exists()) {
                    Files.createDirectories(path.parent)
                    path.createFile()
                }
                path.writeLines(lines)
            }

            CachingStrategy.None -> {
                /* Do nothing. */
            }
        }
    }

    sealed interface CachingStrategy {
        /** Don't try to cache this to a local file, instead just always read from network. */
        data object None : CachingStrategy

        /** Cache to a local text file from network on first read, then return from the local text file. */
        data class LocalTextFile(
            val cacheDirPath: String,
        ) : CachingStrategy
    }
}