/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinxRpc)
    alias(libs.plugins.serialization)
}

group = "com.storyteller_f.alphaheart.cloud"
version = "unspecified"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.napier)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.rpc.core)
    implementation(libs.kotlinx.rpc.krpc.client)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
}
