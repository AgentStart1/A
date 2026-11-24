/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.utils.LocalAppPreferences
import com.storyteller_f.alphaheart.app.utils.rememberStringPreference
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import kotlinx.coroutines.launch

@Composable
internal fun StringListPreference(
    key: String,
    defaultValue: String,
    title: String,
    items: Map<String, String>,
    trailingContent: @Composable () -> Unit,
) {
    val preferences = LocalAppPreferences.current
    val currentValue by rememberStringPreference(key, defaultValue)
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        ReadingTextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(items[currentValue] ?: items[defaultValue].orEmpty(), Modifier.weight(1f))
            ReadingIcon(actionIcon(if (expanded) ActionGlyph.CHEVRON_UP else ActionGlyph.CHEVRON_DOWN), null)
        }
        if (expanded) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (value, label) ->
                    ReadingTab(selected = currentValue == value, onClick = {
                        expanded = false
                        scope.launch { preferences.setString(key, value) }
                    }) { Text(label) }
                }
            }
        }
        trailingContent()
    }
}
