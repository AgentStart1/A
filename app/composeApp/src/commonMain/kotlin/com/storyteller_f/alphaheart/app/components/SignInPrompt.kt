/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.go_to_sign_in
import com.storyteller_f.alphaheart.app.need_sign_in
import com.storyteller_f.alphaheart.app.sign_in_required_message
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogInternal
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogState
import com.storyteller_f.alphaheart.client.compose_core.components.SignInButton
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
        CenterBox { SignInButton(onSignIn) }
    }
}
