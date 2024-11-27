package com.soberg.kotlin.aoc.api

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

class AdventOfCodeInputApi(
    private val cachingStrategy: CachingStrategy,
) {
    /** Blocking (non-coroutine) version of [readInput]. */
    fun blockingReadInput(
        year: Int,
        day: Int,
        sessionToken: String,
    ) = runBlocking {
        readInput(
            year = year,
            day = day,
            sessionToken = sessionToken,
        )
    }

    /** Attempts to read from cache based on the specified [cachingStrategy].
     * If no cache is read, this will read from network and attempt to store in cache.
     *
     *  @return The read lines of input for the specified [year] and [day] if success, exception describing the error if failure.
     */
    suspend fun readInput(
        /** The year of AoC input that should be read (e.g. 2024). */
        year: Int,
        /** The day of AoC input that should be read (e.g. 1 for the first day of AoC). */
        day: Int,
        /** The session token (grabbed from the Cookie header when logged into AoC online) to be used to grab your specific user input. */
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
        val response: HttpResponse = AdventOfCodeHttpInputQuery.runQuery(
            year = year,
            day = day,
            sessionToken = sessionToken,
        )
        if (response.status.value in 200..299) {
            return response.bodyAsText()
                // Trim to remove trailing next-line chars.
                .trim()
                .lines()
        } else {
            error("Unexpected response code ${response.status.value}")
        }
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
            /** Attempts to read from a local cache file in the format <cacheDirPath>/<year>/Day<day>.txt */
            override fun tryRead(year: Int, day: Int): List<String>? {
                val path = Path(cacheDirPath, "$year", filenameForDay(day))
                return if (path.exists()) {
                    path.readLines()
                } else {
                    null
                }
            }

            private fun filenameForDay(day: Int) = "Day${"%02d".format(day)}.txt"

            /** Attempts to write to a local cache file in the format <cacheDirPath>/<year>/Day<day>.txt */
            override fun write(year: Int, day: Int, lines: List<String>) {
                val path = Path(cacheDirPath, "$year", filenameForDay(day))
                if (!path.exists()) {
                    Files.createDirectories(path.parent)
                    path.createFile()
                }
                path.writeLines(lines)
            }
        }

        class Custom(
            private val tryReadBlock: (year: Int, day: Int) -> List<String>?,
            private val writeBlock: (year: Int, day: Int, lines: List<String>) -> Unit,
        ) : CachingStrategy {
            override fun tryRead(year: Int, day: Int): List<String>? = tryReadBlock(year, day)

            override fun write(year: Int, day: Int, lines: List<String>) {
                writeBlock(year, day, lines)
            }
        }
    }
}