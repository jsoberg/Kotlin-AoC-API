plugins {
    kotlin("jvm") version libs.versions.kotlin
    id("maven-publish")
}

group = Publishing.GroupId
version = Publishing.Version

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
    testImplementation(libs.test.ktor.mock)
    testImplementation(libs.test.mockk)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = Publishing.ArtifactId
            groupId = Publishing.GroupId
            version = Publishing.Version

            from(components["java"])
        }
    }
}