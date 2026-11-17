/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserOverview
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class UserMenuCountTilesTest {
    @Test
    fun originalOverviewCountsAndDestinationsRemainAvailableWithoutASeparateStatPanel() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            for (compact in listOf(false, true)) {
                runComposeUiTest {
                    var loading by mutableStateOf(false)
                    val clicked = mutableListOf<String>()
                    val overview = UserOverview(13, 11, 7, 0, 17, 19, userInfo = UserInfo.EMPTY)
                    setContent {
                        MaterialTheme {
                            ReadingInteractions {
                                Box(Modifier.width(if (compact) 390.dp else 800.dp)) {
                                    ReadingActionMenuSurface({}, { Text("User") }, compact) {
                                        IconActionMenu {
                                            UserOverviewActionTiles(
                                                overview,
                                                loading,
                                                { clicked += "favorite" },
                                                { clicked += "subscription" },
                                                { clicked += "reaction" },
                                                { clicked += "comment" },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val labels =
                        listOf(
                            "ACG, 7",
                            "My favorites, 11",
                            "My subscriptions, 13",
                            "My reactions, 17",
                            "My comments, 19",
                        )
                    val bounds = labels.map { onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }
                    for (value in listOf("7", "11", "13", "17", "19")) {
                        onNodeWithText(value, useUnmergedTree = true).assertIsDisplayed()
                    }
                    runOnIdle { loading = true }
                    labels.forEachIndexed { index, label ->
                        assertEquals(bounds[index], onNodeWithContentDescription(label).getUnclippedBoundsInRoot())
                    }
                    runOnIdle { loading = false }
                    labels.forEach { onNodeWithContentDescription(it).performClick() }
                    runOnIdle { assertEquals(listOf("favorite", "subscription", "reaction", "comment"), clicked) }
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
