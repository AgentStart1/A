/*
 * This is a private project. All rights reserved.
 */

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.google.common.base.CaseFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
//    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
    alias(libs.plugins.buildconfig)
}
val buildIosTarget = project.findProperty("target.ios") == "true"
val flavorStr = project.findProperty("server.flavor") as String
val flavorId = CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(flavorStr)!!
val buildType = project.findProperty("server.buildType") as String
kotlin {
    android {
        namespace = "com.storyteller_f.alphaheart.panel.android_library"
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
        withHostTest { }
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    if (buildIosTarget) {
        listOf(
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

    run {
//        js {
//            browser()
//            binaries.executable()
//        }

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            binaries.executable()
            browser()
        }
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

            implementation(libs.connectivity.device)
            implementation(libs.connectivity.compose.device)
            implementation(libs.accompanist.permissions)

            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.datastore.preferences.core)
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
            dependencies {
                implementation(libs.androidx.ui.test.junit4.android)
            }
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material)
            implementation(libs.material.icons.extended)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
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
            implementation(libs.krop.ui)

            implementation(libs.haze)
            implementation(libs.haze.materials)
            implementation(libs.emoji.kt)
            implementation(libs.emoji.compose.m3)
            implementation(libs.connectivity.compose)
            implementation(libs.connectivity.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)

            implementation(libs.ui.test)
        }
        jvmMain.dependencies {
            implementation(libs.vlcj)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.connectivity.http)
            implementation(libs.connectivity.compose.http)
            implementation(libs.tika.core)
        }
        jvmTest {
            dependsOn(headlessTest)
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.addAll(rootProject.layout.projectDirectory.file("stability_config.conf"))
}

dependencies {
    androidRuntimeClasspath(libs.ui.tooling)
    androidRuntimeClasspath(libs.androidx.ui.test.manifest)
}

buildkonfig {
    packageName = "com.storyteller_f.alphaheart.panel"
    objectName = "PanelConfig"
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
    defaultConfigs {
        buildConfigField(STRING, "SERVER_URL", serverUrl ?: "", const = true)
        buildConfigField(STRING, "BUILD_TYPE", buildType, const = true)
        buildConfigField(STRING, "FLAVOR", flavorStr, const = true)
        buildConfigField(BOOLEAN, "ENABLE_LOGIN_CHECK", "false", const = true)
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.storyteller_f.alphaheart.panel"
    generateResClass = auto
}

private fun KotlinDependencyHandler.implementation(
    dependencyNotation: Provider<MinimalExternalModuleDependency>,
    configure: ExternalModuleDependency.() -> Unit,
) {
    implementation(dependencyNotation.get().toString(), configure)
}
