/*
 * This is a private project. All rights reserved.
 */
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.HexFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackRule

plugins {
    kotlin("multiplatform") version "2.4.21"
    kotlin("plugin.compose") version "2.4.21"
}
group = "org.jetbrains.compose.ui"
version = "1.13.0-beta02-input1"
val upstreamSources = configurations.create("upstreamSources") {
    isCanBeConsumed = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
dependencies { upstreamSources("org.jetbrains.compose.ui:ui-wasm-js:1.13.0-beta02:sources@jar") }
abstract class VerifyComposeArchive : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: RegularFileProperty
    @get:Input abstract val expectedSha256: Property<String>
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
            "Unexpected Compose UI source archive; review the Web input patch before upgrading"
        }
    }
}
abstract class ComposeArchiveOperations {
    @get:javax.inject.Inject abstract val archives: org.gradle.api.file.ArchiveOperations
}
val verifySources = tasks.register<VerifyComposeArchive>("verifySources") {
    archive.set(layout.file(upstreamSources.elements.map { it.single().asFile }))
    expectedSha256.set("ceafdd1ec92d02ca112b38d6eee39b510fe3f2abb7a65cbd6a57db97f262ca19")
}
val archives = objects.newInstance<ComposeArchiveOperations>().archives
val upstreamNative = configurations.create("upstreamNative") {
    isCanBeConsumed = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
dependencies { upstreamNative("org.jetbrains.skiko:skiko-wasm-js:0.156.1:skiko-runtime@jar") }
val verifyNative = tasks.register<VerifyComposeArchive>("verifyNative") {
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
abstract class WasmImportMetaRule @javax.inject.Inject constructor(name: String) : KotlinWebpackRule(name) {
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
val prepareSources = tasks.register<Sync>("prepareSources") {
    val archiveOperations = archives
    dependsOn(verifySources)
    from(upstreamSources.elements.map { files -> files.map { archiveOperations.zipTree(it.asFile) } }) {
        exclude(
            "webMain/androidx/compose/ui/platform/DomInputStrategy.kt",
            "webMain/androidx/compose/ui/platform/NativeInputEventsProcessor.kt",
            "webMain/androidx/compose/ui/platform/BackingDomInput.kt",
            "webMain/androidx/compose/ui/platform/WebTextInputService.kt",
            "skikoMain/androidx/compose/ui/platform/Synchronization.skiko.kt",
            "wasmJsMain/androidx/compose/ui/platform/PlatformClipboard.wasm.kt",
        )
    }
    from("patches") { into("webMain/androidx/compose/ui/platform") }
    from("compat")
    into(layout.buildDirectory.dir("upstream"))
}
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("compose-multiplatform-core-compose-ui-ui")
        browser {
            testTask {
                inputs.dir(browserConfig)
                inputs.file(importMetaLoader)
                useKarma {
                    useChromeHeadless()
                    useConfigDirectory(browserConfig.asFile)
                    webpackConfig.rules {
                        rule(WasmImportMetaRule::class.java, "wasmImportMeta") {
                            loaderPath.set(importMetaLoader.asFile.invariantSeparatorsPath)
                        }
                    }
                }
            }
        }
    }
    sourceSets {
        commonMain { kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("commonMain") }) }
        val skikoMain = create("skikoMain") {
            dependsOn(commonMain.get())
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("skikoMain") })
        }
        val nonJvmMain = create("nonJvmMain") {
            dependsOn(skikoMain)
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("nonJvmMain") })
        }
        val webMain = create("webMain") {
            dependsOn(nonJvmMain)
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("webMain") })
        }
        wasmJsMain {
            dependsOn(webMain)
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("wasmJsMain") })
            dependencies {
                api("androidx.annotation:annotation-wasm-js:1.9.1")
                api("androidx.collection:collection-wasm-js:1.6.0")
                api("androidx.compose.runtime:runtime-retain-wasm-js:1.13.0-beta02")
                api("androidx.lifecycle:lifecycle-viewmodel-compose-wasm-js:2.11.0")
                api("androidx.lifecycle:lifecycle-viewmodel-savedstate-wasm-js:2.11.0")
                api("androidx.savedstate:savedstate-compose-wasm-js:1.4.0")
                api("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose-wasm-js:2.9.6")
                api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate-wasm-js:2.9.6")
                api("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-wasm-js:2.9.6")
                api("org.jetbrains.androidx.navigationevent:navigationevent-compose-wasm-js:1.1.0")
                api("org.jetbrains.compose.annotation-internal:annotation-wasm-js:1.10.0")
                api("org.jetbrains.compose.collection-internal:collection-wasm-js:1.10.0")
                api("org.jetbrains.compose.runtime:runtime-saveable-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.runtime:runtime-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-backhandler-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-geometry-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-graphics-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-skiko-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-text-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-unit-wasm-js:1.13.0-beta02")
                api("org.jetbrains.compose.ui:ui-util-wasm-js:1.13.0-beta02")
                api("org.jetbrains.kotlinx:atomicfu-wasm-js:0.28.0")
                api("org.jetbrains.kotlinx:kotlinx-browser-wasm-js:0.5.0")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:1.9.0")
                api("org.jetbrains.skiko:skiko-wasm-js:0.156.1")
            }
        }
        wasmJsTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
            }
        }
    }
    compilerOptions.optIn.addAll(
        "androidx.compose.ui.ExperimentalComposeUiApi",
        "androidx.compose.ui.InternalComposeUiApi",
        "androidx.compose.runtime.InternalComposeApi",
        "androidx.compose.runtime.ExperimentalComposeApi",
        "androidx.compose.ui.text.ExperimentalTextApi",
        "kotlin.contracts.ExperimentalContracts",
        "kotlin.ExperimentalStdlibApi",
        "kotlin.js.ExperimentalWasmJsInterop",
        "org.jetbrains.skiko.ExperimentalSkikoApi",
        "org.jetbrains.skiko.InternalSkikoApi",
    )
}
tasks.named<Kotlin2JsCompile>("compileKotlinWasmJs") {
    compilerOptions.moduleName.set("compose-multiplatform-core-compose-ui-ui")
    compilerOptions.freeCompilerArgs.addAll(
        "-Xir-module-name=org.jetbrains.compose.ui:ui",
        "-XXLanguage:+AllowAnyAsAnActualTypeForExpectInterface",
        "-XXLanguage:+JsAllowImplementingFunctionInterface",
        "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
    )
}

// KGP resource discovery does not include composite components. Stage the exact
// verified native runtime through its public binary link-sync input contract.
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
