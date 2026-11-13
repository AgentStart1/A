/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.go_to_sign_in
import com.storyteller_f.alphaheart.app.private_key
import com.storyteller_f.alphaheart.app.sign_up
import com.storyteller_f.alphaheart.app.sign_up_subtitle
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpPage(signInAndSignUpNav: SignInAndSignUpNav) {
    AuthPageChrome(
        title = stringResource(Res.string.sign_up),
        subtitle = stringResource(Res.string.sign_up_subtitle),
        footer = {
            TextButton(
                onClick = { signInAndSignUpNav.gotoSignIn() },
                modifier = Modifier.appiumSemantics(text = stringResource(Res.string.go_to_sign_in)),
            ) {
                Text(stringResource(Res.string.go_to_sign_in))
            }
        },
    ) {
        Column {
            Button(
                onClick = { signInAndSignUpNav.gotoPrivateKey(true) },
                modifier =
                Modifier.fillMaxWidth().appiumSemantics(
                    testTag = "private_key",
                    text = stringResource(Res.string.private_key),
                ),
                shape = ButtonDefaults.shape,
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Text(stringResource(Res.string.private_key), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
