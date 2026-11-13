/*
 * This is a private project. All rights reserved.
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("appium-runtime-classpath")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(projects.app.composeApp)
    implementation(projects.client.composeCore)
    implementation(projects.shared)

    implementation(libs.foundation)
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.decorated.window.tao)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.napier)
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.addAll(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

compose.desktop {
    application {
        mainClass = "com.storyteller_f.alphaheart.app.JvmMainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.storyteller_f.alphaheart.app"
            packageVersion = "1.0.0"
        }
        buildTypes.release.proguard {
            version.set("7.5.0")
            isEnabled = false
            obfuscate = true
            optimize = true
            configurationFiles.from(file("proguard-rules-desktop.pro"))
        }
    }
}
