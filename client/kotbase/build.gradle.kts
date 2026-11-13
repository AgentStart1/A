@file:Suppress("UnstableApiUsage")

import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}

val buildIosTarget = project.findProperty("target.ios") == "true"
kotlin {
    android {
        namespace = "com.storyteller_f.alphaheart.client.kotbase"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest { }
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        }
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
        val headlessTest = create("headlessTest") {
            dependsOn(commonTest.get())
        }
        headlessTest.dependencies {
            implementation(projects.client.kotbase)
        }
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(projects.client.modelStorage)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.napier)
            implementation(libs.couchbase.lite)
            implementation(libs.couchbase.lite.ktx)
            implementation(libs.couchbase.lite.paging)
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
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest {
            dependsOn(headlessTest)
        }
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes")
    }
}

dependencies {
    androidRuntimeClasspath(libs.androidx.ui.test.manifest)
}

interface Injected {
    @get:Inject
    val operations: ArchiveOperations
}

val extractCouchbaseLibsTask = tasks.register("extractCouchbaseNativeLib") {
    group = "libs"
    val outputDir = layout.buildDirectory.dir("native-libs/couchbase")
    val runtimeClasspath = configurations.named("jvmRuntimeClasspath").get().files.toList()
    val injected = project.objects.newInstance<Injected>()

    inputs.files(runtimeClasspath)
    outputs.dir(outputDir)

    val osName = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch")
    doLast {
        val output = outputDir.get().asFile
        if (!output.exists() && !output.mkdirs()) {
            throw Exception("mkdirs failed $output")
        }
        runtimeClasspath.forEach { jarFile ->
            if (jarFile.path.contains("couchbase-lite-java")) {
                injected.operations.zipTree(jarFile).forEach { file ->
                    val match = if (osName.contains("mac")) {
                        file.path.contains("macos") && (file.path.contains("universal") || file.path.contains(arch))
                    } else if (osName.contains("win")) {
                        file.path.contains("windows")
                    } else if (osName.contains("nux")) {
                        file.path.contains("linux")
                    } else {
                        false
                    }
                    if (match) {
                        file.copyTo(File(output, file.name), true)
                    }
                }
            }
        }
    }
}

afterEvaluate {
    tasks.getByName("testAndroid").dependsOn(extractCouchbaseLibsTask)
    listOf(
        ":api:jvmJar",
        ":shared:jvmJar",
        ":backend:core:jar",
        ":backend:elastic:jar",
        ":backend:exposed:jar",
        ":backend:filesystem:jar",
        ":backend:lucene:jar",
        ":backend:minio:jar",
        ":backend:redis:jar",
        ":backend:simple:jar",
        ":cloud:pdf:jar",
        ":cloud:openpdf:jar",
        ":cloud:service:jar",
        ":client:model-storage:jvmJar",
    ).forEach { path ->
        extractCouchbaseLibsTask.dependsOn(path)
    }
}

tasks.withType<Test>().configureEach {
    val dir = project.layout.buildDirectory.dir("native-libs/couchbase").get().asFile
    jvmArgs("-Djava.library.path=$dir")
}
