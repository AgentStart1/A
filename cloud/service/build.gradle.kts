/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlinBuildConfig)
    id("me.champeau.jmh") version "0.7.4"
}

group = "com.storyteller_f.alphaheart.cloud"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.napier)
    implementation(libs.cryptography.provider.jdk)
    implementation(projects.shared)
    implementation(projects.api)
    implementation(projects.backend.core)
    implementation(projects.cloud.pdf)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.tika.core)
    implementation(projects.backend.elastic)
    implementation(projects.backend.filesystem)
    implementation(projects.backend.lucene)
    implementation(projects.backend.minio)
    implementation(projects.backend.redis)
    implementation(projects.backend.simple)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.commons.logging)
    testImplementation(kotlin("test"))
    implementation(libs.commons.imaging)
    jmhImplementation(libs.testcontainers.elasticsearch)
}

tasks.withType<Test> {
    jvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

jmh {
    warmupIterations = 2
    iterations = 2
    fork = 2
}
