/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProductionSemanticsBrowserTest {
    @Test
    fun bridgePayloadDoesNotReplaceOrDuplicateVisibleText() =
        runAppComposeUiTest {
            val payload = "bridge-only-hash raw **markdown**"
            var clicks = 0
            val click: () -> Unit = { clicks++ }
            setContent {
                MaterialTheme {
                    Column(
                        Modifier.clickable(onClick = click).appiumSemantics(
                            testTag = "public-topic",
                            text = payload,
                            onClick = click,
                        ),
                    ) {
                        Text("Visible author")
                        Text("Visible topic")
                    }
                }
            }
            onNodeWithTag("public-topic").assertTextEquals("Visible author", "Visible topic")
            onNodeWithContentDescription(payload).assertDoesNotExist()
            onNodeWithTag("public-topic").performClick()
            runOnIdle { assertEquals(1, clicks) }
        }

    @Test
    fun genuineAccessibilityDescriptionSurvivesWithoutBridgeText() =
        runAppComposeUiTest {
            setContent {
                MaterialTheme {
                    Column(
                        Modifier.clickable {}.appiumSemantics(
                            testTag = "room-navigation",
                            description = "Rooms, 3 unread messages",
                            text = "bridge-only-room-id",
                        ),
                    ) {
                        Text("Rooms")
                    }
                }
            }
            onNodeWithTag("room-navigation").assertContentDescriptionEquals("Rooms, 3 unread messages")
            onNodeWithTag("room-navigation").assertTextEquals("Rooms")
            onNodeWithContentDescription("bridge-only-room-id").assertDoesNotExist()
        }
}
