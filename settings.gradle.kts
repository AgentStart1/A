@file:Suppress("UnstableApiUsage")

rootProject.name = "AlphaHeart"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // maven("https://jitpack.io") // Disabled - no JitPack dependencies
    }
}

include(":result")
include(":shared")

include(":app:composeApp")
include(":app:webApp")
include(":app:cliApp")
include(":app:androidApp")
include(":app:desktopApp")

include(":dev:wasmMediaExperiment")
include(":dev:testInfrastructure")
include(":dev:e2eCoverageRuntime")
include(":dev:e2eCore")

include(":dev:appiumCore")
include(":app:androidAppium")
include(":app:desktopAppium")
include(":app:linuxAppium")
include(":app:wasmAppium")
include(":panel:androidAppium")
include(":panel:desktopAppium")
include(":panel:wasmAppium")

include(":app:cliE2e")
include(":panel:cliE2e")

include(":cloud:server")
include(":cloud:runtime")
include(":cloud:ws-api")
include(":cloud:ws")
include(":cloud:cli")
include(":cloud:worker")
include(":cloud:service")
//include(":cloud:pdfbox") // Disabled - pdfbox-layout requires JitPack
include(":cloud:openpdf")
include(":cloud:pdf")
include(":panel:composeApp")
include(":panel:webApp")
include(":panel:androidApp")
include(":panel:cliApp")
include(":panel:desktopApp")

include(":backend:exposed")
include(":backend:core")
include(":backend:lucene")
include(":backend:elastic")
include(":backend:simple")
include(":backend:filesystem")
include(":backend:redis")
include(":backend:minio")
include(":cloud:filesystem-service")
include(":cloud:lucene-service")
include(":services:filesystem-protocol")
include(":services:lucene-protocol")

include(":client:core")
include(":client:bot-lib")
include(":client:composeCore")
include(":client:model-storage")
//include(":client:kotbase")
include(":client:room")
//include(":client:sqlite-now")
include(":client:asciidoc-parser")

include(":api")

include(":bot:builtin-bot")

include(":panel:benchmark")
include(":app:benchmark")

includeBuild("bgscripts")

// Preserve rapid CLI input until upstream Mosaic provides a lossless event queue.
// https://github.com/JakeWharton/mosaic/issues/1241
includeBuild("dev/mosaicInputRuntime") {
    dependencySubstitution {
        substitute(module("com.jakewharton.mosaic:mosaic-tty-terminal")).using(project(":"))
        substitute(module("com.jakewharton.mosaic:mosaic-tty-terminal-jvm")).using(project(":"))
    }
}

// Wasm-only Skiko source patch; Android/JVM keep their published artifacts.
includeBuild("dev/skikoMediaRuntime") {
    dependencySubstitution {
        substitute(module("org.jetbrains.skiko:skiko-wasm-js")).using(project(":"))
    }
}
