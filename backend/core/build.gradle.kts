/*
 * This is a private project. All rights reserved.
 */

plugins {
    `java-test-fixtures`
    alias(libs.plugins.kotlinJvm)
}

group = "com.storyteller_f.alphaheart.backend"
version = "unspecified"

dependencies {
    implementation(libs.napier)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.shared)
    implementation(libs.kotlinx.datetime)
    testFixturesImplementation(libs.ktor.server.core)
    testFixturesImplementation(libs.ktor.server.cio)
    testFixturesImplementation(libs.ktor.server.websockets)
    testFixturesImplementation(libs.kotlinx.rpc.krpc.ktor.server)
    testFixturesImplementation(libs.kotlinx.rpc.krpc.serialization.json)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.backend.elastic)
    testImplementation(projects.backend.lucene)
    testImplementation(projects.backend.minio)
    testImplementation(projects.backend.filesystem)
    testImplementation(projects.backend.exposed)
    testImplementation(projects.backend.simple)
    testImplementation(libs.testcontainers.elasticsearch)
    testImplementation(libs.testcontainers.minio)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.h2)
    testImplementation(libs.postgresql)
    runtimeOnly(libs.image.avif)
}

tasks.test {
    dependsOn(":cloud:filesystem-service:buildTestDockerImage", ":cloud:lucene-service:buildTestDockerImage")
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
