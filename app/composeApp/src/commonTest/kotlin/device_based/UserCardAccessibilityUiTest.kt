/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.common.HomeScreen
import com.storyteller_f.alphaheart.app.common.UserScreen
import com.storyteller_f.alphaheart.app.common.newAppNav
import com.storyteller_f.alphaheart.app.pages.user.UserCardContainer
import com.storyteller_f.shared.model.UserInfo
import kotlin.test.Test
import kotlin.test.assertEquals

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
            onNodeWithTag("user-dialog-cell").assertContentDescriptionEquals("Reader, ad: synthetic")
            onNodeWithContentDescription("user-dialog-cell").assertDoesNotExist()
        }
}
