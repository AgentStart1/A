/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileInputStream
import java.net.URI
import java.util.Properties

abstract class DownloadFont : DefaultTask() {
    @get:Input
    abstract val sourceUrl: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun download() {
        val destination = outputFile.get().asFile
        if (destination.isFile) return

        destination.parentFile.mkdirs()
        URI(sourceUrl.get()).toURL().openStream().buffered().use { input ->
            destination.outputStream().buffered().use { output ->
                input.copyTo(output)
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.serialization)
    id("com.mikepenz.aboutlibraries.plugin")
    id("wasm-browser-test-interop")
}

val buildIosTarget = project.findProperty("target.ios") == "true"
val flavorStr = project.findProperty("server.flavor") as String
val buildType = project.findProperty("server.buildType") as String

kotlin {
    android {
        namespace = "com.storyteller_f.alphaheart.app.android_library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        androidResources {
            enable = true
        }
        packaging {
            resources.merges.add("META-INF/LICENSE.md")
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
//        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
//        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    if (buildIosTarget) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "ComposeApp"
                isStatic = true
            }
        }
    }

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate()

    sourceSets {
        val headlessTest =
            create("headlessTest") {
                dependsOn(commonTest.get())
            }
        headlessTest.kotlin.exclude("platform_storage/**")
        val platformStorageTest =
            create("platformStorageTest") {
                dependsOn(headlessTest)
                kotlin.srcDir("src/headlessTest/kotlin/platform_storage")
                dependencies {
                    implementation(libs.androidx.room.runtime)
                }
            }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.session)
            implementation(libs.lifecycle.service)

            implementation(libs.connectivity.device)
            implementation(libs.connectivity.compose.device)
            implementation(libs.accompanist.permissions)

            implementation(libs.androidx.core.splashscreen)
            implementation(libs.litertlm.android)

            implementation(libs.connector) {
                exclude(group = "com.google.protobuf", module = "protobuf-java")
            }
            implementation(libs.okhttp)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
            }
            dependsOn(headlessTest)
        }
        getByName("androidDeviceTest") {
            dependsOn(platformStorageTest)
            dependencies {
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.ui.test.junit4.android)
            }
        }
        commonMain.dependencies {
            implementation(libs.androidx.navigationevent.compose)
            implementation(libs.ktor.webrtc)
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material)
            implementation(libs.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.ui)
            implementation(libs.ui.backhandler)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.common)
            implementation(libs.jetbrains.navigation3.ui)
            implementation(libs.jetbrains.material3.adaptiveNavigation3)
            implementation(libs.jetbrains.lifecycle.viewmodelNavigation3)

            implementation(projects.shared)
            implementation(projects.client.core)
            implementation(projects.api)
            implementation(projects.client.modelStorage)
            implementation(projects.client.room)
            implementation(projects.client.composeCore)

            // no ui
            implementation(libs.napier)
            implementation(libs.kotlinx.datetime)
            implementation(libs.bundles.ktor.client)
            implementation(libs.kim)
            implementation(libs.uri.kmp)
            implementation(libs.human.readable)
            implementation(libs.kfswatch)
            implementation(libs.kodio.core)
            // ui
            implementation(libs.material3.window.size)
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
            implementation(libs.qrose)
            implementation(libs.krop.ui)
            implementation(libs.androidx.datastore.preferences.core)

            implementation(libs.kmpnotifier.local)
            implementation(libs.haze)
            implementation(libs.haze.materials)
            implementation(libs.kmp.zip)
            implementation(libs.kmp.zip.kotlinx)
            implementation(libs.emoji.kt)
            implementation(libs.emoji.compose.m3)
            implementation(libs.connectivity.compose)
            implementation(libs.connectivity.core)

            implementation(libs.latex.base)
            implementation(libs.latex.renderer)
        }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.androidx.room.runtime)
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.ui.test)
        }
        wasmJsMain.dependencies {
            implementation(npm("download-file-worker", project.file("file-worker")))
        }
        jvmMain.dependencies {
            val nativePlatform =
                providers.systemProperty("os.name").map { name ->
                    when {
                        name.startsWith("Windows", ignoreCase = true) -> "windows"
                        name.startsWith("Mac", ignoreCase = true) -> "macos"
                        name.startsWith("Linux", ignoreCase = true) -> "linux"
                        else -> error("Unsupported RTC platform: $name")
                    }
                }
            val nativeArchitecture =
                providers.systemProperty("os.arch").map { architecture ->
                    when (architecture.lowercase()) {
                        "amd64", "x86_64" -> "x86_64"
                        "aarch64", "arm64" -> "aarch64"
                        else -> error("Unsupported RTC architecture: $architecture")
                    }
                }
            runtimeOnly(
                nativePlatform.zip(nativeArchitecture) { platform, architecture ->
                    "dev.onvoid.webrtc:webrtc-java:${libs.versions.webrtcJava.get()}:$platform-$architecture"
                },
            )
            implementation(libs.vlcj)
            implementation(libs.connectivity.http)
            implementation(libs.connectivity.compose.http)
            implementation(libs.tika.core)

            implementation(libs.litertlm.jvm)
        }
        jvmTest {
            dependsOn(platformStorageTest)
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
    androidRuntimeClasspath(libs.androidx.ui.test.manifest)
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.addAll(rootProject.layout.projectDirectory.file("stability_config.conf"))
}
val properties =
    Properties().apply {
        val file = layout.projectDirectory.file("../../deploy/$flavorStr.env").asFile
        if (file.exists()) {
            FileInputStream(file).use {
                load(it)
            }
        }
    }
val serverUrl =
    providers.gradleProperty("app.server.url").orNull
        ?: properties["SERVER_URL"] as? String
val wsServerUrl =
    providers.gradleProperty("app.ws.server.url").orNull
        ?: properties["WS_SERVER_URL"] as? String
val deepLinkHost =
    serverUrl?.let {
        URI.create(it).host
    } ?: "storyteller_f.com"

buildkonfig {
    packageName = "com.storyteller_f.alphaheart.app"
    objectName = "AppConfig"

    val isDebug = properties["app.DEBUG"] as? String ?: "false"
    defaultConfigs {
        buildConfigField(STRING, "SERVER_URL", serverUrl ?: "", const = true)
        buildConfigField(STRING, "WS_SERVER_URL", wsServerUrl ?: "", const = true)
        buildConfigField(STRING, "BUILD_TYPE", buildType, const = true)
        buildConfigField(STRING, "FLAVOR", flavorStr, const = true)
        buildConfigField(BOOLEAN, "ENABLE_LOGIN_CHECK", "false", const = true)
        buildConfigField(STRING, "DEEP_LINK_HOST", deepLinkHost, const = true)
        buildConfigField(STRING, "DEEP_LINK_SCHEME_PREFIX", "alphaheart-$flavorStr", const = true)
        buildConfigField(BOOLEAN, "DEBUG", isDebug, const = true)
    }
}

aboutLibraries {
    // Allow to enable "offline mode", will disable any network check of the plugin (including [fetchRemoteLicense] or pulling spdx license texts)
    offlineMode = true

    collect {
        // Define the path configuration files are located in. E.g. additional libraries, licenses to add to the target .json
        // Warning: Please do not use the parent folder of a module as path, as this can result in issues. More details: https://github.com/mikepenz/AboutLibraries/issues/936
        // The path provided is relative to the modules path (not project root)
        configPath = file("../config")

        // (optional) GitHub token to raise API request limit to allow fetching more licenses
        gitHubApiToken = if (hasProperty("github.pat")) property("github.pat")?.toString() else null

        // Enable fetching of "remote" licenses.  Uses the API of supported source hosts
        // See https://github.com/mikepenz/AboutLibraries#special-repository-support
        // A `gitHubApiToken` is required for this to work as it fetches information from GitHub's API.
        fetchRemoteLicense = false

        // Enables fetching of "remote" funding information. Uses the API of supported source hosts
        // See https://github.com/mikepenz/AboutLibraries#special-repository-support
        // A `gitHubApiToken` is required for this to work as it fetches information from GitHub's API.
        fetchRemoteFunding = false

        // Allows to only collect dependencies of specific variants during the `collectDependencies` step.
        // filterVariants.addAll("debug", "release")

        // Enable inclusion of `platform` dependencies in the library report
        includePlatform = true
    }

    export {
        // Define the output path (including fileName). Modifying this will disable the automatic meta data discovery for supported platforms.
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")

        // The default export variant to use for this module.
        // variant = "release"

        // Allows to exclude some fields from the generated meta data field.
        // If the class name is specified, the field is only excluded for that class; without a class name, the exclusion is global.
        excludeFields.addAll("developers", "funding")

        // Enable pretty printing for the generated JSON file
        prettyPrint = true
    }

    exports {
        // Define export configuration per variant.
        create("jvm") {
            outputFile = file("files/jvm/aboutlibraries.json")
        }
//        create("wasmJs") {
//            outputFile = file("files/wasmJs/aboutlibraries.json")
//        }
    }

    license {
        // Define the strict mode, will fail if the project uses licenses not allowed
        // - This will only automatically fail for Android projects using the Android-specific plugin (com.mikepenz.aboutlibraries.plugin.android)
        // For other projects, execute `exportLibraryDefinitions` manually
        strictMode = com.mikepenz.aboutlibraries.plugin.StrictMode.WARN

        // Allowed set of licenses, this project will be able to use without build failure
        allowedLicenses.addAll(
            "Apache-2.0",
            "asdkl",
            "MIT",
            "BSD-3-Clause",
            "The BSD License",
            "The 3-Clause BSD License",
        )

        // Allowed set of licenses for specific dependencies, this project will be able to use without build failure
        allowedLicensesMap =
            mapOf(
                "asdkl" to listOf("androidx.jetpack.library"),
                "NOASSERTION" to listOf("org.jetbrains.kotlinx"),
            )

        // Full license text for license IDs mentioned here will be included, even if no detected dependency uses them.
        // additionalLicenses.addAll("mit", "mpl_2_0")
    }

    library {
        // Enable the duplication mode, allows to merge, or link dependencies which relate
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.LINK
        // Configure the duplication rule, to match "duplicates" with
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

tasks.withType(KotlinCompile::class.java).configureEach {
    dependsOn("exportLibraryDefinitions")
}
tasks.named("copyNonXmlValueResourcesForCommonMain") {
    dependsOn("exportLibraryDefinitions")
}

tasks.withType<Test> {
    when (name) {
        "testDebugUnitTest", "testReleaseUnitTest", "testAndroidHostTest", "jvmTest", "testBenchmarkUnitTest" -> {
            exclude("**/device_based/*")
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.storyteller_f.alphaheart.app"
    generateResClass = auto
}

private fun KotlinDependencyHandler.implementation(
    dependencyNotation: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit,
) {
    implementation(dependencyNotation.get().toString(), configure)
}

// Should be run at least once before running the app
val downloadFonts =
    tasks.register<DownloadFont>("downloadFonts") {
        sourceUrl.set(
            "https://github.com/google/material-design-icons/raw/master/variablefont/" +
                "MaterialSymbolsOutlined%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf",
        )
        outputFile.set(
            layout.projectDirectory.file("src/commonMain/composeResources/font/material_symbols_outlined.ttf"),
        )
    }

tasks.named("copyNonXmlValueResourcesForCommonMain") {
    dependsOn(downloadFonts)
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "3096m"
}

tasks.named<Test>("jvmTest") {
    systemProperty("java.io.tmpdir", layout.buildDirectory.get().asFile.absolutePath)
    systemProperty(
        "app.protected.storage.dir",
        layout.buildDirectory.dir("test-protected-storage").get().asFile.absolutePath,
    )
}

// UI fixtures run separately from the regular host suite and share no backend/device state.
tasks.register<Test>("adaptiveUiTest") {
    dependsOn("jvmTestClasses")
    val hostTest = tasks.named<Test>("jvmTest").get()
    mustRunAfter(hostTest)
    testClassesDirs = hostTest.testClassesDirs
    classpath = hostTest.classpath
    include(
        "device_based/HomeAdaptiveUiTest*",
        "device_based/RoomPagingUiTest*",
        "device_based/FileExplorerNavigationUiTest*",
        "device_based/SignInPromptUiTest*",
        "device_based/CommunityFontRenderingUiTest*",
        "device_based/CommunityTopicFontsUiTest*",
        "device_based/TopicComposePageBlockEditorTest*",
    )
    systemProperty("java.io.tmpdir", layout.buildDirectory.get().asFile.absolutePath)
    systemProperty("user.language", "en")
    systemProperty("user.country", "US")
    maxParallelForks = 1
}
