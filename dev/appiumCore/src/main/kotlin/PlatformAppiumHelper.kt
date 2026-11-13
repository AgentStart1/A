/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.E2eCoverage
import com.storyteller_f.alphaheart.test.useBackendFixture
import io.appium.java_client.AppiumDriver
import io.appium.java_client.android.AndroidDriver
import io.appium.java_client.android.options.UiAutomator2Options
import java.io.File
import java.net.URI
import java.util.Base64

data class AppiumTestSetup<T>(val data: T, val injectedSession: InjectedSession? = null)

interface AppiumTestScope {
    val driver: AppTestDriver

    suspend fun assertAsciidocPreviewOpened(source: String)

    /** Restarts the target process while retaining its isolated persistent storage. */
    suspend fun restart()
}

interface PlatformAppiumHelper {
    val capturesExternalAsciidocPreview: Boolean

    suspend fun <T> runTest(
        testName: String,
        target: TargetAppiumHelper,
        captureBrowserOpen: Boolean = false,
        setup: suspend (AppiumPorts) -> AppiumTestSetup<T>,
        block: suspend (AppiumTestScope, T) -> Unit,
    )
}

class AndroidAppiumHelper : PlatformAppiumHelper {
    override val capturesExternalAsciidocPreview = false

    /** Blocks only protected preferences with real filesystem errors, restoring the original files. */
    suspend fun <T> withProtectedWritesBlocked(packageName: String, block: suspend () -> T): T {
        require(packageName.matches(Regex("[a-zA-Z0-9_.]+")))
        val command = listOf("shell", "run-as", packageName)
        runAdbCommand(command + listOf("mkdir", "-p", "shared_prefs", "files"))
        val undo = mutableListOf<() -> Unit>()
        return AutoCloseable { restoreProtectedPreferences(undo) }.use {
            val backup = "files/appium-protected-preferences"
            runAdbCommand(command + listOf("mkdir", backup))
            undo.add { runAdbCommand(command + listOf("rmdir", backup)) }
            listOf("protected-data.xml", "protected-data.xml.bak").forEach { name ->
                val path = "shared_prefs/$name"
                if (runAdbCommandAllowFailure(command + listOf("test", "-e", path)).exitCode == 0) {
                    runAdbCommand(command + listOf("mv", path, "$backup/$name"))
                    undo.add { runAdbCommand(command + listOf("mv", "$backup/$name", path)) }
                }
                runAdbCommand(command + listOf("mkdir", path))
                undo.add {
                    runAdbCommand(command + listOf("rm", "-f", "$path/blocked"))
                    runAdbCommand(command + listOf("rmdir", path))
                }
                runAdbCommand(command + listOf("touch", "$path/blocked"))
            }
            block()
        }
    }

    private fun restoreProtectedPreferences(undo: List<() -> Unit>) {
        var failure: Exception? = null
        undo.asReversed().forEach { restore ->
            try {
                restore()
            } catch (restoreFailure: Exception) {
                if (failure == null) failure = restoreFailure else failure.addSuppressed(restoreFailure)
            }
        }
        failure?.let { throw it }
    }

    fun protectedPreferencesDigest(packageName: String): String {
        require(packageName.matches(Regex("[a-zA-Z0-9_.]+")))
        return runAdbCommand(
            listOf(
                "shell",
                "run-as",
                packageName,
                "sha256sum",
                "shared_prefs/protected-data.xml",
            ),
        ).substringBefore(' ')
    }

    override suspend fun <T> runTest(
        testName: String,
        target: TargetAppiumHelper,
        captureBrowserOpen: Boolean,
        setup: suspend (AppiumPorts) -> AppiumTestSetup<T>,
        block: suspend (AppiumTestScope, T) -> Unit,
    ) {
        val report = AppiumTestReport(target.suiteName, "android", testName)
        report.run {
            runAndroidTest(target, setup, block, report)
        }
    }

    private suspend fun <T> runAndroidTest(
        target: TargetAppiumHelper,
        setup: suspend (AppiumPorts) -> AppiumTestSetup<T>,
        block: suspend (AppiumTestScope, T) -> Unit,
        report: AppiumTestReport,
    ) {
        // noReset keeps the previous process alive; stop it before replacing its reverse targets.
        withEnglishAndroidAppLocale(target.androidApp.packageName, ::runAdbCommand) {
            runAndroidTestEnvironment(report) { ports ->
                var driver: AndroidDriver? = null
                var testFailed = false
                try {
                    val prepared = setup(ports)
                    prepared.injectedSession?.let { session ->
                        pushInjectedSessionToPrivateDir(
                            packageName = target.androidApp.packageName,
                            file = report.temporaryPath("private-session.json"),
                            content = buildInjectedSessionJson(session),
                        )
                    }
                    val options =
                        UiAutomator2Options()
                            .setAppPackage(target.androidApp.packageName)
                            .setAppActivity(target.androidApp.mainActivityClassName)
                            .setNoReset(true)
                    driver = AndroidDriver(URI("http://127.0.0.1:4723").toURL(), options)
                    driver.startRecordingScreen()
                    block(
                        AndroidAppiumTestScope(
                            driver = AndroidAppTestDriver(driver, report, ::runAdbCommand),
                            packageName = target.androidApp.packageName,
                            restartApp = {
                                collectAndroidCoverage(report, target.androidApp)
                                restartApp(target.androidApp)
                            },
                        ),
                        prepared.data,
                    )
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    testFailed = true
                    throw throwable
                } finally {
                    report.collect("Android coverage") { collectAndroidCoverage(report, target.androidApp) }
                    driver?.let { activeDriver ->
                        report.captureDriver(activeDriver)
                        report.collect("Recording") { saveRecording(activeDriver, report.directory) }
                        report.collect("Driver shutdown") { activeDriver.quit() }
                        if (testFailed) {
                            report.collect("Bugreport") { collectBugreport(report.directory) }
                        }
                    }
                    report.collect("Application log") {
                        copyAppLogToBuild(report.directory, target.androidApp.packageName)
                    }
                    report.collect("Exit diagnostics") {
                        copyAnrTracesToBuild(report.directory, target.androidApp.packageName)
                    }
                    report.collect("Application shutdown") {
                        runAdbCommand(listOf("shell", "am", "force-stop", target.androidApp.packageName))
                    }
                }
            }
        }
    }

    private suspend fun runAndroidTestEnvironment(report: AppiumTestReport, block: suspend (AppiumPorts) -> Unit) {
        useBackendFixture(
            report.directory.resolve("logs"),
            environment = { services ->
                bindAndroidReverse(hostPort = services.filesystemHttpPort, devicePort = 8822)
                mapOf("FILESYSTEM_PUBLIC_URL" to "http://127.0.0.1:8822")
            },
        ) { backend ->
            bindAndroidReverse(hostPort = backend.wsPort, devicePort = 8813)
            bindAndroidReverse(hostPort = backend.serverPort, devicePort = 8811)
            block(createAppiumPorts(backend))
        }
    }

    private fun restartApp(app: AppUnderTest) {
        runAdbCommand(listOf("shell", "am", "force-stop", app.packageName))
        runAdbCommand(listOf("shell", "run-as", app.packageName, "rm", "-f", INJECTED_SESSION_FILE))
        runAdbCommand(listOf("shell", "am", "start", "-n", "${app.packageName}/${app.mainActivityClassName}"))
    }

    private fun collectAndroidCoverage(report: AppiumTestReport, app: AppUnderTest) {
        if (!E2eCoverage.enabled) return
        E2eCoverage.collect(report.directory.resolve("coverage/android")) {
            val pid = runAdbCommand(listOf("shell", "pidof", app.packageName)).trim()
            check(pid.isNotEmpty() && pid.all(Char::isDigit)) { "Expected one running Android application process" }
            val response =
                runAdbCommand(
                    listOf(
                        "shell",
                        "am",
                        "broadcast",
                        "-n",
                        "${app.packageName}/com.storyteller_f.alphaheart.coverage.CoverageReceiver",
                    ),
                )
            check(
                "result=-1" in response && "data=\"$pid\"" in response,
            ) { "Android coverage export did not acknowledge the running process" }
            val directory = report.directory.resolve("coverage/android/$pid").also { it.mkdirs() }
            listOf("report.ic", "classes.properties").forEach { name ->
                val encoded =
                    runAdbCommand(
                        listOf("shell", "run-as", app.packageName, "base64", "files/e2e-coverage/$name"),
                    )
                directory.resolve(name).writeBytes(Base64.getMimeDecoder().decode(encoded))
            }
            check(directory.resolve("report.ic").length() > 0) { "Android coverage report is empty" }
            val scope = if (app.packageName.contains(".panel.")) "panel" else "app"
            E2eCoverage.copyDescriptor(directory.resolve("classes.properties"), directory, ":$scope:androidApp")
        }
    }

    private fun pushInjectedSessionToPrivateDir(packageName: String, file: File, content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content)
        try {
            runAdbCommand(listOf("push", file.canonicalPath, INJECTED_SESSION_TEMP_PATH))
            runAdbCommand(listOf("shell", "run-as", packageName, "mkdir", "-p", INJECTED_SESSION_DIR))
            runAdbCommand(
                listOf(
                    "shell",
                    "run-as",
                    packageName,
                    "cp",
                    INJECTED_SESSION_TEMP_PATH,
                    INJECTED_SESSION_FILE,
                ),
            )
        } finally {
            file.delete()
        }
    }

    private fun bindAndroidReverse(hostPort: Int, devicePort: Int) {
        val devices = runAdbCommandAllowFailure(listOf("devices")).connectedDeviceSerials()
        check(devices.isNotEmpty()) { "No Android device available for adb reverse" }
        devices.forEach { device ->
            val result =
                runAdbCommandAllowFailure(
                    listOf("-s", device, "reverse", "tcp:$devicePort", "tcp:$hostPort"),
                )
            check(result.exitCode == 0) {
                "Failed to bind android reverse for $device tcp:$devicePort -> tcp:$hostPort: " +
                    result.output.ifBlank { "exitCode=${result.exitCode}" }
            }
            waitForAndroidReverse(device, devicePort)
        }
    }

    private fun waitForAndroidReverse(device: String, devicePort: Int) {
        val result =
            runAdbCommandAllowFailure(
                listOf("-s", device, "shell", "nc", "-z", "127.0.0.1", devicePort.toString()),
            )
        if (result.exitCode == 0) return

        val reverseList = runAdbCommandAllowFailure(listOf("-s", device, "reverse", "--list")).output
        error(
            "android reverse $device tcp:$devicePort is not reachable. " +
                "adb reverse --list: ${reverseList.ifBlank { "<empty>" }}",
        )
    }

    private fun copyAppLogToBuild(outputDir: File, packageName: String) {
        val outputFile = File(outputDir, "application.log")
        val logResult =
            runAdbCommandAllowFailure(
                listOf("exec-out", "run-as", packageName, "cat", "files/logs/$APP_LOG_FILE_NAME"),
            )
        if (logResult.exitCode == 0 && logResult.output.isNotBlank()) {
            outputFile.writeText(logResult.output)
        } else {
            error("Failed to export app log: ${logResult.output.ifBlank { "exitCode=${logResult.exitCode}" }}")
        }
    }

    private fun collectBugreport(outputDir: File) {
        val outputFile = File(outputDir, "bugreport.zip")
        runAdbCommand(listOf("bugreport", outputFile.canonicalPath))
    }

    private fun copyAnrTracesToBuild(outputDir: File, packageName: String) {
        val output = runAdbCommand(listOf("shell", "dumpsys", "activity", "exit-info", packageName))
        File(outputDir, "exit-info.txt").writeText(output)
    }

    private fun saveRecording(driver: AndroidDriver, outputDir: File) {
        val content = driver.stopRecordingScreen()
        val decoded = Base64.getDecoder().decode(content)
        File(outputDir, "recording.mp4").writeBytes(decoded)
    }

    private fun runAdbCommand(args: List<String>): String {
        val result = runAdbCommandAllowFailure(args)
        check(result.exitCode == 0) {
            val arguments = args.joinToString(" ")
            if (result.output.isNotEmpty()) {
                "adb command failed ($arguments): ${result.output}"
            } else {
                "adb command failed ($arguments)"
            }
        }
        return result.output
    }

    private fun runAdbCommandAllowFailure(args: List<String>): AdbCommandResult {
        val process =
            ProcessBuilder(listOf("adb") + args)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        return AdbCommandResult(exitCode = process.waitFor(), output = output)
    }

    private data class AdbCommandResult(val exitCode: Int, val output: String) {
        fun connectedDeviceSerials(): List<String> =
            output.lineSequence()
                .drop(1)
                .map { it.trim().split(Regex("\\s+")) }
                .filter { it.size >= 2 && it[1] == "device" }
                .map { it[0] }
                .toList()
    }

    private companion object {
        const val APP_LOG_FILE_NAME = "appium-app.log"
        const val INJECTED_SESSION_TEMP_PATH = "/data/local/tmp/appium-session-session.json"
        const val INJECTED_SESSION_DIR = "files/appium-session"
        const val INJECTED_SESSION_FILE = "files/appium-session/session.json"
    }
}

class DesktopAppiumHelper(
    private val protectedStoreLibrary: String? = null,
    private val filesystemPublicUrl: String? = null,
) : PlatformAppiumHelper {
    private val platform = DesktopTestPlatform.current()

    init {
        require(protectedStoreLibrary == null || platform == DesktopTestPlatform.Linux) {
            "Credential-helper failure scenarios require a Linux desktop target"
        }
    }
    override val capturesExternalAsciidocPreview = false

    override suspend fun <T> runTest(
        testName: String,
        target: TargetAppiumHelper,
        captureBrowserOpen: Boolean,
        setup: suspend (AppiumPorts) -> AppiumTestSetup<T>,
        block: suspend (AppiumTestScope, T) -> Unit,
    ) {
        val report = AppiumTestReport(target.suiteName, "desktop-${platform.name.lowercase()}", testName)
        report.run {
            val browserCapture =
                if (captureBrowserOpen) {
                    DesktopBrowserCapture.create(
                        report.directory.resolve("browser"),
                    )
                } else {
                    null
                }
            runDesktopTest(
                report = report,
                config = target.desktopRuntimeConfig,
                beforeLaunch = { ports, sessionFilePath ->
                    val prepared = setup(ports)
                    prepared.injectedSession?.let { session ->
                        writeSessionFile(sessionFilePath, buildInjectedSessionJson(session))
                    }
                    prepared
                },
                browserCapture = browserCapture,
            ) { driver, prepared, processId, restartApp ->
                block(
                    DesktopAppiumTestScope(DesktopAppTestDriver(driver, report, processId), browserCapture, restartApp),
                    prepared.data,
                )
            }
        }
    }

    private suspend fun <T> runDesktopTest(
        report: AppiumTestReport,
        config: DesktopAppiumRuntimeConfig,
        beforeLaunch: suspend (ports: AppiumPorts, sessionFilePath: String) -> T,
        browserCapture: DesktopBrowserCapture?,
        block: suspend (AppiumDriver, T, Long?, suspend () -> DesktopAppTestDriver) -> Unit,
    ) {
        runAppiumTestEnvironment(report, filesystemPublicUrl = filesystemPublicUrl) { ports ->
            val sessionFile = report.temporaryPath("private-session.json")
            val runtimeDir = report.temporaryPath("runtime")
            val logDir = report.directory.resolve("application").also { it.mkdirs() }
            val appLogFile = File(logDir, "desktop.log")
            runtimeDir.mkdirs()

            val setup = beforeLaunch(ports, sessionFile.canonicalPath)
            val launchScript =
                buildLaunchScript(
                    LaunchScriptContext(
                        ports = ports,
                        sessionFile = sessionFile,
                        runtimeDir = runtimeDir,
                        appLogFile = appLogFile,
                        runtimeClasspath = resolveRuntimeClasspath(config),
                        config = config,
                        browserCapture = browserCapture,
                    ),
                )

            val collectCoverage: suspend () -> Unit = {
                collectDesktopCoverage(report, runtimeDir, config, sessionFile)
            }
            var driver: AppiumDriver? = null
            try {
                val caps = platform.createCapabilities(launchScript, config.windowWaitSeconds)
                val serverUrl = System.getenv("APPIUM_DESKTOP_SERVER_URL") ?: "http://127.0.0.1:4723"
                driver = AppiumDriver(URI(serverUrl).toURL(), caps)
                block(driver, setup, readDesktopProcessId(sessionFile)) {
                    collectCoverage()
                    driver?.quit()
                    sessionFile.delete()
                    File("${sessionFile.canonicalPath}.pid").delete()
                    val restarted = AppiumDriver(URI(serverUrl).toURL(), caps)
                    driver = restarted
                    DesktopAppTestDriver(restarted, report, readDesktopProcessId(sessionFile))
                }
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                report.collect("Desktop diagnostics") {
                    DesktopAppiumFailureDumper.dumpOnFailure(
                        context =
                        DesktopAppiumFailureDumper.Context(
                            outputDir = report.directory.resolve("diagnostics"),
                            appLabel = config.appLabel,
                            logDir = logDir,
                            appLogFile = appLogFile,
                        ),
                        driver = driver,
                        throwable = throwable,
                    )
                }
                throw throwable
            } finally {
                finishDesktopTest(report, driver, launchScript, collectCoverage)
            }
        }
    }

    private suspend fun finishDesktopTest(
        report: AppiumTestReport,
        driver: AppiumDriver?,
        launchScript: File,
        collectCoverage: suspend () -> Unit,
    ) {
        try {
            if (E2eCoverage.enabled) {
                report.collectSuspending("Desktop coverage") {
                    collectCoverage()
                }
            }
        } finally {
            driver?.let { activeDriver ->
                report.captureDriver(activeDriver)
                report.collect("Driver shutdown") { activeDriver.quit() }
            }
            report.collect("Launch script cleanup") { launchScript.deleteRecursively() }
        }
    }

    private suspend fun collectDesktopCoverage(
        report: AppiumTestReport,
        runtimeDir: File,
        config: DesktopAppiumRuntimeConfig,
        sessionFile: File,
    ) {
        if (E2eCoverage.enabled) {
            E2eCoverage.collectJvm(
                runtimeDir.resolve("coverage"),
                report.directory.resolve("coverage/desktop"),
                config.coverageClassSet,
                processIds = readCoverageProcessIds(sessionFile),
            )
        }
    }

    private fun writeSessionFile(path: String, sessionJson: String) {
        File(path).also { it.parentFile?.mkdirs() }.writeText(sessionJson)
    }

    private fun readDesktopProcessId(sessionFile: File): Long? =
        if (platform == DesktopTestPlatform.Windows) {
            File("${sessionFile.canonicalPath}.pid").readText().trim().toLong()
        } else {
            null
        }

    private fun readCoverageProcessIds(sessionFile: File): Set<Long> =
        setOfNotNull(
            File("${sessionFile.canonicalPath}.pid").takeIf(File::isFile)?.readText()?.trim()?.toLongOrNull(),
        )

    private fun resolveRuntimeClasspath(config: DesktopAppiumRuntimeConfig): String =
        config.runtimeClasspathCandidates.firstOrNull { it.isFile }
            ?.readText()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error(config.runtimeClasspathErrorMessage)

    private data class LaunchScriptContext(
        val ports: AppiumPorts,
        val sessionFile: File,
        val runtimeDir: File,
        val appLogFile: File,
        val runtimeClasspath: String,
        val config: DesktopAppiumRuntimeConfig,
        val browserCapture: DesktopBrowserCapture?,
    )

    private suspend fun buildLaunchScript(context: LaunchScriptContext): File {
        val runtimeDir = context.runtimeDir
        val appLogFile = context.appLogFile
        val runtimeClasspath = context.runtimeClasspath
        val config = context.config
        val browserCapture = context.browserCapture
        val javaExec = resolveDesktopJava(platform)
        val launchClasspath =
            listOfNotNull(
                runtimeClasspath,
                File(
                    DesktopTestLauncher::class.java
                        .protectionDomain.codeSource.location.toURI(),
                ).canonicalPath,
            )
                .joinToString(File.pathSeparator)
        val prefsDir = runtimeDir.resolve("prefs").also { it.mkdirs() }
        val tmpDir = runtimeDir.resolve("tmp").also { it.mkdirs() }
        val scriptDir = File("build/test/appium/tmp").also { it.mkdirs() }
        val script =
            File.createTempFile(
                config.scriptPrefix,
                if (usesDesktopArgumentFile(platform)) ".args" else ".sh",
                scriptDir,
            )
        val arguments = buildLaunchArguments(context, launchClasspath, prefsDir, tmpDir)
        if (usesDesktopArgumentFile(platform)) {
            script.writeText(buildJavaArgumentFile(arguments))
            return script
        }
        val browserEnvironment =
            browserCapture?.let {
                """
                export BROWSER="${it.command.canonicalPath.escapeForDoubleQuotedShell()}"
                export PATH="${it.command.parentFile.canonicalPath.escapeForDoubleQuotedShell()}:${'$'}PATH"
                """.trimIndent()
            }.orEmpty()
        script.writeText(
            buildDesktopLaunchScriptContent(
                javaExec = javaExec,
                arguments = arguments,
                appLogFile = appLogFile,
                browserEnvironment = browserEnvironment,
            ),
        )
        script.setExecutable(true)
        return if (platform == DesktopTestPlatform.Mac) {
            buildDesktopMacBundle(script, arguments, javaExec).also { signDesktopMacBundle(it) }
        } else {
            script
        }
    }

    private fun buildLaunchArguments(
        context: LaunchScriptContext,
        runtimeClasspath: String,
        prefsDir: File,
        tmpDir: File,
    ): List<String> {
        val ports = context.ports
        val sessionFile = context.sessionFile
        val appLogFile = context.appLogFile
        val config = context.config
        val arguments =
            buildList {
                add("-Duser.language=en")
                add("-Duser.country=US")
                add("-Dappium.server.url=http://127.0.0.1:${ports.server}")
                if (config.includeWsUrl) {
                    add("-Dappium.ws.url=ws://127.0.0.1:${ports.ws}")
                }
                ports.filesystemHttp?.let { add("-Dappium.filesystem.url=http://127.0.0.1:$it") }
                add("-Dappium.session.file=${sessionFile.canonicalPath}")
                protectedStoreLibrary?.let {
                    require(platform == DesktopTestPlatform.Linux) { "Credential executable fixture requires Linux" }
                    add("-Dappium.protected.storage.library=$it")
                }
                add("-Djava.util.prefs.userRoot=${prefsDir.canonicalPath}")
                add("-Dapp.protected.storage.dir=${File(prefsDir, "protected-storage").canonicalPath}")
                if (platform == DesktopTestPlatform.Linux) {
                    add("-Djava.io.tmpdir=${tmpDir.canonicalPath}")
                }
                add("-XX:ErrorFile=${appLogFile.parentFile.canonicalPath}/hs_err_pid%p.log")
                if (platform == DesktopTestPlatform.Windows) {
                    // Resolve declared JNI resources instead of unrelated EGL DLLs on developer PATH.
                    add("-Djava.library.path=")
                    add("-Djava.util.prefs.PreferencesFactory=${DesktopTestPreferences::class.java.name}")
                }
                add("-Dappium.desktop.tmpdir=${tmpDir.canonicalPath}")
                add("-Dappium.desktop.log=${appLogFile.canonicalPath}")
                if (E2eCoverage.enabled) {
                    add("-De2e.coverage.directory=${context.runtimeDir.resolve("coverage").canonicalPath}")
                }
                add("-Dappium.desktop.pid.file=${sessionFile.canonicalPath}.pid")
                add("-cp")
                add(runtimeClasspath)
                add(DesktopTestLauncher::class.java.name)
                add(config.mainClassName)
            }
        return arguments
    }

    companion object {
        fun safeName(value: String): String = value.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}

internal fun buildDesktopLaunchScriptContent(
    javaExec: String,
    arguments: List<String>,
    appLogFile: File,
    browserEnvironment: String,
): String {
    val appLogPath = appLogFile.path.toBashPath()
    val appLogDirectory = requireNotNull(appLogFile.parentFile).path.toBashPath()
    val argumentLines =
        arguments.joinToString(" \\\n") {
            "              \"${it.escapeForDoubleQuotedShell()}\""
        }
    return buildString {
        appendLine("#!/bin/bash")
        appendLine("""mkdir -p "$appLogDirectory"""")
        if (browserEnvironment.isNotEmpty()) {
            appendLine(browserEnvironment)
        }
        append("exec \"")
        append(javaExec.escapeForDoubleQuotedShell())
        appendLine("\" \\")
        append(argumentLines)
        appendLine(" \\")
        append("""  >> "$appLogPath" 2>&1""")
    }
}

private fun String.toBashPath(): String = replace('\\', '/')

private fun String.escapeForDoubleQuotedShell(): String {
    val escapedBackslashes = replace("\\", "\\\\")
    return escapedBackslashes
        .replace("\"", "\\\"")
        .replace("${'$'}", "\\${'$'}")
        .replace("`", "\\`")
}

private class AndroidAppiumTestScope(
    override val driver: AndroidAppTestDriver,
    private val packageName: String,
    private val restartApp: suspend () -> Unit,
) : AppiumTestScope {
    override suspend fun assertAsciidocPreviewOpened(source: String) {
        driver.assertAsciidocPreviewOpened(packageName, source)
    }

    override suspend fun restart() = restartApp()
}

private class DesktopAppiumTestScope(
    override var driver: DesktopAppTestDriver,
    private val browserCapture: DesktopBrowserCapture?,
    private val restartApp: suspend () -> DesktopAppTestDriver,
) : AppiumTestScope {
    override suspend fun assertAsciidocPreviewOpened(source: String) {
        browserCapture?.assertOpenedAsciidocPreview(source)
            ?: driver.assertAsciidocPreviewOpened()
    }

    override suspend fun restart() {
        driver = restartApp()
    }
}
