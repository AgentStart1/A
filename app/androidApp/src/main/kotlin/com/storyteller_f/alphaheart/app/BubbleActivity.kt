/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox

class BubbleActivity : ComponentActivity() {
    val roomId = mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val roomId = intent.getLongExtra("roomId", 0)
        this.roomId.longValue = roomId
        setContent {
            val currentRoomId by this.roomId
            if (currentRoomId == 0L) {
                CenterBox {
                    Text("invalid roomId")
                }
            } else {
                CompositionLocalProvider(
                    LocalUiViewModel provides uiViewModel,
                ) {
                    BubblePage(currentRoomId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val roomId = intent.getLongExtra("roomId", 0)
        this.roomId.longValue = roomId
    }
}
