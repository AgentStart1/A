/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.app.Application
import android.content.Intent
import android.os.StrictMode
import com.storyteller_f.alphaheart.app.utils.appPlatformImpl
import com.storyteller_f.alphaheart.client.compose_core.components.ConstPlayItem
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.mainActivityRef
import com.storyteller_f.alphaheart.client.compose_core.startPlayMedia
import com.storyteller_f.shared.appContextRef
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.loadCryptoLibIfNeed
import kotlinx.coroutines.DelicateCoroutinesApi
import java.lang.ref.WeakReference

class AApplication : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    val mediaPlayer = buildMediaPlayer()

    override fun onCreate() {
        super.onCreate()
        appPlatformImpl = AndroidAppPlatformImpl
        setupAppLogger(this)
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build(),
        )

        appContextRef = WeakReference(this)
        loadCryptoLibIfNeed()
    }
}

private fun buildMediaPlayer(): MediaPlayerService =
    object : MediaPlayerService() {
        override fun fullscreen(remoteMediaItem: RemoteMediaItem) {
            val context = mainActivityRef?.get() ?: return
            context.startActivity(
                Intent(context, MediaPlayerActivity::class.java).apply {
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    putExtra("json", commonJson.encodeToString<RemoteMediaItem>(remoteMediaItem))
                },
            )
        }

        override suspend fun start(
            remoteMediaItem: RemoteMediaItem,
            localMediaPlaySession: LocalMediaPlaySession,
            playList: List<ConstPlayItem>,
        ) {
            val instance = uiViewModel.instance.value
            instance.controller.startPlayMedia(remoteMediaItem, localMediaPlaySession, this, playList)
        }

        override val enablePip: Boolean
            get() = true
    }
