/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
}

group = "com.storyteller_f.alphaheart.backend"
version = "unspecified"

dependencies {
    implementation(libs.napier)
    implementation(libs.bundles.exposed)

    implementation(projects.backend.core)
    implementation(projects.shared)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
