/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.panelclie2e

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.overview
import com.storyteller_f.result.getOrThrow
import com.storytellerf.alphaheart.e2e.E2eCliTerminal
import com.storytellerf.alphaheart.e2e.E2eCliTerminalFactory
import com.storytellerf.alphaheart.e2e.E2ePanel
import com.storytellerf.alphaheart.e2e.E2ePorts
import com.storytellerf.alphaheart.e2e.createE2ePanel
import com.storytellerf.alphaheart.e2e.createE2eUser
import com.storytellerf.alphaheart.e2e.runE2eBlockingTest
import com.storytellerf.alphaheart.e2e.runE2eTestEnvironment
import org.junit.Rule
import org.junit.rules.TestName
import kotlin.test.Test

internal class PanelCliContractE2eTest {
    @get:Rule
    val testName = TestName()
    private val factory = E2eCliTerminalFactory()

    @Test
    fun `login accepts multiline key for expected panel identity`() =
        withPanel { _, terminal, panel ->
            val model = panel.session.model
            terminal.awaitText(requireNotNull(model.userHandler.data.value).name)
            terminal.sendLine("6")
            terminal.awaitText("=== Overview ===")
        }

    @Test
    fun `all users renders the seeded user`() =
        withPanel { ports, terminal, _ ->
            createE2eUser(ports).use { user ->
                terminal.sendLine("3")
                terminal.awaitText("=== Member List ===")
                terminal.awaitText("User ID: ${user.id}")
            }
        }

    @Test
    fun `all communities renders the seeded community`() =
        withPanel { ports, terminal, _ ->
            createE2eUser(ports).use { user ->
                val community =
                    user.session.createCommunity(
                        NewCommunity("panel-cli-community", "panelclicommunity"),
                    ).getOrThrow()
                terminal.sendLine("4")
                terminal.awaitText("=== Community List ===")
                terminal.awaitText("Community ID: ${community.id} - ${community.name}")
            }
        }

    @Test
    fun `all rooms renders the seeded public room`() =
        withPanel { ports, terminal, _ ->
            createE2eUser(ports).use { user ->
                val community =
                    user.session.createCommunity(
                        NewCommunity("panel-cli-community", "panelclicommunity"),
                    ).getOrThrow()
                val room =
                    user.session.createRoom(
                        NewRoom("panel-cli-room", "panelcliroom", communityId = community.id),
                    ).getOrThrow()
                terminal.sendLine("5")
                terminal.awaitText("=== Room List ===")
                terminal.awaitText("Room ID: ${room.id} - ${room.name}")
            }
        }

    @Test
    fun `overview renders real backend user and file counts`() =
        withPanel { _, terminal, panel ->
            val overview = panel.session.overview().getOrThrow()
            terminal.sendLine("6")
            terminal.awaitText("=== Overview ===")
            terminal.awaitText("User count: ${overview.userCount}")
            terminal.awaitText("File count: ${overview.fileCount}")
            terminal.awaitText("File volume: ${overview.fileVolume}")
        }

    @Test
    fun `invalid private key cannot access panel overview`() =
        withGuest { _, terminal ->
            terminal.sendLine("2")
            terminal.awaitText("Enter Private Key:")
            terminal.sendLine("not-a-private-key")
            terminal.awaitText("Error")
            terminal.sendLine("6")
            terminal.awaitText("Failed")
            terminal.sendLine("0")
            terminal.awaitExit()
        }

    @Test
    fun `guest cannot query panel users`() =
        withGuest { _, terminal ->
            terminal.sendLine("3")
            terminal.awaitText("Failed")
            terminal.sendLine("0")
            terminal.awaitExit()
        }

    @Test
    fun `invalid menu choice retains panel terminal usability`() =
        withGuest { _, terminal ->
            terminal.sendLine("unknown-command")
            terminal.awaitText("Invalid Choice")
            terminal.sendLine("0")
            terminal.awaitExit()
        }

    private fun withGuest(scenario: suspend (E2ePorts, E2eCliTerminal) -> Unit) =
        runE2eBlockingTest {
            runE2eTestEnvironment("PanelCliContractE2eTest", testName.methodName) { ports ->
                factory.useTerminal("panel.cli.app.install.dir", ports, this) { terminal ->
                    terminal.awaitText("PANEL CLI")
                    scenario(ports, terminal)
                }
            }
        }

    private fun withPanel(scenario: suspend (E2ePorts, E2eCliTerminal, E2ePanel) -> Unit) =
        withGuest { ports, terminal ->
            createE2ePanel(ports).use { panel ->
                terminal.sendLine("2")
                terminal.awaitText("Enter Private Key:")
                panel.key.lineSequence().filter(String::isNotBlank).forEach { terminal.sendLine(it) }
                terminal.awaitText("Logged in as:")
                scenario(ports, terminal, panel)
            }
        }
}
