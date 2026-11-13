import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
    alias(libs.plugins.sqlitenow)
}

val buildIosTarget = project.findProperty("target.ios") == "true"

kotlin {

    android {
        withHostTest {
            isIncludeAndroidResources = true
        }
        namespace = "com.storyteller_f.alphaheart.client.sqlitenow"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
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
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.robolectric)
            }
            dependsOn(headlessTest)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest {
            dependsOn(headlessTest)
        }
        commonMain.dependencies {
            implementation(libs.napier)
            implementation(projects.shared)
            implementation(projects.client.modelStorage)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.sqlitenow.core)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.androidx.paging.common)
        }
    }
}

sqliteNow {
    databases {
        create("AppDatabase") {
            packageName = "com.storyteller_f.alphaheart.client.sqlitenow.db"
            debug = false
        }
    }
}
