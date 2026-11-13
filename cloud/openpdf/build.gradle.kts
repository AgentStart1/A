/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
}

group = "com.storyteller_f.alphaheart.cloud"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.shared)
    implementation(libs.kotlinx.datetime)
    implementation(projects.cloud.pdf)
    implementation(libs.markdown)
    implementation(libs.openpdf)
    implementation(libs.highlights)
    implementation(libs.jna)
    testImplementation(kotlin("test"))
    testImplementation(libs.pdfbox)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    // Eight concurrent cold instances retain full-size CJK sources plus immutable output copies.
    maxHeapSize = "1g"
}
