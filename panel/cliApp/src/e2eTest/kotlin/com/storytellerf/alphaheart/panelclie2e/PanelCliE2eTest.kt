/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.panelclie2e

import com.storytellerf.alphaheart.e2e.E2eCliTerminalFactory
import com.storytellerf.alphaheart.e2e.runE2eBlockingTest
import com.storytellerf.alphaheart.e2e.runE2eTestEnvironment
import org.junit.Rule
import org.junit.rules.TestName
import kotlin.test.Test

private const val INSTALL_DIR_PROPERTY = "panel.cli.app.install.dir"

internal class PanelCliE2eTest {
    @get:Rule
    val testName = TestName()

    private val terminalFactory = E2eCliTerminalFactory()

    @Test
    fun `register and browse panel overview`() {
        runE2eBlockingTest {
            runE2eTestEnvironment("PanelCliE2eTest", testName.methodName) { ports ->
                terminalFactory.useTerminal(INSTALL_DIR_PROPERTY, ports, this) { terminal ->
                    terminal.awaitText("PANEL CLI")
                    terminal.sendLine("1")
                    terminal.awaitText("Enter Private Key (leave empty for auto-generate):")
                    terminal.sendLine("")
                    terminal.awaitText("Registered and Logged in as:")

                    terminal.sendLine("6")
                    terminal.awaitText("Fetching Overview...")
                    terminal.awaitText("=== Overview ===")

                    terminal.sendLine("")
                    terminal.awaitText("--- Menu ---")
                    terminal.sendLine("0")
                    terminal.awaitExit()
                }
            }
        }
    }
}
