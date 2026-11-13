/*
 * This is a private project. All rights reserved.
 */

plugins {
    id("test-docker-image")
    application
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.serialization)
}

group = "com.storyteller_f.alphaheart.cloud"
version = "unspecified"

application {
    mainClass.set("com.storyteller_f.alphaheart.cloud.filesystem.FileSystemServiceKt")
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(projects.result)
    implementation(libs.logback)
    implementation(projects.services.filesystemProtocol)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.urlbuilder)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.partial.content)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.kotlinx.rpc.krpc.server)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.websockets)
    testImplementation(libs.kotlinx.rpc.krpc.ktor.client)
}

tasks.test {
    useJUnitPlatform()
}
