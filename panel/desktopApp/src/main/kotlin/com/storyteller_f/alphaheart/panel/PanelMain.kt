/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.runtime.CompositionLocalProvider
import com.storyteller_f.alphaheart.client.compose_core.components.DesktopWindow
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.setupKmpLogger
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import io.github.aakira.napier.Napier
import java.awt.BorderLayout
import java.awt.Button
import java.awt.Dialog
import java.awt.Frame
import java.awt.TextArea
import kotlin.system.exitProcess

fun main() {
    setupKmpLogger()
    initForJvmMain { exitProcess(1) }
    loadCryptoLibIfNeed()
    @Suppress("OPT_IN_USAGE")
    val panelUiViewModel = PanelUIViewModel(kotlinx.coroutines.GlobalScope, getDesktopPanelServerUrl())
    nucleusApplication(backend = NucleusBackend.Tao, enableSingleInstance = false) {
        DesktopWindow(
            onCloseRequest = ::exitApplication,
            title = "Alpha Heart Panel",
        ) {
            CompositionLocalProvider(LocalPanelUiViewModel provides panelUiViewModel) {
                App()
            }
        }
    }
}

private fun initForJvmMain(exit: () -> Nothing) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Napier.e(e) {
            "uncaught exception"
        }
        val dialog = Dialog(Frame(), e.message ?: "Error")
        dialog.apply {
            layout = BorderLayout()
            val label = TextArea(e.stackTraceToString())
            add(label, BorderLayout.CENTER)
            val button = Button("OK")
            button.addActionListener {
                dispose()
                exit()
            }
            add(button, BorderLayout.SOUTH)
            setSize(300, 300)
            isVisible = true
        }
    }
}
