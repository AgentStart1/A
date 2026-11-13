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
    implementation(libs.markdown)
}
