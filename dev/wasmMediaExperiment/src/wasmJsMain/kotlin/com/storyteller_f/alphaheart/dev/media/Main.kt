/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewEmbed
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewEmbed
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    enableMediaTestControls()
    ComposeViewport(document.body!!) {
        MaterialTheme {
            var mounted by remember { mutableStateOf(true) }
            var overlay by remember { mutableStateOf(OverlayMode.NONE) }
            var alternate by remember { mutableStateOf(false) }
            val audio = window.location.search.contains("audio=true")
            val item = createFixtureItem(audio, alternate)
            Column(
                Modifier.width(
                    if (window.location.search.contains("display=1080p")) 1952.dp else 640.dp,
                ).padding(16.dp),
            ) {
                Text("Media experiment: WebCodecs + Web Audio + Compose")
                Row {
                    Button(onClick = { mounted = !mounted }) { Text("Mount / dispose") }
                    Button(onClick = { alternate = !alternate }) { Text("Switch source") }
                }
                if (mounted) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))) {
                        if (audio) AudioViewEmbed(item) else VideoViewEmbed(item)
                        if (!audio) {
                            Box(Modifier.align(Alignment.TopEnd)) {
                                Button(onClick = { overlay = OverlayMode.MENU }) { Text("Compose overlay") }
                                DropdownMenu(
                                    expanded = overlay == OverlayMode.MENU,
                                    onDismissRequest = { overlay = OverlayMode.NONE },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open dialog") },
                                        onClick = { overlay = OverlayMode.DIALOG },
                                    )
                                }
                            }
                            Text(
                                "Compose subtitle",
                                Modifier.align(Alignment.Center).background(Color.Yellow).padding(8.dp),
                                color = Color.Black,
                            )
                        }
                    }
                }
                if (overlay == OverlayMode.DIALOG) {
                    AlertDialog(
                        onDismissRequest = { overlay = OverlayMode.NONE },
                        confirmButton = { Button(onClick = { overlay = OverlayMode.NONE }) { Text("Close") } },
                        text = { Text("Compose dialog above decoded frames") },
                    )
                }
            }
        }
    }
}

private fun createFixtureItem(audio: Boolean, alternate: Boolean): RemoteMediaItem {
    val requested = readRequestedMedia()
    val source =
        if (alternate) {
            if (audio) "/fixture-alt.wav" else "/silent.webm"
        } else {
            requested.ifEmpty { "/fixture.${if (audio) "wav" else "webm"}" }
        }
    return RemoteMediaItem("fixture", source, "", "fixture")
}

private enum class OverlayMode {
    NONE,
    MENU,
    DIALOG,
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun(
    "() => { const url = new URL(location.href); url.searchParams.set('appium', 'true'); history.replaceState(null, '', url); }",
)
private external fun enableMediaTestControls()

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => new URL(location.href).searchParams.get('media') ?? ''")
private external fun readRequestedMedia(): String
