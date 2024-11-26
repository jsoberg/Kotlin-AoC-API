plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("maven-publish")
}

group = "com.github.jsoberg"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    testImplementation(libs.test.assertk)
    testImplementation(libs.test.junitJupiter)
    testImplementation(libs.test.kotlin.coroutines)
}

tasks.withType<Test> {
    useJUnitPlatform()
}