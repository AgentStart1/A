/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.storyteller_f.alphaheart.client.compose_core.PlaybackService
import com.storyteller_f.alphaheart.client.compose_core.commonForActivity
import com.storyteller_f.alphaheart.client.compose_core.components.DefaultMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.bindActivity
import com.storyteller_f.alphaheart.client.compose_core.components.unbindActivity
import com.storyteller_f.shared.isRunningOnRobolectric
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import java.util.concurrent.Future

class MainActivity : ComponentActivity() {
    private var controllerFuture: Future<MediaController>? = null

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val listener =
            object : MediaController.Listener {
                override fun onAvailableSessionCommandsChanged(controller: MediaController, commands: SessionCommands) {
                    super.onAvailableSessionCommandsChanged(controller, commands)
                    Napier.d {
                        "MediaController onAvailableSessionCommandsChanged"
                    }
                }

                override fun onDisconnected(controller: MediaController) {
                    super.onDisconnected(controller)
                    Napier.d {
                        "MediaController onDisconnected"
                    }
                }
            }
        if (!isRunningOnRobolectric) {
            val future = MediaController.Builder(this, sessionToken).setListener(listener).buildAsync()
            controllerFuture = future
            future.addListener({
                (application as PanelApplication).mediaPlayer.controller.value = future.get()
            }, MoreExecutors.directExecutor())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        commonForActivity()
        bindActivity(this)
        setContent {
            CompositionLocalProvider(
                LocalPanelUiViewModel provides (application as PanelApplication).panelUiViewModel,
                LocalMediaPlayListHandlerProvider provides DefaultMediaPlayListHandlerProvider,
                LocalMediaPlayerService provides (application as PanelApplication).mediaPlayer,
            ) {
                App()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindActivity()
    }
}
