/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.settings_edit_value
import com.storyteller_f.alphaheart.app.settings_reset_value
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadingSettingsLayout(
    modifier: Modifier = Modifier,
    maxContentWidth: Dp = 740.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.padding(horizontal = 20.dp).widthIn(max = maxContentWidth).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun ReadingSettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
fun SettingOptionView(
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    content: @Composable () -> Unit,
) {
    SettingRow(title, Modifier.clickable(onClick = onClick), showDivider) { content() }
}

@Composable
internal fun SettingValueRow(title: String, value: String, onEdit: (() -> Unit)? = null, showDivider: Boolean = true) {
    SettingRow(title, showDivider = showDivider) {
        Text(value, Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium)
        if (onEdit != null) SettingEditButton(title, onEdit)
    }
}

@Composable
fun SettingOptionResettableView(
    title: String,
    supportReset: Boolean,
    onClick: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    SettingRow(title) {
        Box(Modifier.weight(1f, fill = false)) { content() }
        SettingEditButton(title) { onClick(false) }
        if (supportReset) {
            val label = stringResource(Res.string.settings_reset_value, title)
            val reset = { onClick(true) }
            ReadingIconButton(reset, Modifier.appiumSemantics(description = label, onClick = reset)) {
                ReadingIcon(actionIcon(ActionGlyph.ROTATE_CCW), label)
            }
        }
    }
}

@Composable
private fun SettingEditButton(title: String, onEdit: () -> Unit) {
    val label = stringResource(Res.string.settings_edit_value, title)
    ReadingIconButton(onEdit, Modifier.appiumSemantics(description = label, onClick = onEdit)) {
        ReadingIcon(actionIcon(ActionGlyph.PEN_LINE), label)
    }
}

@Composable
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    trailing: @Composable RowScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 55.dp).padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Row(
                Modifier.weight(1.25f),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing,
            )
        }
        if (showDivider) HorizontalDivider()
    }
}
