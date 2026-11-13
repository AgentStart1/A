/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DesktopTestPlatformTest {
    @Test
    fun `remote mac launch uses portable arguments`() {
        val arguments = File("build/test/mac arguments.args")
        val caps = DesktopTestPlatform.Mac.createCapabilities(arguments, 60L)

        assertEquals("Mac2", caps.getCapability("appium:automationName"))
        assertEquals(listOf("@${arguments.canonicalPath}"), caps.getCapability("appium:appArguments"))
        assertEquals(null, caps.getCapability("appium:appPath"))
        assertEquals(true, usesDesktopArgumentFile(DesktopTestPlatform.Mac, hostIsMac = false))
        assertEquals(false, usesDesktopArgumentFile(DesktopTestPlatform.Mac, hostIsMac = true))
        assertEquals(true, usesDesktopArgumentFile(DesktopTestPlatform.Windows, hostIsMac = true))
    }

    @Test
    fun `mac native predicates escape text`() {
        assertEquals("""'O\'Brien\\file\nnext'""", macPredicateLiteral("O'Brien\\file\nnext"))
    }

    @Test
    fun `windows launches native UIA with argument file`() {
        val arguments = File("build/test/desktop arguments.args")
        val capabilities = DesktopTestPlatform.Windows.createCapabilities(arguments, 60L)

        assertEquals("windows", requireNotNull(capabilities.getCapability("platformName")).toString())
        assertEquals("DesktopDriver", capabilities.getCapability("appium:automationName"))
        assertEquals(false, capabilities.getCapability("appium:javaSwing"))
        assertEquals("\"@${arguments.canonicalPath}\"", capabilities.getCapability("appium:appArguments"))
    }

    @Test
    fun `argument file preserves special characters`() {
        val arguments = listOf("-cp", "C:\\runtime folder\\app.jar", "-Dexample=\"quoted\"")

        assertEquals(
            """
            "-cp"
            "C:\\runtime folder\\app.jar"
            "-Dexample=\"quoted\""
            """.trimIndent() + "\n",
            buildJavaArgumentFile(arguments),
        )
        assertFailsWith<IllegalArgumentException> { buildJavaArgumentFile(listOf("invalid\nargument")) }
    }

    @Test
    fun `linux launches Java with argument array`() {
        val script = File("build/test/desktop.sh")
        val capabilities = DesktopTestPlatform.Linux.createCapabilities(script, 60L)

        assertEquals("linux", capabilities.getCapability("appium:automationName"))
        assertEquals(resolveDesktopJava(DesktopTestPlatform.Linux), capabilities.getCapability("appium:app"))
        assertEquals(listOf("@${script.canonicalPath}"), capabilities.getCapability("appium:appArguments"))
    }

    @Test
    fun `mac launcher retains JVM and app arguments`() {
        val bundle = File("build/test/desktop-test.app")
        val caps = DesktopTestPlatform.Mac.createCapabilities(bundle, 60L)
        assertEquals("Mac2", caps.getCapability("appium:automationName"))
        assertEquals(bundle.canonicalPath, caps.getCapability("appium:appPath"))
        val config =
            buildMacLauncherConfig(
                listOf("-Dexample=space value", "-cp", "/runtime/app.jar", "Launcher", "AppMain"),
                "/runtime/jdk/Contents/Home",
            )
        kotlin.test.assertTrue(config.contains("app.mainclass=Launcher\n"))
        kotlin.test.assertTrue(config.contains("java-options=-Dexample=space value\n"))
        kotlin.test.assertTrue(config.contains("arguments=AppMain\n"))
        assertFailsWith<IllegalArgumentException> {
            buildMacLauncherConfig(listOf("-cp", "bad\npath", "Main"), "/jdk/Contents/Home")
        }
    }

    @Test
    fun `multiline input sends Enter keys`() {
        assertEquals(
            listOf(mapOf("text" to "first"), mapOf("virtualKeyCode" to 13), mapOf("text" to "second")),
            buildWindowsTextActions("first\r\nsecond"),
        )
    }

    @Test
    fun `preferences isolate JVMs and retain nodes`() {
        val first = DesktopTestPreferences().userRoot()
        val second = DesktopTestPreferences().userRoot()
        first.node("app").put("test-key", "value")

        assertEquals("value", first.node("app").get("test-key", null))
        assertEquals(null, second.node("app").get("test-key", null))
        first.node("app").removeNode()
        assertEquals(null, first.node("app").get("test-key", null))
    }
}
