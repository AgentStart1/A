/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.close
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAdaptiveOverlay
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadingSheet(
    title: String,
    onDismiss: () -> Unit,
    size: ReadingOverlaySize = ReadingOverlaySize.LIST,
    content: @Composable ColumnScope.() -> Unit,
) {
    val close = stringResource(Res.string.close)
    ReadingAdaptiveOverlay(onDismiss, size) {
        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 12.dp).semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                ReadingIconButton(onDismiss) {
                    ReadingIcon(actionIcon(ActionGlyph.X), close)
                }
            }
            content()
        }
    }
}
