/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class UserIconSemanticsTest {
    @Test
    fun decorativeAvatarDoesNotReplaceTheClickableUsersName() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    Row(Modifier.clickable {}) {
                        UserIcon(false, null) {}
                        Text("Reader")
                    }
                }
            }
            onNodeWithText("Reader").assertHasClickAction()
            onNodeWithContentDescription("avatar").assertDoesNotExist()
        }
}
