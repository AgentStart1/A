/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.ImageBitmap
import com.storyteller_f.alphaheart.app.common.Downloader
import com.storyteller_f.alphaheart.app.common.ExternalUriHandler
import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import com.storyteller_f.alphaheart.app.common.Uploader
import com.storyteller_f.alphaheart.app.common.UploaderImpl
import com.storyteller_f.alphaheart.app.utils.AppPlatformImpl
import com.storyteller_f.alphaheart.app.utils.appPlatformImpl
import com.storyteller_f.alphaheart.client.compose_core.components.ConstPlayItem
import com.storyteller_f.alphaheart.client.compose_core.components.DesktopWindow
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.setupKmpLogger
import com.storyteller_f.shared.utils.safeMessage
import dev.nucleusframework.application.NucleusBackend
import dev.nucleusframework.application.nucleusApplication
import io.github.aakira.napier.Napier
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import java.awt.BorderLayout
import java.awt.Button
import java.awt.Dialog
import java.awt.Frame
import java.awt.TextArea
import kotlin.system.exitProcess
import kotlin.uuid.ExperimentalUuidApi

/** Shared desktop UI model. */
@OptIn(DelicateCoroutinesApi::class)
val uiViewModel: UIViewModel by lazy {
    UIViewModel(
        GlobalScope,
        getDesktopWsServerUrl(),
        getDesktopServerUrl(),
    )
}

object JvmAppPlatformImpl : AppPlatformImpl {
    override suspend fun notifyNotification(room: RoomInfo, bitmap: ImageBitmap?) = Unit
}

@OptIn(ExperimentalUuidApi::class)
private val jvmMediaPlayerService =
    object : MediaPlayerService() {
        override val enablePip: Boolean
            get() = false

        override fun fullscreen(remoteMediaItem: RemoteMediaItem) = Unit

        override suspend fun start(
            remoteMediaItem: RemoteMediaItem,
            localMediaPlaySession: LocalMediaPlaySession,
            playList: List<ConstPlayItem>,
        ) {
            state.value =
                MediaPlaySession(
                    remoteMediaItem = remoteMediaItem,
                    playList = playList,
                    uuids = listOf(localMediaPlaySession.uuid),
                    videoSize = null,
                )
        }
    }

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>) {
    appPlatformImpl = JvmAppPlatformImpl
    setupKmpLogger()
    initForJvmMain { exitCode -> exitProcess(exitCode) }
    loadCryptoLibIfNeed()
    val taskRegister = SimpleTaskRegister(GlobalScope)
    val provider =
        object : ClientFileProvider {
            override suspend fun getDownloader(): Downloader = uiViewModel.downloader

            override suspend fun getUploader(): Uploader = UploaderImpl(uiViewModel, taskRegister)
        }
    nucleusApplication(args = args, backend = NucleusBackend.Tao, enableSingleInstance = false) {
        onDeepLink { uri -> ExternalUriHandler.onNewUri(uri.toString()) }
        DesktopWindow(
            onCloseRequest = ::exitApplication,
            title = "Alpha Heart",
        ) {
            CompositionLocalProvider(
                LocalClientFileProvider provides provider,
                LocalUiViewModel provides uiViewModel,
                LocalMediaPlayerService provides jvmMediaPlayerService,
            ) {
                App()
            }
        }
    }
}

private fun initForJvmMain(exitApplication: (Int) -> Unit) {
    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        Napier.e(e) {
            "uncaught exception"
        }
        Dialog(Frame(), e.safeMessage()).apply {
            layout = BorderLayout()
            val label = TextArea(e.stackTraceToString())
            add(label, BorderLayout.CENTER)
            val button = Button("OK")
            button.addActionListener {
                dispose()
                exitApplication(1)
            }
            add(button, BorderLayout.SOUTH)
            setSize(300, 300)
            isVisible = true
        }
    }
}
