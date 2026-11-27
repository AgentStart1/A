/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics

@Composable
internal fun ActionMenuReference(
    name: String,
    aid: String,
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(vertical = 2.dp),
    icon: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .appiumSemantics(description = name, text = name, onClick = onClick).padding(contentPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            if (aid.isNotBlank()) Text(CoreStrings.aid(aid), style = MaterialTheme.typography.bodySmall)
        }
        ReadingIcon(actionIcon(ActionGlyph.CHEVRON_RIGHT), null, Modifier.align(Alignment.CenterVertically).size(17.dp))
    }
}

@Composable
internal fun ActionMenuOrigin(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
