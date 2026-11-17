/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class ReadingControlIconTest {
    @Test
    fun standardControlsUseSeventeenWhileExplicitMenuGlyphSizeIsPreserved() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    Row {
                        ReadingIcon(actionIcon(ActionGlyph.SEARCH), null, Modifier.testTag("standard-search"))
                        ReadingIcon(actionIcon(ActionGlyph.X), null, Modifier.testTag("standard-close"))
                        ReadingIcon(actionIcon(ActionGlyph.COPY), null, Modifier.size(21.dp).testTag("menu-copy"))
                    }
                }
            }
            onNodeWithTag("standard-search").assertWidthIsEqualTo(17.dp).assertHeightIsEqualTo(17.dp)
            onNodeWithTag("standard-close").assertWidthIsEqualTo(17.dp).assertHeightIsEqualTo(17.dp)
            onNodeWithTag("menu-copy").assertWidthIsEqualTo(21.dp).assertHeightIsEqualTo(21.dp)
        }
}
