/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.common.HomeScreen
import com.storyteller_f.alphaheart.app.common.UserScreen
import com.storyteller_f.alphaheart.app.common.newAppNav
import com.storyteller_f.alphaheart.app.pages.room.RoomRowContent
import com.storyteller_f.alphaheart.app.pages.user.UnboundedUserCell
import com.storyteller_f.alphaheart.app.pages.user.UserCard
import com.storyteller_f.alphaheart.app.pages.user.UserCardContainer
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.UserAddressLine
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.UserInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class UserCardAccessibilityUiTest {
    @Test
    fun userCardAnnouncesVisibleIdentityAndKeepsItsExistingNavigation() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(HomeScreen)
            val nav = newAppNav(stack, isSignedIn = { true }, onSignInRequired = {})
            val factory =
                object : AppNavFactory {
                    override fun newAppNav() = nav
                }
            val user =
                mutableStateOf(
                    UserInfo.EMPTY.copy(id = 14, nickname = "Reader", aid = "readable", address = "synthetic"),
                )
            var dismissed = 0
            setContent {
                CompositionLocalProvider(LocalAppNavFactory provides factory) {
                    UserCardContainer(user.value, { dismissed++ }) { Text(user.value.nickname) }
                }
            }
            onNodeWithTag("user-dialog-cell").assertContentDescriptionEquals("Reader, aid: readable").performClick()
            assertEquals(UserScreen(14), stack.last())
            assertEquals(1, dismissed)
            runOnIdle {
                nav.back()
                user.value = user.value.copy(aid = null)
            }
            onNodeWithTag("user-dialog-cell").assertContentDescriptionEquals("Reader")
            onNodeWithContentDescription("user-dialog-cell").assertDoesNotExist()
        }

    @Test
    fun checkAddressPlacementOnNarrowScreen() = checkAddressPlacement(390.dp)

    @Test
    fun checkAddressPlacementOnWideScreen() = checkAddressPlacement(900.dp)

    private fun checkAddressPlacement(width: Dp) =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(HomeScreen)
            val factory =
                object : AppNavFactory {
                    override fun newAppNav() = newAppNav(stack, isSignedIn = { true }, onSignInRequired = {})
                }
            val address = "0123456789abcdef0123456789abcdef01234567"
            val user = mutableStateOf(UserInfo.EMPTY.copy(id = 14, nickname = "Reader", aid = null, address = address))
            val displayDialog = mutableStateOf(false)
            setContent {
                CompositionLocalProvider(LocalAppNavFactory provides factory) {
                    Box(Modifier.width(width)) {
                        if (displayDialog.value) {
                            ReadingActionMenuSurface({}, { UserCard(user.value) }, compact = width == 390.dp) {
                                UserAddressLine(user.value.address)
                                Text("Actions")
                            }
                        } else {
                            UnboundedUserCell(user.value)
                        }
                    }
                }
            }
            val label = onNodeWithText(address, useUnmergedTree = true)
            label.assertDoesNotExist()
            runOnIdle { user.value = user.value.copy(aid = "readable") }
            onNodeWithText("aid: readable", useUnmergedTree = true).assertIsDisplayed()
            label.assertDoesNotExist()
            runOnIdle { displayDialog.value = true }
            val header = onNodeWithTag("user-dialog-cell").getUnclippedBoundsInRoot()
            val bounds = label.assertIsDisplayed().getUnclippedBoundsInRoot()
            val actions = onNodeWithText("Actions").getUnclippedBoundsInRoot()
            assertTrue(bounds.top >= header.bottom)
            assertTrue(bounds.bottom <= actions.top)
            runOnIdle { user.value = user.value.copy(aid = null) }
            label.assertIsDisplayed()
            onNodeWithText("aid: readable", useUnmergedTree = true).assertDoesNotExist()
        }

    @Test
    fun checkRoomClickTargetsOnNarrowScreen() = checkRoomClickTargets(390.dp)

    @Test
    fun checkRoomClickTargetsOnWideScreen() = checkRoomClickTargets(900.dp)

    private fun checkRoomClickTargets(width: Dp) =
        runAppComposeUiTest {
            val room = RoomInfo.EMPTY.copy(name = "Rust Language")
            val community = CommunityInfo.EMPTY.copy(name = "Programming")
            var openedRoom = 0
            var openedCommunity = 0
            setContent {
                Box(Modifier.width(width)) {
                    RoomRowContent(
                        room,
                        community,
                        wide = false,
                        Modifier.clickable { openedRoom++ }.testTag("room-row"),
                        roomIcon = { RoomIcon(room, 48.dp, false) {} },
                        communityIcon = { CommunityIcon(community, 22.dp, true) { openedCommunity++ } },
                    )
                }
            }
            val icon = onNodeWithContentDescription("Programming", useUnmergedTree = true)
            icon.assertWidthIsEqualTo(22.dp).performTouchInput { click(center) }
            runOnIdle {
                assertEquals(1, openedCommunity)
                assertEquals(0, openedRoom)
            }
            onNodeWithTag("room-row").performTouchInput { click(Offset(24.dp.toPx(), center.y)) }
            runOnIdle {
                assertEquals(1, openedCommunity)
                assertEquals(1, openedRoom)
            }
        }
}
