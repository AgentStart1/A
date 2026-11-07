/*
 * This is a private project. All rights reserved.
 */

plugins {
    alias(libs.plugins.kotlinJvm)
    id("appium-test-concurrency")
}

val appiumTest = sourceSets.create("appiumTest")
val windowsMediaTest = sourceSets.create("windowsMediaTest")

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(projects.dev.appiumCore)
}

listOf(appiumTest, windowsMediaTest).forEach { tests ->
    configurations.named(tests.implementationConfigurationName) {
        extendsFrom(configurations.testImplementation.get())
    }
    configurations.named(tests.runtimeOnlyConfigurationName) {
        extendsFrom(configurations.testRuntimeOnly.get())
    }
}

tasks.register<Test>("appiumTest") {
    description = "Runs the App Desktop Appium tests."
}

tasks.register<Test>("windowsMediaTest") {
    description = "Runs full-application Windows media controls with real VLC."
}

tasks.withType<Test>().matching { it.name in setOf("appiumTest", "windowsMediaTest") }.configureEach {
    group = "verification"
    outputs.upToDateWhen { false }
    val tests = if (name == "windowsMediaTest") windowsMediaTest else appiumTest
    testClassesDirs = tests.output.classesDirs
    classpath = tests.runtimeClasspath
    dependsOn(
        ":cloud:server:buildTestDockerImage",
        ":cloud:worker:buildTestDockerImage",
        ":cloud:cli:buildTestDockerImage",
        ":cloud:ws:buildTestDockerImage",
        ":cloud:filesystem-service:buildTestDockerImage",
        ":cloud:lucene-service:buildTestDockerImage",
        ":app:desktopApp:writeAppiumRuntimeClasspath",
    )
}
