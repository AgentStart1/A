/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_topic
import com.storyteller_f.alphaheart.app.create
import com.storyteller_f.alphaheart.app.create_community_action
import com.storyteller_f.alphaheart.app.create_room_action
import com.storyteller_f.alphaheart.app.create_title_action
import com.storyteller_f.alphaheart.app.pages.title.ComposeMenuContent
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.client.compose_core.utils.readingColorScheme
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class CreateMenuUiTest {
    @Test
    fun fourExistingCreationActionsUseCompactTilesAndCloseBeforeTheSingleSelectionAtBothWidths() =
        runAppComposeUiTest {
            val labels =
                linkedMapOf(
                    ObjectType.TOPIC to getString(Res.string.add_topic),
                    ObjectType.COMMUNITY to getString(Res.string.create_community_action),
                    ObjectType.ROOM to getString(Res.string.create_room_action),
                    ObjectType.TITLE to getString(Res.string.create_title_action),
                )
            val title = getString(Res.string.create)
            val width = mutableStateOf(390.dp)
            val shown = mutableStateOf(false)
            val events = mutableListOf<String>()
            setContent {
                CreateMenuFixture(
                    width.value,
                    shown.value,
                    onDismiss = {
                        shown.value = false
                        events += "close"
                    },
                    onCheck = {
                        assertFalse(shown.value)
                        events += it.name
                    },
                )
            }
            for (size in listOf(390.dp, 1280.dp)) {
                for ((type, label) in labels) {
                    runOnIdle {
                        width.value = size
                        shown.value = true
                        events.clear()
                    }
                    onNodeWithText(title).assertExists()
                    assertCreateMenuGeometry(labels.values.toList(), size <= 600.dp)
                    onNodeWithContentDescription(label).performClick()
                    onNodeWithTag("action-menu-surface").assertDoesNotExist()
                    assertEquals(listOf("close", type.name), events)
                }
            }
            runOnIdle {
                shown.value = true
                events.clear()
            }
            onNodeWithContentDescription(labels.getValue(ObjectType.TOPIC))
                .performSemanticsAction(SemanticsActions.OnClick) { click ->
                    click()
                    click()
                }
            assertEquals(listOf("close", ObjectType.TOPIC.name), events)
            runOnIdle {
                shown.value = true
                events.clear()
            }
            onNodeWithTag("action-menu-close").performClick()
            assertEquals(listOf("close"), events)
            onNodeWithTag("action-menu-surface").assertDoesNotExist()
        }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertCreateMenuGeometry(labels: List<String>, compact: Boolean) {
    val surface = onNodeWithTag("action-menu-surface")
    surface.assertWidthIsEqualTo(320.dp).assertHeightIsEqualTo(if (compact) 153.dp else 157.dp)
    val close = onNodeWithTag("action-menu-close").getUnclippedBoundsInRoot()
    val bounds =
        labels.map { label ->
            val tile = onNodeWithContentDescription(label)
            tile.assertHeightIsEqualTo(52.dp)
            // Accessible text and a popup tooltip are intentional; an inline painted caption is not.
            val inlineCaption =
                hasText(label) and
                    SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and
                    hasAnyAncestor(hasContentDescription(label) and hasClickAction())
            onAllNodes(inlineCaption, useUnmergedTree = true).assertCountEquals(0)
            onNode(hasContentDescription(label) and !hasClickAction(), useUnmergedTree = true)
                .assertWidthIsEqualTo(16.dp).assertHeightIsEqualTo(16.dp)
            tile.performMouseInput {
                enter(center)
                exit()
            }
            val pixels = tile.captureToImage().toPixelMap()
            assertEquals(Color.White, pixels[pixels.width / 2, pixels.height - 5])
            tile.getUnclippedBoundsInRoot()
        }
    bounds.forEachIndexed { index, rect ->
        assertEquals(34.dp, rect.top - close.bottom)
        if (index > 0) assertEquals(12.dp, rect.left - bounds[index - 1].right)
    }
    val container = surface.getUnclippedBoundsInRoot()
    assertEquals(if (compact) 19.dp else 21.dp, container.bottom - bounds.last().bottom)
    assertEquals(5, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
}

@Composable
private fun CreateMenuFixture(width: Dp, shown: Boolean, onDismiss: () -> Unit, onCheck: (ObjectType) -> Unit) {
    MaterialTheme(colorScheme = readingColorScheme(false), typography = readingTypography) {
        ReadingInteractions {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(width, 600.dp),
                ) {
                    if (shown) ComposeMenuContent(onDismiss, onCheck, compact = width <= 600.dp)
                }
            }
        }
    }
}
