/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.github.panpf.zoomimage.util.setMainThreadChecker
import com.storyteller_f.alphaheart.client.compose_core.utils.LocalFileDialogSettings
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.core.runtime.Platform
import dev.nucleusframework.window.TitleBar
import dev.nucleusframework.window.styling.LocalTitleBarStyle
import dev.nucleusframework.window.tao.LocalTaoWindow
import io.github.vinceglb.filekit.dialogs.FileKitDialogParent
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings

@Composable
fun DesktopWindow(title: String, onCloseRequest: () -> Unit, content: @Composable () -> Unit) {
    val navigationInput = remember { DesktopBackNavigationInput() }
    DecoratedWindow(title = title, onCloseRequest = onCloseRequest, onKeyEvent = navigationInput::onKeyEvent) {
        remember { bindDesktopImageThread(Thread.currentThread()) }
        val window = LocalTaoWindow.current
        val dialogSettings =
            remember(window) {
                when (Platform.Current) {
                    Platform.Windows ->
                        FileKitDialogSettings(parent = FileKitDialogParent.windows(requireNotNull(window).nativeHandle))

                    Platform.Linux ->
                        window?.x11WindowId?.let { FileKitDialogSettings(parent = FileKitDialogParent.x11(it)) }
                            ?: FileKitDialogSettings.createDefault()

                    else -> FileKitDialogSettings.createDefault()
                }
            }
        val dispatcher = requireNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
        DisposableEffect(dispatcher, navigationInput) {
            dispatcher.addInput(navigationInput)
            onDispose { dispatcher.removeInput(navigationInput) }
        }
        TitleBar { BasicText(title, style = TextStyle(color = LocalTitleBarStyle.current.colors.content)) }
        CompositionLocalProvider(LocalFileDialogSettings provides dialogSettings) {
            content()
        }
    }
}

/** Supplies the same Escape-to-navigation binding as Compose's AWT window mediator. */
internal class DesktopBackNavigationInput : NavigationEventInput() {
    private var attached = false

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        attached = true
    }

    override fun onRemoved() {
        attached = false
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!attached || event.type != KeyEventType.KeyDown || event.key != Key.Escape) return false
        dispatchOnBackCompleted()
        return true
    }
}

/** ZoomImage defaults to Swing; the native window owns its own UI event thread. */
internal fun bindDesktopImageThread(owner: Thread) {
    setMainThreadChecker { Thread.currentThread() === owner }
}
