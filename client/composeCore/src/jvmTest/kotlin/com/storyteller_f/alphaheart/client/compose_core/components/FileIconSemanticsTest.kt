/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.ObjectType
import kotlinx.datetime.LocalDateTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class FileIconSemanticsTest {
    @Test
    fun decorativeMediaIconsDoNotReplaceMergedFileNamesAndMetadata() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
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
                val clicked = mutableListOf<Long>()
                setContent {
                    MaterialTheme {
                        ReadingInteractions {
                            Column {
                                files.forEach { file ->
                                    ListItem(
                                        headlineContent = { Text(file.name) },
                                        overlineContent = { Text(file.contentType) },
                                        supportingContent = { Text("1024 bytes") },
                                        leadingContent = { FileIcon(file) },
                                        modifier = Modifier.clickable { clicked.add(file.id) },
                                    )
                                }
                            }
                        }
                    }
                }
                for (file in files) {
                    onNodeWithText(file.name).assertHasClickAction()
                        .assertTextContains(file.contentType).assertTextContains("1024 bytes").performClick()
                }
                for (generic in listOf("audio file", "video file", "other file")) {
                    onNodeWithContentDescription(generic).assertDoesNotExist()
                }
                runOnIdle { assertEquals(files.map { it.id }, clicked) }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
