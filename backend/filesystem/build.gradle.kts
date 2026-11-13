/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinBuildConfig)
}

group = "com.storyteller_f.alphaheart.backend"
version = "unspecified"

dependencies {
    implementation(libs.napier)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.services.filesystemProtocol)
    implementation(projects.backend.core)
    implementation(projects.shared)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.rpc.krpc.client)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(testFixtures(projects.backend.core))
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
