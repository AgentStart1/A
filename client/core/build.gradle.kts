/*
 * This is a private project. All rights reserved.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}

val buildIosTarget = project.findProperty("target.ios") == "true"
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    android {
        namespace = "com.storyteller_f.alphaheart.client.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTest {}
    }

    if (buildIosTarget) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    jvm()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    sourceSets {
        val headlessTest =
            create("headlessTest") {
                dependsOn(commonTest.get())
            }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        getByName("androidHostTest") {
            dependsOn(headlessTest)
        }
        commonMain.dependencies {
            implementation(libs.napier)
            implementation(libs.kotlinx.collections.immutable)
            implementation(projects.shared)
            implementation(libs.bundles.ktor.client)
            implementation(libs.kotlinx.datetime)
            implementation(projects.api)
            implementation(libs.endpoint4k.common)
            implementation(libs.endpoint4k.ktor.client)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmTest {
            dependsOn(headlessTest)
        }
        if (buildIosTarget) {
            iosMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
    }
}
