/*
 * This is a private project. All rights reserved.
 */

import java.io.BufferedInputStream
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.HexFormat
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    kotlin("multiplatform") version "2.4.21"
    kotlin("jvm") version "2.4.21" apply false
}
group = "org.jetbrains.skiko"
version = "0.150.2-media1"

val upstreamSources = configurations.create("upstreamSources") {
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
val upstreamRuntime = configurations.create("upstreamRuntime") {
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
val upstreamVersion = "0.150.2"
dependencies {
    upstreamSources("org.jetbrains.skiko:skiko-wasm-js:$upstreamVersion:sources@jar")
    upstreamRuntime("org.jetbrains.skiko:skiko-js-wasm-runtime:$upstreamVersion")
}
abstract class VerifySkikoArchive : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: RegularFileProperty
    @get:Input
    abstract val expectedSha256: Property<String>
    @TaskAction fun verify() {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(archive.get().asFile)).use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        check(HexFormat.of().formatHex(digest.digest()) == expectedSha256.get()) {
            "Unexpected Skiko archive"
        }
    }
}
val verifySourceArchive = tasks.register<VerifySkikoArchive>("verifySourceArchive") {
    archive.set(layout.file(upstreamSources.elements.map { it.single().asFile }))
    expectedSha256.set("a68a303b1638005b4a9094bd171c69c695816646bc0b9a50616b3d4bac54a174")
}
val verifyRuntimeArchive = tasks.register<VerifySkikoArchive>("verifyRuntimeArchive") {
    archive.set(layout.file(upstreamRuntime.elements.map { it.single().asFile }))
    expectedSha256.set("811eaada148b986be198ad5d966f239ce0b3fbdd05994c1efbd0aa5f8074ff79")
}

val stagedRuntimeArchive = layout.buildDirectory.file("native-runtime/skiko-runtime.jar")
val stageRuntimeArchive = tasks.register<Sync>("stageRuntimeArchive") {
    dependsOn(verifyRuntimeArchive)
    from(upstreamRuntime)
    rename { "skiko-runtime.jar" }
    into(stagedRuntimeArchive.map { it.asFile.parentFile })
}

// Preserve the published native-runtime variant when Kotlin sources are substituted by this build.
configurations.create("skikoWasmRuntimeElementsForWasmJs") {
    isCanBeResolved = false
    isCanBeConsumed = true
    attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Usage.USAGE_ATTRIBUTE, objects.named("skiko-runtime"))
        attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named("non-jvm"))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.wasm)
        attribute(KotlinWasmTargetAttribute.wasmTargetAttribute, KotlinWasmTargetAttribute.js)
    }
    outgoing.artifact(stagedRuntimeArchive) {
        builtBy(stageRuntimeArchive)
    }
}
val sourceArchives = upstreamSources.elements
val archiveOperations = objects.newInstance<ArchiveOperationsHolder>()

abstract class ArchiveOperationsHolder {
    @get:javax.inject.Inject abstract val archives: org.gradle.api.file.ArchiveOperations
}
val prepareSources = tasks.register<Sync>("prepareSources") {
    dependsOn(verifySourceArchive)
    val unpack = archiveOperations.archives
    from(sourceArchives.map { files -> files.map { unpack.zipTree(it.asFile) } }) {
        exclude(
            "webMain/org/jetbrains/skiko/CanvasRenderer.kt",
            "webMain/org/jetbrains/skiko/SkiaLayer.web.kt",
            "webMain/org/jetbrains/skiko/GLTexture.web.kt",
        )
    }
    from("patches") { into("webMain/org/jetbrains/skiko") }
    into(layout.buildDirectory.dir("upstream"))
}
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("nativeJs") {
                group("web") {
                    withWasmJs()
                }
            }
        }
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("commonMain") })
            dependencies { api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0") }
        }
        named("nativeJsMain") {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("nativeJsMain") })
        }
        named("webMain") {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("webMain") })
            dependencies { api("org.jetbrains.kotlinx:kotlinx-browser:0.5.0") }
        }
        wasmJsMain {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("wasmJsMain") })
        }
    }
}
val generatedImports = layout.buildDirectory.file("imports/skiko-imports.mjs").get().asFile
val importPlugin = project(":import-generator").layout.buildDirectory.file("libs/import-generator.jar").get().asFile
tasks.named<Kotlin2JsCompile>("compileKotlinWasmJs") {
    dependsOn(":import-generator:jar")
    inputs.file(importPlugin)
    compilerOptions.optIn.addAll("org.jetbrains.skiko.InternalSkikoApi", "org.jetbrains.skiko.ExperimentalSkikoApi", "kotlin.js.ExperimentalWasmJsInterop")
    compilerOptions.freeCompilerArgs.addAll(
        "-Xplugin=${importPlugin.absolutePath}",
        "-P", "plugin:org.jetbrains.skiko.imports.generator:import-generator-path=${generatedImports.absolutePath}",
        "-P", "plugin:org.jetbrains.skiko.imports.generator:import-generator-module-name=skiko",
    )
    outputs.file(generatedImports)
}
