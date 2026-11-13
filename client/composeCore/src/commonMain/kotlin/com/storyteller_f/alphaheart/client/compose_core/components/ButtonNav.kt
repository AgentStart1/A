/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics

@Composable
fun ButtonNav(icon: ImageVector, title: String, semanticDescription: String? = null, onClick: () -> Unit = {}) {
    ButtonNav(IconRes.Vector(icon), title, semanticDescription = semanticDescription, onClick = onClick)
}

@Composable
fun ButtonNav(icon: Char, title: String, semanticDescription: String? = null, onClick: () -> Unit = {}) {
    ButtonNav(IconRes.Font(icon), title, semanticDescription = semanticDescription, onClick = onClick)
}

@Composable
fun ButtonNav(
    icon: IconRes,
    title: String,
    suffix: @Composable () -> Unit = {},
    semanticDescription: String? = null,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(8.dp)
    val semanticsModifier =
        Modifier.appiumSemantics(
            description = semanticDescription,
            text = title,
            onClick = onClick,
        )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
        Modifier.fillMaxWidth().then(semanticsModifier).clip(shape).clickable {
            onClick()
        }.padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        CustomIcon(icon)
        Text(title)
        Spacer(modifier = Modifier.weight(1f))
        suffix()
    }
}
