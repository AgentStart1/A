/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.storyteller_f.alphaheart.client.compose_core.components.ConstPlayItem
import com.storyteller_f.alphaheart.client.compose_core.components.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.startPlay
import com.storyteller_f.result.CustomResult
import io.github.aakira.napier.Napier

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Napier.d {
            "PlaybackService onCreate"
        }
        val player = ExoPlayer.Builder(this).build()
        mediaSession =
            MediaSession.Builder(this, player)
                .setCallback(MyCallback())
                .build()
    }

    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? {
        Napier.i {
            "PlaybackService onGetSession $p0"
        }
        return mediaSession
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Napier.d {
            "PlaybackService onTaskRemoved"
        }
        if (isPlaybackOngoing) return
        pauseAllPlayersAndStopSelf()
    }

    private class MyCallback : MediaSession.Callback {
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            Napier.d {
                "PlaybackService onConnect"
            }
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailablePlayerCommands(
                    MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                        .remove(COMMAND_SEEK_TO_NEXT)
                        .remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .remove(COMMAND_SEEK_TO_PREVIOUS)
                        .remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build(),
                )
                .build()
        }
    }
}

/** Starts playback while exposing progress and failures through this dialog controller. */
suspend fun <C> CustomGlobalDialogController<C>.startPlayMedia(
    remoteMediaItem: RemoteMediaItem,
    localMediaPlaySession: LocalMediaPlaySession,
    mediaPlayerService: MediaPlayerService,
    playList: List<ConstPlayItem>,
) {
    useResult {
        mediaPlayerService.startPlay(remoteMediaItem, localMediaPlaySession, playList)
        CustomResult.success(Unit)
    }
}
