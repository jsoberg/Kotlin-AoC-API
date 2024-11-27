package com.soberg.kotlin.aoc.api

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.exists
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import com.soberg.kotlin.aoc.api.AdventOfCodeInputApi.CachingStrategy
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.UnknownHostException
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.exists
import java.nio.file.Path as JavaNioPath

class AdventOfCodeInputApiTest {

    @AfterEach
    fun teardown() {
        unmockkObject(AdventOfCodeHttpInputQuery)
    }

    @Test
    fun `store in cache directory for LocalTextFile cache strategy`(
        @TempDir tempDir: JavaNioPath,
    ) = runTest {
        val api = AdventOfCodeInputApi(CachingStrategy.LocalTextFile(tempDir.absolutePathString()))
        setupMockHttpEngine()

        assertThat(Path(tempDir.absolutePathString(), "2024", "Day02.txt").exists())
            .isFalse()
        api.readInput(2024, 2, "token")
        assertThat(Path(tempDir.absolutePathString(), "2024", "Day02.txt"))
            .exists()
    }

    @Test
    fun `read from cache when stored instead of network for LocalTextFile cache strategy`(
        @TempDir tempDir: JavaNioPath,
    ) = runTest {
        val api = AdventOfCodeInputApi(CachingStrategy.LocalTextFile(tempDir.absolutePathString()))
        setupMockHttpEngine(bodyContent = "1\n2\n3 and me\n")

        val result = api.readInput(2024, 1, "token")
        assertThat(result.getOrNull())
            .isNotNull()
            .containsExactly("1", "2", "3 and me")

        // Assert that even though the "network" returns something else, we should get the same cached result as before.
        setupMockHttpEngine(bodyContent = "a and c\nb\nd\n")
        val cachedResult = api.readInput(2024, 1, "token")
        assertThat(cachedResult.getOrNull())
            .isNotNull()
            .containsExactly("1", "2", "3 and me")
    }

    @Test
    fun `read from cache for Custom cache strategy`() = runTest {
        val cachingStrategy = CachingStrategy.Custom(
            tryReadBlock = { _, _ -> listOf("1", "2") },
            writeBlock = { _, _, _ -> },
        )
        val api = AdventOfCodeInputApi(cachingStrategy)
        setupMockHttpEngine()

        val result = api.readInput(2024, 1, "token")
        assertThat(result.getOrNull())
            .isNotNull()
            .containsExactly("1", "2")
    }

    @Test
    fun `store in cache for Custom cache strategy`(
        @TempDir tempDir: JavaNioPath,
    ) = runTest {
        val cachingStrategy = CachingStrategy.Custom(
            tryReadBlock = { _, _ -> null },
            writeBlock = { _, _, _ -> Path(tempDir.absolutePathString(), "TEST.txt").createFile() },
        )
        val api = AdventOfCodeInputApi(cachingStrategy)
        setupMockHttpEngine()

        api.readInput(2024, 1, "token")
        assertThat(Path(tempDir.absolutePathString(), "TEST.txt"))
            .exists()
    }

    @Test
    fun `read expected line input from network for success`() = runTest {
        val api = AdventOfCodeInputApi(CachingStrategy.None)
        setupMockHttpEngine(
            bodyContent = "a\nb\nc\nd",
            statusCode = HttpStatusCode.OK,
        )

        val result = api.readInput(2024, 1, "token")
        assertThat(result.getOrNull())
            .isNotNull()
            .containsExactly("a", "b", "c", "d")
    }

    @Test
    fun `return failure result for non-200 status`() = runTest {
        val api = AdventOfCodeInputApi(CachingStrategy.None)
        setupMockHttpEngine(
            statusCode = HttpStatusCode.BadRequest,
        )

        val result = api.readInput(2024, 1, "token")
        assertThat(result.exceptionOrNull())
            .isNotNull()
            .hasMessage("Unexpected response code 400")
    }

    @Test
    fun `return failure result when exception thrown`() = runTest {
        val api = AdventOfCodeInputApi(CachingStrategy.None)
        val exception = UnknownHostException("Test")
        mockkObject(AdventOfCodeHttpInputQuery)
        coEvery { AdventOfCodeHttpInputQuery.createClient(any()) } throws exception

        val result = api.readInput(2024, 1, "token")
        assertThat(result.exceptionOrNull())
            .isEqualTo(exception)
    }

    private fun setupMockHttpEngine(
        bodyContent: String = "",
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ) {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(bodyContent),
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        mockkObject(AdventOfCodeHttpInputQuery)
        coEvery { AdventOfCodeHttpInputQuery.createClient(any()) } returns HttpClient(mockEngine)
    }
}