/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import android.app.Application
import android.content.Intent
import android.os.StrictMode
import com.storyteller_f.alphaheart.client.compose_core.components.ConstPlayItem
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.mainActivityRef
import com.storyteller_f.alphaheart.client.compose_core.startPlayMedia
import com.storyteller_f.shared.appContextRef
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.setupKmpLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.lang.ref.WeakReference

class PanelApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    val panelUiViewModel by lazy { createPanelUIViewModel(applicationScope) }
    val mediaPlayer by lazy { buildPanelMediaPlayer(panelUiViewModel) }

    override fun onCreate() {
        super.onCreate()
        setupKmpLogger()
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build(),
        )
        appContextRef = WeakReference(this)
        loadCryptoLibIfNeed()
    }
}

private fun buildPanelMediaPlayer(panelUiViewModel: PanelUIViewModel): MediaPlayerService =
    object : MediaPlayerService() {
        override fun fullscreen(remoteMediaItem: RemoteMediaItem) {
            val context = mainActivityRef?.get() ?: return
            context.startActivity(
                Intent(context, PanelMediaPlayerActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    putExtra("id", remoteMediaItem.id)
                },
            )
        }

        override suspend fun start(
            remoteMediaItem: RemoteMediaItem,
            localMediaPlaySession: LocalMediaPlaySession,
            playList: List<ConstPlayItem>,
        ) {
            val instance = panelUiViewModel.instance.value
            instance.controller.startPlayMedia(remoteMediaItem, localMediaPlaySession, this, playList)
        }

        override val enablePip: Boolean
            get() = false
    }
