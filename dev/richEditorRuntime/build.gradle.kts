/*
 * This is a private project. All rights reserved.
 */

import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.FilterReader
import java.io.Reader
import java.io.StringReader
import java.security.MessageDigest
import java.util.HexFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

group = "com.mohamedrejeb.richeditor"
version = "1.2.1-lossless1"

abstract class VerifyRichEditorArchive : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: RegularFileProperty
    @get:Input abstract val expectedSha256: Property<String>

    @TaskAction
    fun verify() {
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
            "Unexpected Rich Editor source archive; review the Markdown compatibility patch before upgrading"
        }
    }
}

// Keep Markdown syntax that the upstream traversal otherwise drops at unhandled leaf nodes.
class LosslessRichEditorMarkdown(reader: Reader) : FilterReader(StringReader(
    reader.readText().let { source ->
        val anchor = "        MarkdownTokenTypes.TEXT -> onText(node.getTextInNode(markdown).toString())"
        check(source.indexOf(anchor) >= 0 && source.indexOf(anchor) == source.lastIndexOf(anchor)) {
            "Rich Editor Markdown traversal changed; review the lossless import patch"
        }
        source.replace(
            anchor,
            """        GFMTokenTypes.EQUALS,
        GFMElementTypes.INLINE_MATH,
        MarkdownTokenTypes.TEXT -> onText(node.getTextInNode(markdown).toString())""",
        )
    }
))

abstract class RichEditorArchiveOperations {
    @get:javax.inject.Inject abstract val archives: org.gradle.api.file.ArchiveOperations
}
val archives = objects.newInstance<RichEditorArchiveOperations>().archives
val archiveHashes = mapOf(
    "richeditor-compose" to "2dcf5372a0558664095e61b3299aa23c3429f08bb0345dc8ccfca0be46356adc",
    "richeditor-compose-android" to "016ebaabef8535fbb0b58425a0ce50489bdf24a726e5c032b08e7ac261e64c00",
    "richeditor-compose-desktop" to "80dfff0f530a161749b51329587c6d7b7f2e2cd93b13b19879b4225f8ace8cce",
    "richeditor-compose-wasm-js" to "b9c718589026c322471826d8709f6dac016c7383f7582affaef3e372f4be8fd9",
)
val preparedSources = archiveHashes.entries.mapIndexed { index, (artifact, hash) ->
    val sourceArchive = configurations.create("upstreamSources$index") {
        isCanBeConsumed = false
        resolutionStrategy.useGlobalDependencySubstitutionRules = false
    }
    dependencies { add(sourceArchive.name, "com.mohamedrejeb.richeditor:$artifact:1.2.1:sources@jar") }
    val verifySources = tasks.register<VerifyRichEditorArchive>("verifySources$index") {
        archive.set(layout.file(sourceArchive.elements.map { it.single().asFile }))
        expectedSha256.set(hash)
    }
    artifact to tasks.register<Sync>("prepareSources$index") {
        val archiveOperations = archives
        dependsOn(verifySources)
        inputs.property("markdownImportPolicy", "preserve-equals-and-inline-math-v2")
        from(sourceArchive.elements.map { files -> files.map { archiveOperations.zipTree(it.asFile) } }) {
            if (index == 0) {
                include("commonMain/**/*.kt", "iosMain/**/*.kt")
            } else {
                include(when (artifact) {
                    "richeditor-compose-android" -> "androidMain/**/*.kt"
                    "richeditor-compose-desktop" -> "desktopMain/**/*.kt"
                    else -> "wasmJsMain/**/*.kt"
                })
            }
            filesMatching("commonMain/**/MarkdownUtils.kt") {
                filter(LosslessRichEditorMarkdown::class.java)
            }
        }
        into(layout.buildDirectory.dir("upstream/$artifact"))
    }
}.toMap()

val upstreamNative = configurations.create("upstreamNative") {
    isCanBeConsumed = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
dependencies { upstreamNative("org.jetbrains.skiko:skiko-wasm-js:0.156.1:skiko-runtime@jar") }
val verifyNative = tasks.register<VerifyRichEditorArchive>("verifyNative") {
    archive.set(layout.file(upstreamNative.elements.map { it.single().asFile }))
    expectedSha256.set("a9578fa5a70c27b701d9b755acd771afe01f9a8eb6c8448a4e6680e285f748c9")
}
val unpackNative = tasks.register<Sync>("unpackNative") {
    val archiveOperations = archives
    dependsOn(verifyNative)
    from(upstreamNative.elements.map { files -> files.map { archiveOperations.zipTree(it.asFile) } })
    exclude("META-INF/**")
    into(layout.buildDirectory.dir("native-runtime"))
}

abstract class RichEditorWasmImportMetaRule @javax.inject.Inject constructor(name: String) : KotlinWebpackRule(name) {
    abstract val loaderPath: Property<String>

    init {
        enabled.convention(true)
        test.convention("/\\.import-object\\.mjs$/")
    }

    override fun loaders(): List<KotlinWebpackRule.Loader> =
        listOf(KotlinWebpackRule.Loader("require.resolve(${json(loaderPath.get())})", emptyMap(), emptyList()))
}

val browserConfig = layout.projectDirectory.dir("../../config/karma")
val importMetaLoader = layout.projectDirectory.file("../../config/webpack/wasm-node-import-meta.cjs")

kotlin {
    jvmToolchain(21)
    jvm("desktop") { compilerOptions.jvmTarget.set(JvmTarget.JVM_21) }
    android {
        namespace = "com.mohamedrejeb.richeditor"
        compileSdk {
            version = release(libs.versions.android.compileSdk.get().toInt()) {
                minorApiLevel = libs.versions.android.compileSdkMinor.get().toInt()
            }
        }
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        binaries.executable()
        browser {
            testTask {
                inputs.dir(browserConfig)
                inputs.file(importMetaLoader)
                useKarma {
                    useConfigDirectory(browserConfig.asFile)
                    useChromeHeadless()
                    webpackConfig.rules {
                        rule(RichEditorWasmImportMetaRule::class.java, "richEditorWasmImportMeta") {
                            loaderPath.set(importMetaLoader.asFile.invariantSeparatorsPath)
                        }
                    }
                }
            }
        }
    }
    if (providers.gradleProperty("target.ios").orNull == "true") {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
    sourceSets {
        commonMain {
            kotlin.srcDir(
                preparedSources.getValue("richeditor-compose").map { it.destinationDir.resolve("commonMain") },
            )
            dependencies {
                implementation(libs.runtime)
                implementation(libs.foundation)
                implementation(libs.material)
                implementation(libs.material3)
                implementation("com.mohamedrejeb.ksoup:ksoup-html:0.6.0")
                implementation("com.mohamedrejeb.ksoup:ksoup-entities:0.6.0")
                implementation(libs.markdown)
            }
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("desktopMain") {
            kotlin.srcDir(
                preparedSources.getValue("richeditor-compose-desktop").map { it.destinationDir.resolve("desktopMain") },
            )
        }
        androidMain {
            kotlin.srcDir(
                preparedSources.getValue("richeditor-compose-android").map { it.destinationDir.resolve("androidMain") },
            )
        }
        wasmJsMain {
            kotlin.srcDir(
                preparedSources.getValue("richeditor-compose-wasm-js").map { it.destinationDir.resolve("wasmJsMain") },
            )
        }
        if (providers.gradleProperty("target.ios").orNull == "true") {
            getByName("iosMain") {
                kotlin.srcDir(
                preparedSources.getValue("richeditor-compose").map { it.destinationDir.resolve("iosMain") },
            )
            }
        }
    }
}

// The Wasm test import object references Skiko next to its generated entry module.
// Stage the exact verified runtime into each Wasm binary's package before webpack bundles it.
kotlin.targets.withType<KotlinJsIrTarget>().configureEach {
    compilations.configureEach {
        binaries.configureEach {
            linkSyncTask.configure {
                dependsOn(unpackNative)
                from.from(unpackNative.map { it.destinationDir })
            }
        }
    }
}
