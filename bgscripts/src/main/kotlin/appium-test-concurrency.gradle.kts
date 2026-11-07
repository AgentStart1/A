/*
 * This is a private project. All rights reserved.
 */

plugins {
    id("test-artifact-reports")
}

val platform = when (project.name) {
    "androidAppium" -> "android"
    "desktopAppium", "linuxAppium" -> "desktop"
    "wasmAppium" -> "browser"
    else -> error("Unsupported Appium runner: ${project.path}")
}
val platformTestService = gradle.sharedServices.registerIfAbsent(
    "appium-$platform", AppiumTestBuildService::class,
) {
    maxParallelUsages = 1
}

tasks.withType<Test>().matching { it.name in setOf("appiumTest", "windowsMediaTest") }.configureEach {
    usesService(platformTestService)
    maxParallelForks = 1
}
