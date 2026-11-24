/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.go_to_sign_in
import com.storyteller_f.alphaheart.app.need_sign_in
import com.storyteller_f.alphaheart.app.read_sign_in_prompt
import com.storyteller_f.alphaheart.app.sign_in_required_message
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogInternal
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SignInPrompt(shown: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (!shown) return
    CustomAlertDialogInternal(
        CustomAlertDialogState(
            title = stringResource(Res.string.need_sign_in),
            message = stringResource(Res.string.sign_in_required_message),
            positiveButton = stringResource(Res.string.go_to_sign_in),
            negativeButton = CoreStrings.cancel(),
        ),
        onDismiss,
        onConfirm,
    )
}

@Composable
internal fun SignInRequiredContent(isSignedIn: Boolean, onSignIn: () -> Unit, content: @Composable () -> Unit) {
    if (isSignedIn) {
        content()
    } else {
        CenterBox {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(Res.string.read_sign_in_prompt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                SignInButton(onSignIn)
            }
        }
    }
}
