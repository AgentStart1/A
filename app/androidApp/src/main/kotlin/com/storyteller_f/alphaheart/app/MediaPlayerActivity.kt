/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import com.storyteller_f.alphaheart.client.compose_core.commonForActivity
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.media.DefaultMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.shared.commonJson

class MediaPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commonForActivity()
        setContent {
            val json = intent.getStringExtra("json")
            if (json == null) {
                CenterBox {
                    Text("invalid")
                }
            } else {
                val remoteMediaItem = commonJson.decodeFromString<RemoteMediaItem>(json)
                CompositionLocalProvider(
                    LocalUiViewModel provides uiViewModel,
                    LocalMediaPlayListHandlerProvider provides DefaultMediaPlayListHandlerProvider,
                    LocalMediaPlayerService provides (application as AApplication).mediaPlayer,
                ) {
                    MediaPlayerPage(remoteMediaItem)
                }
            }
        }
    }
}
