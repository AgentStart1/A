/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import io.github.aakira.napier.Napier
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun EnablePipPre31(enablePip: Boolean, localMediaPlaySession: LocalMediaPlaySession) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        val context = LocalContext.current
        DisposableEffect(context, enablePip, localMediaPlaySession) {
            val onUserLeaveBehavior =
                Runnable {
                    Napier.d(tag = "MediaPlayer") {
                        "EnablePipPre31 ${localMediaPlaySession.uuid} $enablePip"
                    }
                    if (enablePip) {
                        context.findActivity()
                            .enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    }
                }
            context.findActivity().addOnUserLeaveHintListener(onUserLeaveBehavior)
            onDispose {
                context.findActivity().removeOnUserLeaveHintListener(onUserLeaveBehavior)
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun BoxScope.AndroidPlayerContainer(player: MediaController, block: @Composable BoxScope.() -> Unit) {
    block()
    PipBroadcastReceiver(player)
}

@Composable
fun Modifier.androidPipMode(enable: Boolean, ratio: Rational): Modifier {
    val mediaPlayerService = LocalMediaPlayerService.current
    if (!mediaPlayerService.enablePip) return this
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return this
    }
    if (!enable) {
        val builder = PictureInPictureParams.Builder().setAutoEnterEnabled(false)
        context.findActivity().setPictureInPictureParams(builder.build())
        return this
    }
    return onGloballyPositioned { layoutCoordinates ->
        val sourceRect = layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()
        val builder = PictureInPictureParams.Builder()
        // 12 之后引入
        builder.setSourceRectHint(sourceRect)
        builder.setAutoEnterEnabled(true)
        builder.setActions(emptyList())
        builder.setAspectRatio(ratio)
        context.findActivity().setPictureInPictureParams(builder.build())
    }
}

internal fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error("Picture in picture should be called in the context of an Activity")
}

@Composable
fun PipBroadcastReceiver(player: Player) {
    val isInPipMode = rememberIsInPipMode()
    if (!isInPipMode) return
    val context = LocalContext.current

    DisposableEffect(player) {
        val broadcastReceiver: BroadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent == null || intent.action != ACTION_BROADCAST_CONTROL) {
                        return
                    }

                    when (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                        EXTRA_CONTROL_PAUSE -> player.pause()
                        EXTRA_CONTROL_PLAY -> player.play()
                    }
                }
            }
        ContextCompat.registerReceiver(
            context,
            broadcastReceiver,
            IntentFilter(ACTION_BROADCAST_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }
    }
}
