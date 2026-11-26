/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.sign_in
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun SignInButton(onClick: () -> Unit = {}) {
    val signIn = stringResource(Res.string.sign_in)
    Button({
        onClick()
    }, modifier = Modifier.appiumSemantics(testTag = "sign_in", text = signIn)) {
        Icon(actionIcon(ActionGlyph.LOG_IN_RTL), signIn)
        Spacer(modifier = Modifier.width(10.dp))
        Text(signIn)
    }
}
