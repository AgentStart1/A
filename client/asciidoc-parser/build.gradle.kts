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
        compileSdk = libs.versions.android.compileSdk.get().toInt()
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
        jvmAndroidTest.resources.exclude("node_modules/**")
        wasmJsTest.get().dependsOn(headlessTest)
        headlessTest.dependencies {
        }
        androidMain.dependencies {
            implementation(libs.javet.node.android)
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.components.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)

                implementation(libs.javet.node.linux.arm64)
                implementation(libs.javet.node.linux.x86.x4)
                implementation(libs.javet.node.macos.arm64)
                implementation(libs.javet.node.macos.x86.x4)
                implementation(libs.javet.node.windows.x86.x4)
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

        jvmMain.dependencies {
            implementation(libs.javet)
            implementation(libs.javet.node.linux.arm64)
            implementation(libs.javet.node.linux.x86.x4)
            implementation(libs.javet.node.macos.arm64)
            implementation(libs.javet.node.macos.x86.x4)
            implementation(libs.javet.node.windows.x86.x4)
        }
        val jvmAndroidMain =
            create("jvmAndroidMain") {
                dependsOn(commonMain.get())
                dependencies {
                    compileOnly(libs.javet)
                }
            }
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

val parserTestFixture = layout.buildDirectory.dir("parser-test-fixture")
val copyParserTestFixture =
    tasks.register<Copy>("copyParserTestFixture") {
        from(layout.projectDirectory.dir("src/jvmAndroidTest/resources"))
        into(parserTestFixture)
        include("package.json", "package-lock.json", "parse-asciidoc.js")
    }
val npmTestCommand =
    providers.systemProperty("os.name").map { osName ->
        val npm = listOf("npm", "ci", "--no-audit", "--no-fund")
        if (osName.startsWith("Windows")) listOf("cmd", "/d", "/c") + npm else npm
    }
val prepareParserTestDependencies =
    tasks.register<Exec>("prepareParserTestDependencies") {
        dependsOn(copyParserTestFixture)
        workingDir(parserTestFixture)
        commandLine(npmTestCommand.get())
        inputs.files(
            parserTestFixture.map { it.file("package.json") },
            parserTestFixture.map { it.file("package-lock.json") },
        )
        outputs.dir(parserTestFixture.map { it.dir("node_modules") })
    }
tasks.withType<Test>().configureEach {
    dependsOn(prepareParserTestDependencies)
    inputs.dir(parserTestFixture)
    systemProperty("asciidoc.test.fixture", parserTestFixture.get().asFile.absolutePath)
}

// Robolectric runs on the host JVM and needs the JVM loader, not Android's
// preprocessed loader and its device-only native library loading behavior.
val hostJavetModule = libs.javet.asProvider().get().let { "${it.module}:${it.versionConstraint.requiredVersion}" }
configurations.matching { it.name == "androidHostTestRuntimeClasspath" }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.caoccao.javet:javet-node-android")).using(module(hostJavetModule))
    }
}
