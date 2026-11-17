/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.shared.model.FileInfo
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class MediaDownloadActionTest {
    @Test
    fun mediaMenuUsesTheActualFileNameAndSizeAndClosesOnlyItsOwnOptions() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            runComposeUiTest {
                val file =
                    FileInfo.EMPTY.copy(
                        url = "https://fixture.invalid/video?signature=private",
                        name = "real.mp4",
                        size = 6,
                    )
                val requests = mutableListOf<ResourceDownloadRequest>()
                setContent {
                    CompositionLocalProvider(
                        LocalResourceDownloadAction provides {
                            requests.add(it)
                            Unit
                        },
                        LocalResourceDownloadFile provides file,
                    ) {
                        MediaPlayerSurface(
                            "Optional caption",
                            MediaControlState(title = "A different displayed title"),
                            MediaControlActions({}, {}, {}, {}),
                            Modifier.size(360.dp, 203.dp),
                            sourceUrl = file.url,
                        ) {}
                    }
                }
                onNodeWithContentDescription("Media options").performClick()
                onNodeWithContentDescription("Download").performClick()
                onNodeWithContentDescription("Download").assertDoesNotExist()
                runOnIdle {
                    val request = requests.single()
                    assertEquals(file.url, request.url)
                    assertEquals("real.mp4", request.fileName)
                    assertEquals(6L, request.expectedSize)
                }
            }
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun unrelatedMetadataCannotReplaceTheResourceAndFallbackNameOmitsSignedQuery() {
        val resource =
            mediaDownloadRequest(
                "https://fixture.invalid/current.mp4?signature=private#part",
                FileInfo.EMPTY.copy(url = "https://fixture.invalid/other", name = "wrong.mp4", size = 999),
            )
        assertEquals("current.mp4", resource.fileName)
        assertEquals(null, resource.expectedSize)
    }
}
