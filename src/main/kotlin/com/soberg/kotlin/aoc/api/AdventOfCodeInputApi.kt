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

    /** @return A [Result] indicating success or failure*/
    suspend fun readInput(
        cachingStrategy: CachingStrategy,
        year: Int,
        day: Int,
        sessionToken: String,
    ): Result<List<String>> = runCatching {
        // If cache exists, read from it and return immediately.
        cachingStrategy.tryRead(year, day)?.let { cachedLines ->
            return Result.success(cachedLines)
        }

        tryReadInputFromNetwork(year, day, sessionToken)
    }.onSuccess { lines ->
        // Side effect, store in cache.
        cachingStrategy.write(year, day, lines)
    }

    private suspend fun tryReadInputFromNetwork(
        year: Int,
        day: Int,
        sessionToken: String,
    ): List<String> {
        val response: HttpResponse = AdventOfCodeKtorClient.create().use { client ->
            readInput(client, year, day, sessionToken)
        }
        if (response.status.value in 200..299) {
            return response.bodyAsText().lines()
        } else {
            error("Unexpected response code ${response.status.value}")
        }
    }

    private suspend fun readInput(
        client: HttpClient,
        year: Int,
        day: Int,
        sessionToken: String,
    ) = client.get("https://adventofcode.com/$year/day/$day/input") {
        header("Cookie", "session=$sessionToken")
    }

    sealed interface CachingStrategy {

        /** @return Lines of text from this cache if it exists, null otherwise. */
        fun tryRead(year: Int, day: Int): List<String>?

        /** @return Attempts to write the provided [lines] to an output file, if applicable. */
        fun write(year: Int, day: Int, lines: List<String>)

        /** Don't try to cache this to a local file, instead just always read from network. */
        data object None : CachingStrategy {
            override fun tryRead(year: Int, day: Int): List<String>? = null

            override fun write(year: Int, day: Int, lines: List<String>) {
                /* Do nothing. */
            }
        }

        /** Cache to a local text file from network on first read, then return from the local text file. */
        data class LocalTextFile(
            val cacheDirPath: String,
        ) : CachingStrategy {
            override fun tryRead(year: Int, day: Int): List<String>? {
                val path = Path(cacheDirPath, "$year", "$day.txt")
                return if (path.exists()) {
                    path.readLines()
                } else {
                    null
                }
            }

            override fun write(year: Int, day: Int, lines: List<String>) {
                val path = Path(cacheDirPath, "$year", "$day.txt")
                if (!path.exists()) {
                    Files.createDirectories(path.parent)
                    path.createFile()
                }
                path.writeLines(lines)
            }
        }

        class Custom(
            val tryRead: (year: Int, day: Int) -> List<String>?,
            val write: (year: Int, day: Int, lines: List<String>) -> Unit,
        ) : CachingStrategy {
            override fun tryRead(year: Int, day: Int): List<String>? = tryRead(year, day)

            override fun write(year: Int, day: Int, lines: List<String>) {
                write(year, day, lines)
            }
        }
    }
}