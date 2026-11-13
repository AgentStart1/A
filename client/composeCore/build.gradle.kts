/*
 * This is a private project. All rights reserved.
 */

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    id("wasm-browser-test-interop")
}

val buildIosTarget = project.findProperty("target.ios") == "true"
kotlin {
    android {
        namespace = "com.storyteller_f.alphaheart.client.compose_core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTest { }
    }

    if (buildIosTarget) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    sourceSets {
        val headlessTest =
            create("headlessTest") {
                dependsOn(commonTest.get())
            }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.session)
            implementation(libs.lifecycle.service)

            implementation(libs.record.core)
            implementation(libs.connectivity.device)
            implementation(libs.connectivity.compose.device)
            implementation(libs.accompanist.permissions)

            implementation(libs.androidx.datastore.preferences.core)

            implementation(libs.androidx.ui.tooling.preview)
            implementation(libs.okhttp)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
            }
            dependsOn(headlessTest)
        }

        commonMain.dependencies {
            api(libs.compose.native.webview)
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material)
            implementation(libs.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)

            implementation(projects.shared)
            implementation(projects.client.core)
            implementation(projects.client.asciidocParser)
            implementation(projects.api)
            implementation(projects.client.modelStorage)
            implementation(projects.client.room)

            implementation(libs.napier)
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            // ui
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.material3.window.size)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
            implementation(libs.bundles.coil)
            implementation(libs.bundles.markdown.render)
            implementation(libs.sonner)
            implementation(libs.highlights)
            implementation(libs.richeditor.compose)
            implementation(libs.bundles.filekit)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.zoomimage.compose.coil3)
            implementation(libs.fonticons.core)
            implementation(libs.krop.ui)

            implementation(libs.haze)
            implementation(libs.haze.materials)
            implementation(libs.emoji.kt)
            implementation(libs.emoji.compose.m3)
            implementation(libs.connectivity.compose)
            implementation(libs.connectivity.core)

            implementation(libs.kim)
            implementation(libs.uri.kmp)
            implementation(libs.human.readable)
            implementation(libs.kfswatch)

            implementation(libs.latex.base)
            implementation(libs.latex.renderer)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))

            implementation(libs.ui.test)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.navigationevent.compose)
            implementation(libs.nucleus.application)
            implementation(libs.nucleus.decorated.window.tao)
            implementation(libs.jna.platform)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)

            implementation(libs.vlcj)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.connectivity.http)
            implementation(libs.connectivity.compose.http)
            implementation(libs.tika.core)
        }
        jvmTest {
            dependsOn(headlessTest)
        }
        getByName("wasmJsMain") {
            dependencies {
                implementation(libs.mermaid.ffi)
                implementation(libs.kotlinx.browser)
                implementation(npm("local-font-access", project.file("local-font-access")))
                implementation(npm("wasm-media-host", project.file("wasm-media-host")))
                implementation(npm("mermaid-ffi-wasm", libs.versions.mermaidFfi.get()))
            }
        }
        wasmJsTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
        // jvm 与 android 共享 compose-pdf 的 PdfView actual（wasm 上无对应库）。
        val jvmAndroidMain =
            create("jvmAndroidMain") {
                dependsOn(commonMain.get())
                dependencies {
                    implementation(libs.compose.pdf)
                    implementation(libs.mermaid.ffi)
                }
            }
        jvmMain.get().dependsOn(jvmAndroidMain)
        androidMain.get().dependsOn(jvmAndroidMain)
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
    androidRuntimeClasspath(libs.androidx.ui.test.manifest)
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.storyteller_f.alphaheart.client.compose_core"
    generateResClass = auto
}

// Platform integration is explicit; ordinary JVM checks remain portable.
tasks.named<Test>("jvmTest") {
    exclude("**/WindowsProtectedStoreTest*", "**/MacProtectedStoreTest*", "**/NativeMediaPlaybackTest*")
}
tasks.register<Test>("windowsProtectedStoreTest") {
    group = "verification"
    description = "Verifies real Windows protected storage and filesystem write failures."
    dependsOn("jvmTestClasses")
    val hostTest = tasks.named<Test>("jvmTest").get()
    testClassesDirs = hostTest.testClassesDirs
    classpath = hostTest.classpath
    useJUnit()
    filter { includeTestsMatching("*.WindowsProtectedStoreTest") }
    maxParallelForks = 1
}

tasks.register<Test>("macosProtectedStoreTest") {
    group = "verification"
    description = "Verifies real macOS Keychain values and locked-keychain write failures."
    dependsOn("jvmTestClasses")
    val hostTest = tasks.named<Test>("jvmTest").get()
    testClassesDirs = hostTest.testClassesDirs
    classpath = hostTest.classpath
    useJUnit()
    filter { includeTestsMatching("*.MacProtectedStoreTest") }
    maxParallelForks = 1
}

tasks.named("wasmJsBrowserTest") {
    inputs.dir(layout.projectDirectory.dir("karma.config.d"))
}

val testFontGateRuntime =
    tasks.register<Exec>("testFontGateRuntime") {
        group = "verification"
        description = "Verify Wasm font preference, authorization and cancellation transitions."
        inputs.dir("local-font-access")
        commandLine("node", "--test", "local-font-access/fontGateHost.test.mjs")
    }

tasks.named("check") { dependsOn(testFontGateRuntime) }

tasks.register<Test>("nativeMediaTest") {
    group = "verification"
    description = "Verifies real VLC playback, native controls, expansion and explicit network-error recovery."
    dependsOn("jvmTestClasses")
    val hostTest = tasks.named<Test>("jvmTest").get()
    testClassesDirs = hostTest.testClassesDirs
    classpath = hostTest.classpath
    useJUnit()
    filter { includeTestsMatching("*.NativeMediaPlaybackTest") }
    maxParallelForks = 1
    outputs.upToDateWhen { false }
}
