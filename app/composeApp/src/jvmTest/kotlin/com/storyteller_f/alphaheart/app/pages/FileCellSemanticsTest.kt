/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.ObjectType
import kotlinx.datetime.LocalDateTime
import nl.jacobras.humanreadable.HumanReadable
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class FileCellSemanticsTest {
    @Test
    fun reusedFileCellsAnnounceTheCompleteNameTypeAndSizeAndKeepTheirActionAtBothWidths() =
        runComposeUiTest {
            val types = listOf("audio/flac", "video/mp4", "application/octet-stream")
            val files =
                types.mapIndexed { index, type ->
                    FileInfo(
                        id = index + 1L,
                        url = "",
                        fullName = "file-$index",
                        contentType = type,
                        size = 1024,
                        name = "file-$index",
                        owner = 1,
                        ownerType = ObjectType.USER,
                        lastModified = LocalDateTime(2026, 1, 1, 0, 0),
                        dimension = null,
                    )
                }
            val width = mutableStateOf(800.dp)
            val clicked = mutableListOf<FileInfo>()
            setContent {
                MaterialTheme {
                    ReadingInteractions {
                        Column(Modifier.width(width.value)) {
                            files.forEach { file -> FileCell(file) { clicked.addAll(it) } }
                        }
                    }
                }
            }
            for (next in listOf(800.dp, 390.dp)) {
                runOnIdle { width.value = next }
                for (file in files) {
                    onNodeWithContentDescription(file.name, substring = true)
                        .assertContentDescriptionContains(file.contentType, substring = true)
                        .assertContentDescriptionContains(HumanReadable.fileSize(file.size), substring = true)
                        .assertHasClickAction().performClick()
                }
            }
            runOnIdle { assertEquals(files + files, clicked) }
        }
}
