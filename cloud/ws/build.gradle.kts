/*
 * This is a private project. All rights reserved.
 */

plugins {
    id("test-docker-image")
    application
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kotlinBuildConfig)
    alias(libs.plugins.kotlinxRpc)
    id("cloud")
    id("merge-services")
}

group = "com.storyteller_f.alphaheart.cloud"
version = "unspecified"

application {
    mainClass.set("com.storyteller_f.alphaheart.cloud.ws.WsApplicationKt")
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.napier)
    implementation(projects.shared)
    implementation(projects.api)
    implementation(projects.backend.core)
    implementation(projects.backend.exposed)
    implementation(projects.cloud.service)
    implementation(projects.cloud.runtime)
    implementation(projects.cloud.wsApi)
    implementation(libs.bundles.ktor.server)
    implementation(libs.ktor.server.netty)
    implementation(libs.bundles.ktor.server.client)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.datetime)
    implementation(libs.geoip2)
    implementation(libs.kotlinx.rpc.krpc.server)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("mergeServiceFiles") {
    dependsOn(":cloud:ws-api:jar")
    dependsOn(":cloud:runtime:jar")
}
