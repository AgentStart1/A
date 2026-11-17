/*
 * This is a private project. All rights reserved.
 */
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.HexFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("multiplatform") version "2.4.21" }
group = "space.kodio"
version = "0.2.0-jvm21"

abstract class VerifyKodioArchive : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val archive: RegularFileProperty
    @get:Input abstract val expectedSha256: Property<String>
    @TaskAction fun verify() {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(archive.get().asFile)).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val size = input.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        check(HexFormat.of().formatHex(digest.digest()) == expectedSha256.get()) { "Unexpected Kodio archive" }
    }
}
abstract class KodioArchiveOperations {
    @get:javax.inject.Inject abstract val archives: org.gradle.api.file.ArchiveOperations
}
val archives = objects.newInstance<KodioArchiveOperations>().archives
val upstreamSources = configurations.create("upstreamSources") {
    isCanBeConsumed = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
val upstreamRuntime = configurations.create("upstreamRuntime") {
    isCanBeConsumed = false
    resolutionStrategy.useGlobalDependencySubstitutionRules = false
}
dependencies {
    upstreamSources("space.kodio:core-jvm:0.2.0:sources@jar")
    upstreamRuntime("space.kodio:core-jvm:0.2.0@jar")
}
val verifySources = tasks.register<VerifyKodioArchive>("verifySources") {
    archive.set(layout.file(upstreamSources.elements.map { it.single().asFile }))
    expectedSha256.set("f609639f31fe61f719fb6ddf2410b463dbce47b88230f0e73019caa39d3ac765")
}
val verifyRuntime = tasks.register<VerifyKodioArchive>("verifyRuntime") {
    archive.set(layout.file(upstreamRuntime.elements.map { it.single().asFile }))
    expectedSha256.set("3246ddee77bd1ae507012cc25753d086497e5a4b2202e835212af4cbe910dd5e")
}
val prepareSources = tasks.register<Sync>("prepareSources") {
    val archiveOperations = archives
    dependsOn(verifySources)
    from(upstreamSources.elements.map { files -> files.map { archiveOperations.zipTree(it.asFile) } }) {
        include("commonMain/**", "jvmMain/**")
    }
    into(layout.buildDirectory.dir("upstream"))
}
val prepareResources = tasks.register<Sync>("prepareResources") {
    val archiveOperations = archives
    dependsOn(verifyRuntime)
    from(upstreamRuntime.elements.map { files -> files.map { archiveOperations.zipTree(it.asFile) } }) {
        include("space/kodio/core/natives/**")
    }
    from(listOf("NOTICE", "LICENSE")) { into("META-INF") }
    into(layout.buildDirectory.dir("runtime-resources"))
}
kotlin {
    jvmToolchain(21)
    jvm { compilerOptions.jvmTarget.set(JvmTarget.JVM_21) }
    sourceSets {
        commonMain {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("commonMain") })
            dependencies {
                api("com.ionspin.kotlin:bignum:0.3.10")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.9.0")
            }
        }
        jvmMain {
            kotlin.srcDir(prepareSources.map { it.destinationDir.resolve("jvmMain") })
            resources.srcDir(prepareResources)
        }
    }
}
