/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.storyteller_f.alphaheart.app.navigation.SignIn
import com.storyteller_f.alphaheart.app.navigation.SignInAndSignUpNav
import com.storyteller_f.alphaheart.app.navigation.SignInInput
import com.storyteller_f.alphaheart.app.navigation.SignUp
import com.storyteller_f.alphaheart.app.navigation.SignUpInput
import com.storyteller_f.alphaheart.app.navigation.buildSignInAndSignUpNav
import com.storyteller_f.alphaheart.app.utils.appPlatform
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.BindBrowserNavigation
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

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
    val goBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            appNavFactory.newAppNav().back()
        }
    }
    Surface {
        SignInNavDisplay(backStack, nav) {
            if (!appPlatform.hasNativeBack) AuthBackButton(goBack)
        }
    }
}

@Composable
private fun AuthBackButton(onClick: () -> Unit) {
    val description = stringResource(Res.string.back_to_pre_page)
    IconButton(onClick, Modifier.appiumSemantics(description = description, onClick = onClick)) {
        Icon(actionIcon(ActionGlyph.ARROW_LEFT), description)
    }
}

@Composable
private fun SignInNavDisplay(
    backStack: NavBackStack<NavKey>,
    nav: SignInAndSignUpNav,
    navigation: @Composable () -> Unit,
) {
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
                SignInPage(nav, navigation)
            }
            entry<SignUp> {
                SignUpPage(nav, navigation)
            }
            entry<SignUpInput> {
                SignUpPage(nav, navigation)
            }
            entry<SignInInput> {
                SignInPage(nav, navigation)
            }
        },
    )
}
