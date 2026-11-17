/*
 * This is a private project. All rights reserved.
 */
pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement { repositories { mavenCentral(); google() } }
rootProject.name = "ui-wasm-js"
// Keep the same GPU media runtime when this source build is tested independently.
includeBuild("../skikoMediaRuntime") {
    dependencySubstitution {
        substitute(module("org.jetbrains.skiko:skiko-wasm-js")).using(project(":"))
    }
}
