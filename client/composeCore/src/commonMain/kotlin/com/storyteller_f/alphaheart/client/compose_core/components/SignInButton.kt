/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.sign_in
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignInButton(onClick: () -> Unit = {}) {
    val signIn = stringResource(Res.string.sign_in)
    Button({
        onClick()
    }, modifier = Modifier.appiumSemantics(testTag = "sign_in", text = signIn)) {
        Icon(Icons.AutoMirrored.Default.Login, signIn)
        Spacer(modifier = Modifier.width(10.dp))
        Text(signIn)
    }
}
