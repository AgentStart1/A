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
    mainClass.set("com.storyteller_f.alphaheart.cloud.lucene.LuceneServiceKt")
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(libs.logback)
    implementation(projects.services.luceneProtocol)
    implementation(libs.bundles.lucene)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.kotlinx.rpc.krpc.server)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
