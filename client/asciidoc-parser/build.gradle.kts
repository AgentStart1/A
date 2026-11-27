/*
 * This is a private project. All rights reserved.
 */

@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
}

val buildIosTarget = project.findProperty("target.ios") == "true"
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    android {
        namespace = "com.storyteller_f.alphaheart.client.asciidoc_parser"
        compileSdk {
            version =
                release(libs.versions.android.compileSdk.get().toInt()) {
                    minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
                }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest { }
//        optimization {
//            consumerKeepRules.apply {
//                publish = true
//                file("consumer-rules.pro")
//            }
//        }
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
        val jvmAndroidTest = create("jvmAndroidTest")
        val headlessTest =
            create("headlessTest") {
                dependsOn(commonTest.get())
            }
        jvmAndroidTest.dependsOn(headlessTest)
        wasmJsTest.get().dependsOn(headlessTest)
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.components.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
            }
            dependsOn(jvmAndroidTest)
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(libs.androidx.ui.test.junit4.android)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        wasmJsMain.dependencies {
            implementation(npm("@asciidoctor/core", "3.0.4"))
        }

        val jvmAndroidMain = create("jvmAndroidMain") { dependsOn(commonMain.get()) }
        jvmMain.get().dependsOn(jvmAndroidMain)
        androidMain.get().dependsOn(jvmAndroidMain)
        jvmTest {
            dependsOn(jvmAndroidTest)
        }
        if (buildIosTarget) {
            iosMain.dependencies {
            }
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}

dependencies {
    androidRuntimeClasspath(libs.androidx.ui.test.manifest)
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.storyteller_f.alphaheart.client.asciidoc_parser"
    generateResClass = auto
}
