/*
 * This is a private project. All rights reserved.
 */

pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement {
    repositories { mavenCentral(); google() }
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "richeditor-compose"

// Match the parent build's Skiko media runtime; Compose uses official 1.12 artifacts.
includeBuild("../skikoMediaRuntime") {
    dependencySubstitution {
        substitute(module("org.jetbrains.skiko:skiko-wasm-js")).using(project(":"))
    }
}
