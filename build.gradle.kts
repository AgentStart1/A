import dev.detekt.gradle.Detekt
import dev.detekt.gradle.report.ReportMergeTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    id("e2e-jvm-coverage") apply false
    id("e2e-android-coverage") apply false
    id("e2e-wasm-coverage") apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.aboutlibrary)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlinxRpc) apply false
    alias(libs.plugins.sentryAndroidGradle) apply false
}

plugins.withType<YarnPlugin> {
    extensions.configure<YarnRootExtension> {
        lockFileDirectoryProperty.set(layout.projectDirectory.dir("kotlin-js-store"))
        lockFileNameProperty.set("yarn.lock")
    }
}

plugins.withType<WasmYarnPlugin> {
    extensions.configure<WasmYarnRootExtension> {
        lockFileDirectoryProperty.set(layout.projectDirectory.dir("kotlin-js-store/wasm"))
        lockFileNameProperty.set("yarn.lock")
    }
}

val wasmMediaHostSources = layout.projectDirectory.dir("client/composeCore/wasm-media-host").asFileTree.matching {
    include("host.mjs", "decode-worker.mjs", "gpu-bridge.mjs", "video-sample.mjs", "package.json", "NOTICE")
}

val downloadWorkerSources = layout.projectDirectory.dir("app/composeApp/file-worker").asFileTree.matching {
    include("*.mjs", "test/*.mjs", "package.json")
    exclude("*.test.mjs")
}

// Yarn file dependencies retain stale source copies unless installation is forced.
tasks.withType<KotlinNpmInstallTask>().matching { it.name == "kotlinWasmNpmInstall" }.configureEach {
    inputs.files(wasmMediaHostSources).withPropertyName("wasmMediaHostSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(downloadWorkerSources).withPropertyName("downloadWorkerSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    args.add("--force")
}

val wasmMediaHostConsumers = setOf(
    ":client:composeCore", ":app:composeApp", ":app:webApp", ":panel:composeApp", ":panel:webApp",
    ":dev:wasmMediaExperiment",
)

allprojects {
    tasks.withType<KotlinWebpack>().configureEach {
        if (project.path in setOf(":app:composeApp", ":app:webApp") && name.startsWith("wasmJs")) {
            inputs.files(downloadWorkerSources).withPropertyName("downloadWorkerSources")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
        if (project.path in wasmMediaHostConsumers && name.startsWith("wasmJs")) {
            inputs.files(wasmMediaHostSources).withPropertyName("wasmMediaHostSources")
                .withPathSensitivity(PathSensitivity.RELATIVE)
        }
    }
}

fun isNoReleaseCompileTask(taskName: String): Boolean {
    val isKsp = taskName.startsWith("ksp")
    val isKotlinOrJavaCompile = taskName.startsWith("compile") && listOf(
        "Kotlin",
        "Java",
        "JavaWithJavac",
        "KotlinJvm",
        "KotlinMetadata",
        "KotlinWasmJs",
        "Main",
    ).any { suffix ->
        taskName.endsWith(suffix)
    }
    val isExcludedVariant = listOf("Release", "Benchmark", "Test", "Jmh", "Executable").any { variant ->
        taskName.contains(variant)
    }
    return (isKsp || isKotlinOrJavaCompile) && !isExcludedVariant
}

val compileAllNoRelease = tasks.register("compileAllNoRelease") {
    group = "verification"
    description = "Compile all included modules without executable, release, or benchmark variants."
}

subprojects {
    compileAllNoRelease.configure {
        // Filter names before realization: excluded AGP Preview tasks may require absent unit-test variants.
        dependsOn(tasks.names.filter(::isNoReleaseCompileTask).map(tasks::named))
    }
}

val detektReportMergeSarif = tasks.register<ReportMergeTask>("detektReportMergeSarif") {
    output = layout.buildDirectory.file("reports/detekt/merge.sarif")
}
subprojects {
    apply(plugin = "dev.detekt")
    detekt {
        // Include every Kotlin source set so module baselines also cover KMP-only targets.
        source.setFrom("src", "build.gradle.kts")
        // Builds the AST in parallel. Rules are always executed in parallel.
        // Can lead to speedups in larger projects. `false` by default.
        parallel = true

        autoCorrect = false

        // Android: Don't create tasks for the specified build types (e.g. "release")
        ignoredBuildTypes = listOf("release")

        // Specify the base path for file paths in the formatted reports.
        // If not set, all file paths reported will be absolute file path.
        basePath = layout.projectDirectory

        buildUponDefaultConfig = true
    }

    dependencies {
        detektPlugins(rootProject.libs.detekt.formatting)
        detektPlugins(rootProject.libs.detekt.rules.libraries)
        detektPlugins(rootProject.libs.detekt.rules.ruleauthors)
    }

    tasks.withType<Detekt>().configureEach {
        exclude { source -> source.file.absolutePath.replace('\\', '/').contains("/build/") }
        reports {
            checkstyle.required = true
            html.required = true
            sarif.required = true
            markdown.required = true
        }
        basePath = rootDir.absolutePath
        finalizedBy(detektReportMergeSarif)
    }

    detektReportMergeSarif {
        input.from(
            tasks.withType<Detekt>().map { it.reports.sarif.outputLocation })
    }
}
val koverIncludedProjects = listOf(
    ":result",
    ":cloud:runtime",
    ":cloud:ws-api",
    ":cloud:ws",
    ":cloud:filesystem-service",
    ":cloud:lucene-service",
    ":services:filesystem-protocol",
    ":services:lucene-protocol",
    ":shared",
    ":app:composeApp",
    ":app:cliApp",
    ":client:composeCore",
    ":app:androidApp",
    ":app:desktopApp",
    ":cloud:service",
    ":cloud:server",
    ":cloud:cli",
    ":cloud:worker",
    ":cloud:pdf",
    // ":cloud:pdfbox", // Disabled - pdfbox-layout requires JitPack
    ":cloud:openpdf",
    ":api",
    ":panel:composeApp",
    ":panel:androidApp",
    ":panel:cliApp",
    ":panel:desktopApp",
    ":backend:core",
    ":backend:exposed",
    ":backend:simple",
    ":backend:redis",
    ":backend:minio",
    ":backend:lucene",
    ":backend:filesystem",
    ":backend:elastic",
    ":client:core",
    ":client:bot-lib",
    ":client:model-storage",
    ":client:room",
    ":client:asciidoc-parser",
    ":bot:builtin-bot",
).distinct()
dependencies {
    koverIncludedProjects.forEach { projectPath ->
        kover(project(projectPath))
    }
}

subprojects {
    if (path in koverIncludedProjects) {
        apply(plugin = "org.jetbrains.kotlinx.kover")
        kover {
            reports {
                // filters for all report types of all build variants
                filters {
                    excludes {
                        androidGeneratedClasses()
                    }
                }
            }
        }
    }
}

val e2eCoverageEnabled = providers.gradleProperty("e2e.coverage").map(String::toBooleanStrict).orElse(false)
val e2eJvmTargets = setOf(
    ":app:desktopApp", ":panel:desktopApp", ":app:cliApp", ":panel:cliApp",
    ":cloud:server", ":cloud:worker", ":cloud:cli", ":cloud:ws",
    ":cloud:filesystem-service", ":cloud:lucene-service",
)
if (e2eCoverageEnabled.get()) {
    subprojects {
        if (path in e2eJvmTargets) {
            plugins.withId("java") { apply(plugin = "e2e-jvm-coverage") }
        }
        if (path in setOf(":app:androidApp", ":panel:androidApp")) {
            plugins.withId("com.android.application") { apply(plugin = "e2e-android-coverage") }
        }
        if (path in setOf(":app:wasmAppium", ":panel:wasmAppium")) {
            plugins.withId("java") { apply(plugin = "e2e-wasm-coverage") }
        }
        tasks.withType<Test>().matching { it.name in setOf("appiumTest", "e2eTest") }.configureEach {
            systemProperty("e2e.coverage", true)
            outputs.upToDateWhen { false }
        }
    }
}

val wasmCoverageExecutableSuffix = providers.systemProperty("os.name").map { if (it.startsWith("Windows")) ".exe" else "" }
tasks.register<Exec>("buildWasmCoverageTool") {
    group = "verification"
    inputs.files(fileTree("dev/wasmCoverage") { include("src/**", "Cargo.toml", "Cargo.lock") })
    outputs.file(layout.projectDirectory.file("dev/wasmCoverage/build/target/debug/a-wasm-coverage${wasmCoverageExecutableSuffix.get()}"))
    workingDir(layout.projectDirectory)
    commandLine("cargo", "build", "--locked", "--manifest-path", "dev/wasmCoverage/Cargo.toml",
        "--target-dir", "dev/wasmCoverage/build/target")
}

val unitTestForks = providers.gradleProperty("unitTest.maxParallelForks").map {
    it.toInt().also { forks -> require(forks > 0) { "unitTest.maxParallelForks must be positive" } }
}.orElse(gradle.startParameter.maxWorkerCount)
val parallelHostTestProjects = setOf(
    ":api", ":shared", ":client:core", ":client:composeCore", ":client:asciidoc-parser", ":backend:redis",
    ":app:composeApp", ":panel:composeApp", ":cloud:service", ":services:lucene-protocol",
    ":cloud:filesystem-service", ":cloud:lucene-service", ":dev:appiumCore",
    ":backend:core", ":backend:filesystem", ":backend:lucene", ":backend:minio",
    ":cloud:runtime", ":cloud:server", ":cloud:cli", ":cloud:worker",
)
subprojects {
    if (path in parallelHostTestProjects) {
        tasks.withType<Test>().configureEach {
            if (name == "test" || name == "jvmTest" || name == "testAndroidHostTest" ||
                name.endsWith("UnitTest")
            ) {
                maxParallelForks = unitTestForks.get()
            }
        }
    }
}

// Complete every selected runner's immutable application artifacts before starting any live fixture.
if (e2eCoverageEnabled.get()) {
    val runners = listOf("app", "panel").flatMap { target ->
        listOf("androidAppium", "desktopAppium", "wasmAppium", "cliE2e").map { ":$target:$it" }
    } + ":app:linuxAppium"
    val requested = gradle.startParameter.taskNames
    val explicit = runners.filter { runner -> requested.any { it.startsWith("$runner:") } }
    val selected = explicit.ifEmpty { runners }
    val sources = fileTree(rootDir) {
        include("**/src/main/**/*.kt", "**/src/main/**/*.java", "**/src/*Main/**/*.kt", "**/src/*Main/**/*.java")
        exclude(".worktrees/**", "**/build/**", "dev/e2eCoverageRuntime/**")
    }
    val report = tasks.register<E2eCoverageReportTask>("e2eCoverageReport") {
        group = "verification"
        description = "Render completed JVM, Android and mapped Wasm E2E coverage artifacts."
        mustRunAfter("koverXmlReport", "koverHtmlReport")
        repositoryDirectory.set(layout.projectDirectory)
        sourceFiles.from(sources)
        reportDirectory.set(layout.buildDirectory.dir("reports/e2eCoverage"))
        scratchDirectory.set(layout.buildDirectory.dir("e2e-coverage/merged-originals"))
        selected.forEach { runner ->
            val testName = if (runner.endsWith("cliE2e")) "e2eTest" else "appiumTest"
            rawArtifacts.from(fileTree("${runner.trim(':').replace(':', '/')}/build/test-results/$testName/artifacts") {
                include("**/coverage/**")
                exclude("**/coverage/control/**", "**/report.pending.ic")
            })
        }
        (e2eJvmTargets + setOf(":app:androidApp", ":panel:androidApp")).forEach { target ->
            originalClassFiles.from(fileTree("${target.trim(':').replace(':', '/')}/build/e2e-coverage/originals") {
                include("**/*.class", "**/classes.properties")
            })
        }
        mustRunAfter(runners.map { "$it:${if (it.endsWith("cliE2e")) "e2eTest" else "appiumTest"}" })
    }
    val combined = tasks.register<CombinedCoverageReportTask>("coverageReport") {
        group = "verification"
        description = "Union ordinary unit-test and completed E2E source-line coverage, preserving independent reports."
        dependsOn("koverXmlReport", "koverHtmlReport", report)
        unitXml.set(layout.buildDirectory.file("reports/kover/report.xml"))
        e2eReports.set(report.flatMap { it.reportDirectory })
        sourceFiles.from(sources)
        repositoryDirectory.set(layout.projectDirectory)
        reportDirectory.set(layout.buildDirectory.dir("reports/coverage"))
    }
    val prepared = tasks.register("prepareE2eCoverageArtifacts") {
        // Finish host tests and any compilation they require before immutable UI artifacts are launched.
        dependsOn("koverXmlReport", "koverHtmlReport")
        selected.forEach { runner ->
            val target = runner.substringBeforeLast(':')
            when {
                runner.endsWith("androidAppium") -> dependsOn("$target:androidApp:installDebug", "$runner:appiumTestClasses")
                runner.endsWith("desktopAppium") || runner.endsWith("linuxAppium") ->
                    dependsOn("$target:desktopApp:writeAppiumRuntimeClasspath", "$runner:appiumTestClasses")
                runner.endsWith("wasmAppium") -> dependsOn("$runner:prepareE2eCoverageWasmDistribution", "$runner:appiumTestClasses")
                else -> dependsOn("$target:cliApp:prepareE2eCoverageDistribution", "$runner:e2eTestClasses")
            }
        }
        e2eJvmTargets.filter { it.startsWith(":cloud:") }.forEach { dependsOn("$it:buildTestDockerImage") }
    }
    subprojects {
        if (path in selected) {
            configurations.matching { it.name in setOf("appiumTestRuntimeClasspath", "e2eTestRuntimeClasspath") }.configureEach {
                val runtimeConfiguration = this
                prepared.configure { dependsOn(runtimeConfiguration) }
            }
            tasks.withType<Test>().matching { it.name in setOf("appiumTest", "e2eTest") }.configureEach {
                dependsOn(prepared)
                finalizedBy(report, combined)
            }
        }
    }
}
