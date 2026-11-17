/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.panel.pages.PanelFileReferenceList
import com.storyteller_f.shared.model.FileRefInfo
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class PanelFileReferencesTest {
    @Test
    fun oneFileCanRenderSeveralDistinctReferencesAndKeepEachTargetAcrossResize() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runComposeUiTest {
                val types = listOf(ObjectType.USER, ObjectType.COMMUNITY, ObjectType.ROOM, ObjectType.TOPIC)
                val references =
                    types.mapIndexed { index, type ->
                        FileRefInfo(index + 1L, index + 11L, type, 1, "same-file", 55)
                    }
                val values = flowOf(PagingData.from(references))
                val width = mutableStateOf(800.dp)
                val activated = mutableListOf<FileRefInfo>()
                var presented: LazyPagingItems<FileRefInfo>? = null
                setContent {
                    MaterialTheme {
                        ReadingInteractions {
                            val items = values.collectAsLazyPagingItems()
                            presented = items
                            Box(Modifier.width(width.value).height(600.dp)) {
                                PanelFileReferenceList(items) { activated.add(it) }
                            }
                        }
                    }
                }
                waitUntil { presented?.itemCount == references.size }
                for (nextWidth in listOf(800.dp, 390.dp)) {
                    runOnIdle { width.value = nextWidth }
                    onAllNodesWithText("#", substring = true).assertCountEquals(4)
                    for (text in listOf("User #11", "Community #12", "Room #13", "Topic #14")) {
                        onNodeWithText(text).assertIsDisplayed().performClick()
                    }
                }
                runOnIdle { assertEquals(references + references, activated) }
            }
        } finally {
            Dispatchers.resetMain()
            Locale.setDefault(originalLocale)
        }
    }
}
