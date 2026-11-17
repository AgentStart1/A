/*
 * This is a private project. All rights reserved.
 */

pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
dependencyResolutionManagement {
    repositories { mavenCentral(); google() }
    versionCatalogs { create("libs") { from(files("../../gradle/libs.versions.toml")) } }
}
rootProject.name = "richeditor-compose"

// KGP's Wasm package graph resolves composite identities from this build as well as the root build.
// Keep the same two Web runtimes for standalone and parent composite tasks.
includeBuild("../composeWebInputRuntime") {
    dependencySubstitution {
        substitute(module("org.jetbrains.compose.ui:ui-wasm-js")).using(project(":"))
    }
}
includeBuild("../skikoMediaRuntime") {
    dependencySubstitution {
        substitute(module("org.jetbrains.skiko:skiko-wasm-js")).using(project(":"))
    }
}
