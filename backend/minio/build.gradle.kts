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
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.backend.core)
    implementation(projects.shared)
    implementation(libs.minio)
    implementation(libs.urlbuilder)

    testImplementation(kotlin("test"))
    testImplementation(libs.testcontainers.minio)
    testRuntimeOnly(projects.backend.simple)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
