/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.back_to_pre_page
import com.storyteller_f.alphaheart.app.utils.appPlatform
import com.storyteller_f.alphaheart.client.compose_core.utils.BindBrowserNavigation
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource

@Serializable
data object SignIn : NavKey

@Serializable
data object SignUp : NavKey

@Serializable
data object SignUpInput : NavKey

@Serializable
data object SignInInput : NavKey

@Composable
fun SignInAndSignUpPage() {
    val config =
        SavedStateConfiguration {
            serializersModule =
                SerializersModule {
                    polymorphic(NavKey::class) {
                        subclass(SignIn::class)
                        subclass(SignUp::class)
                        subclass(SignUpInput::class)
                        subclass(SignInInput::class)
                    }
                }
        }
    val backStack = rememberNavBackStack(config, SignIn)
    BindBrowserNavigation(backStack, config.serializersModule, "app/signin")
    val nav =
        remember {
            buildSignInAndSignUpNav(backStack)
        }
    val appNavFactory = LocalAppNavFactory.current
    Surface {
        Column {
            if (!appPlatform.hasNativeBack) {
                val backDescription = stringResource(Res.string.back_to_pre_page)
                val goBack: () -> Unit = {
                    if (backStack.size > 1) {
                        backStack.removeLastOrNull()
                    } else {
                        appNavFactory.newAppNav().back()
                    }
                }
                IconButton(
                    onClick = goBack,
                    modifier = Modifier.appiumSemantics(description = backDescription, onClick = goBack),
                ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, backDescription)
                }
            }
            SignInNavDisplay(backStack, nav)
        }
    }
}

@Composable
private fun SignInNavDisplay(backStack: NavBackStack<NavKey>, nav: SignInAndSignUpNav) {
    NavDisplay(
        backStack,
        entryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider =
        entryProvider {
            entry<SignIn> {
                SignInPage(nav)
            }
            entry<SignUp> {
                SignUpPage(nav)
            }
            entry<SignUpInput> {
                PrivateKeyAuthSignUpPage()
            }
            entry<SignInInput> {
                PrivateKeyAuthSignInPage()
            }
        },
    )
}

private fun buildSignInAndSignUpNav(backStack: NavBackStack<NavKey>) =
    object : SignInAndSignUpNav {
        override fun gotoPrivateKey(isSignUp: Boolean) {
            val target = if (isSignUp) SignUpInput else SignInInput
            val i = backStack.indexOf(target)
            if (i >= 0) {
                repeat(backStack.size - i - 1) {
                    backStack.removeLastOrNull()
                }
            }
            if (backStack.last() != target) {
                backStack.add(target)
            }
        }

        override fun gotoSignUp() {
            val i = backStack.indexOf(SignUp)
            if (i >= 0) {
                repeat(backStack.size - i - 1) {
                    backStack.removeLastOrNull()
                }
            }
            if (backStack.last() !is SignUp) {
                backStack.add(SignUp)
            }
        }

        override fun gotoSignIn() {
            val i = backStack.indexOf(SignIn)
            if (i >= 0) {
                repeat(backStack.size - i - 1) {
                    backStack.removeLastOrNull()
                }
            }
            if (backStack.last() !is SignIn) {
                backStack.add(SignIn)
            }
        }
    }

interface SignInAndSignUpNav {
    fun gotoPrivateKey(isSignUp: Boolean)

    fun gotoSignUp()

    fun gotoSignIn()
}
