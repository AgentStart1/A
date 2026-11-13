/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopLaunchScriptTest {
    @Test
    fun `builds script with single line continuations`() {
        val script =
            buildDesktopLaunchScriptContent(
                javaExec = "java",
                arguments = listOf("--flag", "-cp", "/tmp/runtime classpath", "example.Main"),
                appLogFile = File("/tmp/appium logs/app.log"),
                browserEnvironment = "export BROWSER=\"/tmp/browser capture\"",
            )
        val expected =
            listOf(
                "#!/bin/bash",
                "mkdir -p \"/tmp/appium logs\"",
                "export BROWSER=\"/tmp/browser capture\"",
                """exec "java" \""",
                """              "--flag" \""",
                """              "-cp" \""",
                """              "/tmp/runtime classpath" \""",
                """              "example.Main" \""",
                "  >> \"/tmp/appium logs/app.log\" 2>&1",
            ).joinToString("\n")

        assertEquals(expected, script)
    }
}
