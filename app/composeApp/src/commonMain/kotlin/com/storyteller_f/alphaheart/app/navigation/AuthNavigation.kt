/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("com.storyteller_f.alphaheart.app.pages.user.SignIn")
@Serializable
data object SignIn : NavKey

@SerialName("com.storyteller_f.alphaheart.app.pages.user.SignUp")
@Serializable
data object SignUp : NavKey

@SerialName("com.storyteller_f.alphaheart.app.pages.user.SignUpInput")
@Serializable
data object SignUpInput : NavKey

@SerialName("com.storyteller_f.alphaheart.app.pages.user.SignInInput")
@Serializable
data object SignInInput : NavKey

internal fun buildSignInAndSignUpNav(backStack: NavBackStack<NavKey>) =
    object : SignInAndSignUpNav {
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
    fun gotoSignUp()

    fun gotoSignIn()
}
