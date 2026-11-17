/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.audio_content_description
import com.storyteller_f.alphaheart.app.file_content_description
import com.storyteller_f.alphaheart.app.pages.file.UploadItemRow
import com.storyteller_f.alphaheart.app.video_content_description
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class UploadItemSemanticsTest {
    @Test
    fun uploadRowsAnnounceTheFullFileAndKeepDetailLongPressAndNestedTransportActionsAtBothWidths() =
        runComposeUiTest {
            val files =
                listOf("audio/flac", "video/mp4", "text/plain").mapIndexed { index, type ->
                    UploadInfo.EMPTY.copy(
                        id = index.toLong() + 1,
                        name = "upload-$index",
                        contentType = type,
                        total = 1024,
                        progress = 512,
                        status = UploadStatus.UPLOADING,
                    )
                }
            val width = mutableStateOf(800.dp)
            val opened = mutableListOf<Long>()
            val longPressed = mutableListOf<Long>()
            val transport = mutableListOf<Long>()
            var genericDescriptions = emptyList<String>()
            setContent {
                MaterialTheme {
                    ReadingInteractions {
                        genericDescriptions =
                            listOf(
                                stringResource(Res.string.audio_content_description),
                                stringResource(Res.string.video_content_description),
                                stringResource(Res.string.file_content_description),
                            )
                        Column(Modifier.width(width.value)) {
                            files.forEach { file ->
                                UploadItemRow(file, "Uploading", { opened += file.id }, { longPressed += file.id }) {
                                    ReadingIconButton({ transport += file.id }, Modifier.testTag("pause-${file.id}")) {
                                        ReadingIcon(Icons.Default.Pause, "Pause")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            for (next in listOf(800.dp, 390.dp)) {
                runOnIdle { width.value = next }
                genericDescriptions.forEach { generic ->
                    onNodeWithContentDescription(generic, useUnmergedTree = true).assertDoesNotExist()
                }
                for (file in files) {
                    val row = onNode(hasContentDescription(file.name, substring = true) and hasClickAction())
                    row.assertContentDescriptionContains(file.contentType, substring = true)
                        .assertContentDescriptionContains(HumanReadable.fileSize(file.total), substring = true)
                        .assertContentDescriptionContains("Uploading", substring = true)
                        .assertHasClickAction().performClick()
                    row.performSemanticsAction(SemanticsActions.OnLongClick) { it() }
                    onNodeWithTag("pause-${file.id}").assertHasClickAction().performClick()
                }
            }
            runOnIdle {
                val expected = (files + files).map { it.id }
                assertEquals(expected, opened)
                assertEquals(expected, longPressed)
                assertEquals(expected, transport)
            }
        }
}
