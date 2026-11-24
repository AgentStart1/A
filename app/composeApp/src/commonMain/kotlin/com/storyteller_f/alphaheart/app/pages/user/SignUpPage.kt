/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.go_to_sign_in
import com.storyteller_f.alphaheart.app.navigation.SignInAndSignUpNav
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

@Composable
fun SignUpPage(signInAndSignUpNav: SignInAndSignUpNav, navigation: (@Composable () -> Unit)? = null) {
    PrivateKeyAuthSignUpPage(navigation) {
        TextButton(
            onClick = signInAndSignUpNav::gotoSignIn,
            modifier = Modifier.fillMaxWidth().appiumSemantics(text = stringResource(Res.string.go_to_sign_in)),
        ) {
            Text(stringResource(Res.string.go_to_sign_in))
        }
    }
}
