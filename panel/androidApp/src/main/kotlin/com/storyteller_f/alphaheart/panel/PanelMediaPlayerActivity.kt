/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import com.storyteller_f.alphaheart.client.compose_core.commonForActivity
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.DefaultMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayListHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayerService
import com.storyteller_f.alphaheart.panel.pages.PanelFilePreviewPage

class PanelMediaPlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        commonForActivity()
        setContent {
            val fileId = intent.getLongExtra("id", 0)
            if (fileId == 0L) {
                CenterBox {
                    Text("Invalid")
                }
            } else {
                CompositionLocalProvider(
                    LocalPanelUiViewModel provides (application as PanelApplication).panelUiViewModel,
                    LocalMediaPlayListHandlerProvider provides DefaultMediaPlayListHandlerProvider,
                    LocalMediaPlayerService provides (application as PanelApplication).mediaPlayer,
                ) {
                    PanelFilePreviewPage(fileId)
                }
            }
        }
    }
}
