# Kotlin Advent of Code API :santa:
![JitPack](https://img.shields.io/jitpack/version/com.github.jsoberg/Kotlin-AoC-API) [![](https://jitci.com/gh/jsoberg/Kotlin-AoC-API/svg)](https://jitci.com/gh/jsoberg/Kotlin-AoC-API)

Simple Kotlin API for pulling input data for a specified year/day for [Advent of Code](https://adventofcode.com/) from network, and then caching locally if desired.

## Setup

This can be added to a [Kotlin Advent of Code template repository](https://github.com/kotlin-hands-on/advent-of-code-kotlin-template) or any other Kotlin JVM project through [Jitpack](https://jitpack.io/#jsoberg/Kotlin-AoC-API), in your `build.gradle.kts`:

```kts
dependencies {
    implementation("com.github.jsoberg:Kotlin-AoC-API:1.0")
}
```


### Using the Advent of Code Session Token

Note that input for Advent of Code is unique for each user - therefore, you need your account's session token in order to get your input. This will be in the `Cookie` header on the request made for an input page in Advent of Code (e.g. https://adventofcode.com/2023/day/1/input) and can be obtained in your browser. The header value is in the format `session=<token>`, where you'll want to copy the (non-human readable) `<token>` value; [this](https://github.com/GreenLightning/advent-of-code-downloader?tab=readme-ov-file#how-do-i-get-my-session-cookie) overview from [GreenLightning](https://github.com/GreenLightning) can help to find the session token using various browser types.

**Your session token is a secret unique to your account, and should not be stored in git**. For the simple use case of a [Kotlin Advent of Code template repository](https://github.com/kotlin-hands-on/advent-of-code-kotlin-template), you can store the token value in a `session-token.secret` file of your root repository directory and read it in code like so:
```kotlin
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

private fun readSessionToken(): String {
    val secretTokenFile = Path("session-token.secret")
    require(secretTokenFile.exists()) {
        "session-token.secret file must exist and contain the sessionToken for Advent of Code"
    }
    return secretTokenFile.readText().trim()
}
```
**Remember to add the session-token.secret file to your `.gitignore` file**. Additionally, it's recommended to keep your unique user input private; if you're using storing cached input, make sure input locations are also specified in your `.gitignore` file.


### Setup Example

An example of full setup when starting from the template can be found [here](https://github.com/jsoberg/2024-Kotlin-Advent-Of-Code/commit/1a3595be3bd9c1eeb2715c75bb1464a494f89977).

## Usage

Example usage which will cache input for each year/day combination in the `input` folder of the repository's root directory:
```kotlin
import com.soberg.kotlin.aoc.api.AdventOfCodeInputApi

fun readInput(year: Int, day: Int) = AdventOfCodeInputApi(
    cachingStrategy = AdventOfCodeInputApi.CachingStrategy.LocalTextFile("input")
).blockingReadInput(
    year = year, // e.g. 2015-2024
    day = day, // e.g. 1-24
    sessionToken = sessionToken, // e.g. "21216c7...314d"
).getOrThrow()
```
There are 3 available caching strategies:
- `AdventOfCodeInputApi.CachingStrategy.None` - Performs no caching, reads from network every time.
- `AdventOfCodeInputApi.CachingStrategy.LocalTextFile` - Caches to a local text file in `input/<year>/Day<day>.txt` from root, reading from this location instead of network on subsequent calls.
- `AdventOfCodeInputApi.CachingStrategy.Custom` - Fully custom caching logic, if desired.
