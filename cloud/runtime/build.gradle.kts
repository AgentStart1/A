/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
}

group = "com.storyteller_f.alphaheart.cloud"
version = "unspecified"

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.kreds)
    testImplementation(kotlin("test"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.ktor.server.test.host)
    implementation(libs.napier)
    implementation(projects.shared)
    implementation(projects.backend.core)
    implementation(projects.cloud.service)
    implementation(libs.endpoint4k.common)
    implementation(libs.endpoint4k.ktor.server)
    implementation(libs.bundles.ktor.server)
    implementation(libs.geoip2)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.collections.immutable)
}
