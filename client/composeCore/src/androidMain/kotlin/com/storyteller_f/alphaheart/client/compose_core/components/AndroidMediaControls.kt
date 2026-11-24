/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.media3.session.MediaController
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlActions
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlState
import com.storyteller_f.alphaheart.client.compose_core.media.createMediaActions
import com.storyteller_f.alphaheart.client.compose_core.media.readMediaControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun rememberMediaControls(player: MediaController): State<MediaControlState> =
    produceState(readMediaControls(player), player) {
        while (isActive) {
            value = readMediaControls(player)
            delay(250)
        }
    }

@Composable
internal fun rememberMediaActions(player: MediaController): MediaControlActions =
    remember(player) { createMediaActions(player) }
