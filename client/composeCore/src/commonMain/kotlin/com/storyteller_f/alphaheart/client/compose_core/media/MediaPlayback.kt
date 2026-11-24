/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface PlayItem {
    val id: String
    val icon: String?
    val title: String?
}

@SerialName("com.storyteller_f.alphaheart.client.compose_core.components.ConstPlayItem")
@Serializable
data class ConstPlayItem(val url: String, override val icon: String? = null, override val title: String? = null) :
    PlayItem {
    override val id: String
        get() = url
}

@OptIn(ExperimentalUuidApi::class)
data class LocalMediaPlaySession(val id: String, val uuid: Uuid)

@SerialName("com.storyteller_f.alphaheart.client.compose_core.components.CustomVideoSize")
@Serializable
data class CustomVideoSize(val width: Int, val height: Int)

@OptIn(ExperimentalUuidApi::class)
@SerialName("com.storyteller_f.alphaheart.client.compose_core.components.MediaPlaySession")
@Serializable
data class MediaPlaySession(
    val remoteMediaItem: RemoteMediaItem,
    val playList: List<ConstPlayItem>,
    val uuids: List<Uuid>,
    val videoSize: CustomVideoSize?,
) {
    val id = remoteMediaItem.url

    val lastUuid get() = uuids.lastOrNull()

    @OptIn(ExperimentalUuidApi::class)
    fun appendUuid(uuid: Uuid) = copy(uuids = uuids + uuid)

    val uuidCount get() = uuids.size
}

val LocalMediaPlayerService =
    staticCompositionLocalOf<MediaPlayerService> {
        error("LocalMediaPlayerComponent not provided")
    }

interface MediaPlayListHandlerProvider {
    @Composable
    fun playListHandler(remoteMediaItem: RemoteMediaItem): LoadingHandler<List<ConstPlayItem>>
}

val LocalMediaPlayListHandlerProvider =
    staticCompositionLocalOf<MediaPlayListHandlerProvider> {
        error("LocalMediaPlayListHandlerProvider must be provided")
    }

expect abstract class MediaPlayerService {
    abstract val enablePip: Boolean
    val state: MutableStateFlow<MediaPlaySession?>

    abstract fun fullscreen(remoteMediaItem: RemoteMediaItem)
    abstract suspend fun start(
        remoteMediaItem: RemoteMediaItem,
        localMediaPlaySession: LocalMediaPlaySession,
        playList: List<ConstPlayItem>,
    )
}
