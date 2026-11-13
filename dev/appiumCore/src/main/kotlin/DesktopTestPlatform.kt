/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.openqa.selenium.remote.DesiredCapabilities
import java.io.File

internal enum class DesktopTestPlatform {
    Linux,
    Mac,
    Windows,
    ;

    fun createCapabilities(launchFile: File, timeoutSeconds: Long): DesiredCapabilities =
        DesiredCapabilities().apply {
            setCapability("appium:newCommandTimeout", timeoutSeconds)
            when (this@DesktopTestPlatform) {
                Linux -> {
                    setCapability("platformName", "linux")
                    setCapability("appium:automationName", "linux")
                    setCapability("appium:app", resolveDesktopJava(this@DesktopTestPlatform))
                    setCapability("appium:appArguments", listOf("@${launchFile.canonicalPath}"))
                    setCapability("appium:appWorkingDir", File(".").canonicalPath)
                }

                Mac -> {
                    setCapability("platformName", "Mac")
                    setCapability("appium:automationName", "Mac2")
                    if (launchFile.extension == "args") {
                        // A remote environment provider packages the prebuilt target launcher.
                        setCapability("appium:app", resolveDesktopJava(this@DesktopTestPlatform))
                        setCapability("appium:appArguments", listOf("@${launchFile.canonicalPath}"))
                        setCapability("appium:appWorkingDir", File(".").canonicalPath)
                    } else {
                        setCapability("appium:appPath", launchFile.canonicalPath)
                        setCapability(
                            "appium:bundleId",
                            "com.storyteller_f.alphaheart.desktoptest.${launchFile.nameWithoutExtension}",
                        )
                    }
                }

                Windows -> {
                    setCapability("platformName", "Windows")
                    setCapability("appium:automationName", "DesktopDriver")
                    setCapability("appium:app", resolveDesktopJava(this@DesktopTestPlatform))
                    setCapability("appium:appArguments", "\"@${launchFile.canonicalPath}\"")
                    setCapability("appium:appWorkingDir", File(".").canonicalPath)
                    setCapability("appium:javaSwing", false)
                    setCapability("appium:ms:forcequit", true)
                    setCapability("appium:ms:waitForAppLaunch", timeoutSeconds)
                }
            }
        }

    companion object {
        fun current(): DesktopTestPlatform {
            val requested = System.getenv("APPIUM_DESKTOP_PLATFORM") ?: System.getProperty("os.name")
            return when {
                requested.equals("linux", ignoreCase = true) -> Linux
                requested.startsWith("Windows", ignoreCase = true) -> Windows
                requested.startsWith("Mac", ignoreCase = true) -> Mac
                else -> error("Unsupported desktop test platform: $requested; expected Linux, Windows or Mac")
            }
        }
    }
}

/** Remote macOS launches use the same portable argument file as other remote desktops. */
internal fun usesDesktopArgumentFile(
    platform: DesktopTestPlatform,
    hostIsMac: Boolean = System.getProperty("os.name").startsWith("Mac", ignoreCase = true),
): Boolean = platform != DesktopTestPlatform.Mac || !hostIsMac

internal fun resolveDesktopJava(platform: DesktopTestPlatform): String =
    System.getenv("APP_DESKTOP_TEST_JAVA")
        ?: File(
            System.getProperty("java.home"),
            "bin/${if (platform == DesktopTestPlatform.Windows) "javaw.exe" else "java"}",
        )
            .canonicalPath

internal fun buildJavaArgumentFile(arguments: List<String>): String =
    arguments.joinToString("\n", postfix = "\n") { argument ->
        require('\n' !in argument && '\r' !in argument) { "JVM arguments must not contain newlines" }
        val escaped = argument.replace("\\", "\\\\").replace("\"", "\\\"")
        "\"$escaped\""
    }

/** Reuses the JDK's precompiled native launcher so the JVM retains its unique bundle identity. */
internal fun buildDesktopMacBundle(script: File, arguments: List<String>, javaExec: String): File {
    val bundle = File(script.parentFile, "${script.nameWithoutExtension}.app")
    val contents = bundle.resolve("Contents")
    contents.mkdirs()
    val executable = contents.resolve("MacOS/desktop-test")
    executable.parentFile.mkdirs()
    val launcher =
        java.nio.file.FileSystems.getFileSystem(java.net.URI.create("jrt:/"))
            .getPath("/modules/jdk.jpackage/jdk/jpackage/internal/resources/jpackageapplauncher")
    executable.writeBytes(java.nio.file.Files.readAllBytes(launcher))
    check(executable.setExecutable(true)) { "Cannot make the desktop test launcher executable" }
    val app = contents.resolve("app")
    app.mkdirs()
    app.resolve(
        "desktop-test.cfg",
    ).writeText(buildMacLauncherConfig(arguments, File(javaExec).parentFile.parentFile.path))
    contents.resolve("Info.plist").writeText(
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0"><dict>
          <key>CFBundleIdentifier</key><string>com.storyteller_f.alphaheart.desktoptest.${script.nameWithoutExtension}</string>
          <key>CFBundleExecutable</key><string>desktop-test</string>
          <key>CFBundlePackageType</key><string>APPL</string>
          <key>CFBundleName</key><string>Desktop test</string>
        </dict></plist>
        """.trimIndent(),
    )
    script.delete()
    return bundle
}

internal fun buildMacLauncherConfig(arguments: List<String>, javaHome: String): String {
    require(arguments.none { '\n' in it || '\r' in it }) { "JVM arguments must not contain newlines" }
    val classpathIndex = arguments.indexOf("-cp")
    require(classpathIndex >= 0 && arguments.size > classpathIndex + 2) { "Missing JVM classpath or main class" }
    return buildString {
        appendLine("[Application]")
        appendLine("app.runtime=${File(javaHome).parentFile.parentFile.path}")
        appendLine("app.classpath=${arguments[classpathIndex + 1]}")
        appendLine("app.mainclass=${arguments[classpathIndex + 2]}")
        appendLine("[JavaOptions]")
        arguments.take(classpathIndex).forEach { appendLine("java-options=$it") }
        appendLine("[ArgOptions]")
        arguments.drop(classpathIndex + 3).forEach { appendLine("arguments=$it") }
    }
}

/** Ad-hoc signing seals the generated bundle; it packages existing native code without compiling it. */
internal suspend fun signDesktopMacBundle(bundle: File) =
    withContext(Dispatchers.IO) {
        val log = File(bundle.parentFile, "${bundle.nameWithoutExtension}.codesign.log")
        val process =
            ProcessBuilder("/usr/bin/codesign", "--force", "--sign", "-", bundle.canonicalPath)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .start()
        try {
            check(withTimeout(30_000L) { process.onExit().await().exitValue() } == 0) {
                "Desktop bundle signing failed: ${log.readText()}"
            }
        } finally {
            if (process.isAlive) {
                withContext(NonCancellable) { process.destroyForcibly().onExit().await() }
            }
            log.delete()
        }
    }
