/*
 * This is a private project. All rights reserved.
 */
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.HexFormat
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins { kotlin("multiplatform") version "2.4.21" }
group = "space.kodio"
version = "0.2.0-web-sample-rate"

abstract class VerifyKodioSources : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: RegularFileProperty
    @TaskAction fun verify() {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(archive.get().asFile)).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        check(HexFormat.of().formatHex(digest.digest()) ==
            "63f93f252fcf9e3b2dbd101b1449f6c4db4728abfc6b303ba48706de4421cbd5") {
            "Unexpected Kodio Web sources"
        }
    }
}
abstract class KodioArchives {
    @get:javax.inject.Inject abstract val archives: org.gradle.api.file.ArchiveOperations
}
val archives = objects.newInstance<KodioArchives>().archives
val upstreamSources = configurations.create("upstreamSources") {
    isCanBeConsumed = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
dependencies { upstreamSources("space.kodio:core-wasm-js:0.2.0:sources@jar") }
val verifySources = tasks.register<VerifyKodioSources>("verifySources") {
    archive.set(layout.file(upstreamSources.elements.map { it.single().asFile }))
}
val prepareSources = tasks.register<Sync>("prepareSources") {
    val archiveOperations = archives
    dependsOn(verifySources)
    from(upstreamSources.elements.map { files -> files.map { archiveOperations.zipTree(it.asFile) } }) {
        include("commonMain/**", "webMain/**", "wasmJsMain/**")
        filesMatching("webMain/space/kodio/core/WebAudioRecordingSession.kt") {
            filter { line: String ->
                line.replace(
                    "val actualSampleRate = settings.sampleRate ?: format.sampleRate",
                    "val actualSampleRate = settings.sampleRate?.takeIf { it > 0 } ?: format.sampleRate " +
                        "// Alpha Heart compatibility: tolerate omitted browser sample rates.",
                )
            }
        }
    }
    into(layout.buildDirectory.dir("upstream"))
}
val prepareResources = tasks.register<Sync>("prepareResources") {
    from(listOf("NOTICE", "LICENSE")) { into("META-INF") }
    into(layout.buildDirectory.dir("license-resources"))
}
kotlin {
    jvmToolchain(21)
    wasmJs { browser() }
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("commonMain") })
            dependencies {
                api("com.ionspin.kotlin:bignum:0.3.10")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.9.0")
            }
        }
        val webMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("webMain") })
            dependencies { api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.4.1") }
        }
        wasmJsMain {
            dependsOn(webMain)
            resources.srcDir(prepareResources.map { it.destinationDir })
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("wasmJsMain") })
        }
    }
}

tasks.named<Kotlin2JsCompile>("compileKotlinWasmJs") {
    compilerOptions.moduleName.set("kodio-kodio-core")
    compilerOptions.freeCompilerArgs.addAll(
        "-Xir-module-name=space.kodio:kodio-core",
        "-XXLanguage:+AllowAnyAsAnActualTypeForExpectInterface",
        "-XXLanguage:+JsAllowImplementingFunctionInterface",
    )
}
