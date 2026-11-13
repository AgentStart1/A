/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.AppNav
import com.storyteller_f.alphaheart.app.common.HomeScreen
import com.storyteller_f.alphaheart.app.common.SignSessionScreen
import com.storyteller_f.alphaheart.app.common.TopicScreen
import com.storyteller_f.alphaheart.app.common.newAppNav
import com.storyteller_f.alphaheart.app.components.SignInPrompt
import com.storyteller_f.alphaheart.app.components.SignInRequiredContent
import com.storyteller_f.alphaheart.app.go_to_sign_in
import com.storyteller_f.alphaheart.app.need_sign_in
import com.storyteller_f.alphaheart.app.sign_in
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.shared.model.FileInfo
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalTestApi::class)
class SignInPromptUiTest {
    @Test
    fun protectedActionWaitsForConfirmation() =
        runComposeUiTest {
            val title = getString(Res.string.need_sign_in)
            val confirm = getString(Res.string.go_to_sign_in)
            val stack = NavBackStack<NavKey>(HomeScreen, TopicScreen(1))
            val shown = mutableStateOf(false)
            val nav = newAppNav(stack, isSignedIn = { false }, onSignInRequired = { shown.value = true })
            setContent { SignInPromptFixture(nav, shown) }

            onNodeWithText("Open attachment").performClick()
            onNodeWithText(title).assertIsDisplayed()
            runOnIdle { assertEquals(listOf(HomeScreen, TopicScreen(1)), stack.toList()) }
            onNodeWithText(confirm).performClick()

            onNodeWithText(title).assertDoesNotExist()
            runOnIdle {
                assertFalse(shown.value)
                assertEquals(listOf(HomeScreen, TopicScreen(1), SignSessionScreen), stack.toList())
            }
        }

    @Test
    fun cancelKeepsTheSourceTopic() =
        runComposeUiTest {
            val title = getString(Res.string.need_sign_in)
            lateinit var cancel: String
            val stack = NavBackStack<NavKey>(HomeScreen, TopicScreen(1))
            val shown = mutableStateOf(false)
            val nav = newAppNav(stack, isSignedIn = { false }, onSignInRequired = { shown.value = true })
            setContent {
                cancel = CoreStrings.cancel()
                SignInPromptFixture(nav, shown)
            }

            onNodeWithText("Open attachment").performClick()
            onNodeWithText(cancel).performClick()

            onNodeWithText(title).assertDoesNotExist()
            runOnIdle { assertEquals(listOf(HomeScreen, TopicScreen(1)), stack.toList()) }
        }

    @Test
    fun guestPreviewButtonOpensSigninDirectly() =
        runComposeUiTest {
            val signIn = getString(Res.string.sign_in)
            val stack = NavBackStack<NavKey>(HomeScreen, TopicScreen(1))
            var promptCount = 0
            var previewCompositions = 0
            val nav = newAppNav(stack, isSignedIn = { false }, onSignInRequired = { promptCount++ })
            setContent {
                MaterialTheme {
                    SignInRequiredContent(isSignedIn = false, onSignIn = nav::gotoSignIn) {
                        previewCompositions++
                        Text("Preview content")
                    }
                }
            }

            onNodeWithText("Preview content").assertDoesNotExist()
            onNodeWithText(signIn).performClick()

            runOnIdle {
                assertEquals(0, previewCompositions)
                assertEquals(0, promptCount)
                assertEquals(listOf(HomeScreen, TopicScreen(1), SignSessionScreen), stack.toList())
            }
        }

    @Test
    fun signedInPreviewShowsContent() =
        runComposeUiTest {
            val signIn = getString(Res.string.sign_in)
            setContent {
                MaterialTheme {
                    SignInRequiredContent(isSignedIn = true, onSignIn = {}) { Text("Preview content") }
                }
            }

            onNodeWithText("Preview content").assertIsDisplayed()
            onNodeWithText(signIn).assertDoesNotExist()
        }

    @Test
    fun signingOutHidesPreviewContent() =
        runComposeUiTest {
            val signIn = getString(Res.string.sign_in)
            val signedIn = mutableStateOf(true)
            setContent {
                MaterialTheme {
                    SignInRequiredContent(isSignedIn = signedIn.value, onSignIn = {}) { Text("Preview content") }
                }
            }
            onNodeWithText("Preview content").assertIsDisplayed()

            runOnIdle { signedIn.value = false }

            onNodeWithText("Preview content").assertDoesNotExist()
            onNodeWithText(signIn).assertIsDisplayed()
        }
}

@Composable
private fun SignInPromptFixture(nav: AppNav, shown: MutableState<Boolean>) {
    MaterialTheme {
        Button({ nav.gotoMedia(FileInfo.EMPTY.copy(id = 42)) }) { Text("Open attachment") }
        SignInPrompt(shown.value, { shown.value = false }) {
            assertFalse(shown.value)
            nav.gotoSignIn()
        }
    }
}
