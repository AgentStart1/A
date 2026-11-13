/*
 * This is a private project. All rights reserved.
 */

import com.google.common.base.CaseFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.easylauncher)
    alias(libs.plugins.screenshot)
    id("compose-android")
}
val buildIosTarget = project.findProperty("target.ios") == "true"
val flavorStr = project.findProperty("server.flavor") as String
val flavorId = CaseFormat.LOWER_HYPHEN.converterTo(CaseFormat.LOWER_UNDERSCORE).convert(flavorStr)!!
val buildType = project.findProperty("server.buildType") as String

android {
    namespace = "com.storyteller_f.alphaheart.panel"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "com.storyteller_f.alphaheart.panel.$flavorId"
    }
    buildTypes {
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    lint {
        disable.add("Instantiatable")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(projects.client.composeCore)
    implementation(projects.panel.composeApp)
    implementation(projects.shared)
    implementation(projects.client.core)

    // 新增依赖 - 从 composeApp/androidMain 复制
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

    // 新增依赖 - 从 app/androidApp 复制
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.bundles.filekit)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.napier)

    androidTestImplementation(libs.androidx.ui.test.junit4.android)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.bundles.ktor.client)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    screenshotTestImplementation(projects.client.composeCore)
    screenshotTestImplementation(projects.panel.composeApp)
    screenshotTestImplementation(projects.shared)
    screenshotTestImplementation(projects.client.core)
    screenshotTestImplementation(libs.kotlinx.datetime)
    screenshotTestImplementation(libs.runtime)
    screenshotTestImplementation(libs.foundation)
    screenshotTestImplementation(libs.material3)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.ui.tooling)
    screenshotTestImplementation(libs.jetbrains.navigation3.ui)
}

easylauncher {
    iconNames.addAll("@mipmap/ic_launcher", "@mipmap/ic_launcher_round")
    buildTypes {
        register("debug") {
            filters(chromeLike(label = flavorStr), greenRibbonFilter("debug"))
        }
        register("release") {
            filters(chromeLike(label = flavorStr))
        }
    }
}
