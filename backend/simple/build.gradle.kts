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
    implementation(projects.backend.core)
    implementation(projects.shared)
    implementation(libs.kotlinx.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    jvmArgs = listOf("--add-modules", "jdk.incubator.vector")
}
