/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon

@Composable
fun <T> ReadingSelect(
    label: String,
    selected: T,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            ReadingOutlinedButton(
                { expanded = true },
                Modifier.fillMaxWidth().heightIn(min = 46.dp),
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
            ) {
                Text(options.firstOrNull { it.first == selected }?.second.orEmpty())
                Spacer(Modifier.weight(1f))
                ReadingIcon(actionIcon(ActionGlyph.CHEVRON_DOWN), null)
            }
            DropdownMenu(expanded, { expanded = false }) {
                options.forEach { (value, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            expanded = false
                            onSelect(value)
                        },
                    )
                }
            }
        }
    }
}
