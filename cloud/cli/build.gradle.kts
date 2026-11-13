/*
 * This is a private project. All rights reserved.
 */

plugins {
    id("test-docker-image")
    application
    alias(libs.plugins.kotlinJvm)
    id("cloud")
    id("merge-services")
    alias(libs.plugins.kotlinBuildConfig)
}

group = "com.storyteller_f.alphaheart.cloud"
version = "unspecified"

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(projects.backend.lucene)
    testImplementation(projects.services.luceneProtocol)

    implementation(projects.shared)
    implementation(projects.backend.core)
    implementation(projects.backend.exposed)
    implementation(projects.cloud.service)
    implementation(libs.napier)
    implementation(libs.kotlinx.cli)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.cryptography.provider.jdk)
    implementation(libs.tika.core)
    implementation(libs.bundles.ktor.server.client)
    implementation(libs.kotlinx.datetime)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "com.storyteller_f.alphaheart.cloud.cli.CliMainKt"
    applicationDefaultJvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}
