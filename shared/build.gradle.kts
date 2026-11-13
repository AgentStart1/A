/*
 * This is a private project. All rights reserved.
 */

import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("wasm-browser-test-interop")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}

val buildIosTarget = project.findProperty("target.ios") == "true"
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        compilerOptions {
            freeCompilerArgs.add("-Xwasm-attach-js-exception")
        }
    }

    android {
        namespace = "com.storyteller_f.alphaheart.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        packaging {
            resources.merges.add("META-INF/LICENSE.md")
        }
    }

    if (buildIosTarget) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    jvm()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val headlessTest =
            create("headlessTest") {
                dependsOn(commonTest.get())
            }
        val jvmAndroidMain =
            create("jvmAndroidMain") {
                dependencies {
                    implementation(libs.bcprov.jdk18on)
                    implementation(libs.bcpkix.jdk18on)
                    implementation(libs.bcutil.jdk18on)
                }
                dependsOn(commonMain.get())
            }
        val noJvmMain =
            create("noJvmMain") {
                dependsOn(commonMain.get())
            }
        androidMain.dependencies {
            implementation(libs.cryptography.provider.jdk)
        }
        androidMain {
            dependsOn(jvmAndroidMain)
            dependsOn(noJvmMain)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
            }
            dependsOn(headlessTest)
        }
        getByName("androidDeviceTest") {
            dependsOn(headlessTest)
            dependencies {
                implementation(libs.androidx.test.runner)
            }
        }
        commonMain.dependencies {
            api(projects.result)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.cryptography.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.markdown)
            implementation(libs.napier)
            implementation(libs.kotlinx.collections.immutable)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.cryptography.provider.jdk)
            implementation(libs.logback)
            implementation(libs.icu4j)
        }
        jvmMain {
            dependsOn(jvmAndroidMain)
        }
        jvmTest {
            dependsOn(headlessTest)
        }
        if (buildIosTarget) {
            iosMain.dependencies {
                implementation(libs.cryptography.provider.openssl3.prebuilt)
            }
            iosMain {
                dependsOn(noJvmMain)
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.cryptography.provider.webcrypto)
            implementation(npm("@noble/hashes", "1.7.2"))
            implementation(npm("ethereum-cryptography", "3.1.0"))
            implementation(npm("keccak", "3.0.4"))
            implementation(npm("ml-crypto", project.file("ml-crypto")))
        }
        wasmJsMain {
            dependsOn(noJvmMain)
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}
