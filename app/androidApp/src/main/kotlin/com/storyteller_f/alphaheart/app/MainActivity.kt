/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.storyteller_f.alphaheart.app.utils.initEnvironment
import com.storyteller_f.alphaheart.client.compose_core.PlaybackService
import com.storyteller_f.alphaheart.client.compose_core.commonForActivity
import com.storyteller_f.alphaheart.client.compose_core.components.DefaultMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.unbindActivity
import com.storyteller_f.shared.isRunningOnRobolectric
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import java.util.concurrent.Future

class MainActivity :
    ComponentActivity(),
    ClientFileServiceContainer {
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
                (application as AApplication).mediaPlayer.controller.value = future.get()
            }, MoreExecutors.directExecutor())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        commonForActivity()
        initEnvironment(this)
        registerDevice(this)
        val receiver = CustomClientFileProvider(this)
        setContent {
            CompositionLocalProvider(
                LocalClientFileProvider provides receiver,
                LocalUiViewModel provides uiViewModel,
                LocalMediaPlayListHandlerProvider provides DefaultMediaPlayListHandlerProvider,
                LocalMediaPlayerService provides (application as AApplication).mediaPlayer,
            ) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAppNotificationIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindActivity()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }

    override var binder: FileBinder? = null
    override var isConnecting: Boolean = false
}
