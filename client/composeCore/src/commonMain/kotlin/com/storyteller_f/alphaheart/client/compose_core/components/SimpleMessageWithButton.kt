/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.copy
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.alphaheart.client.compose_core.view_full_value
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun SimpleMessageWithButton(message: String, title: String) {
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    ExpandableFieldValue(message, title) { clipboard.copyText(it, toast) }
}

/** The field row owns copy work so dismissing its read-only dialog does not cancel the write. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpandableFieldValue(message: String, title: String, copy: suspend (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val viewFullValue = stringResource(Res.string.view_full_value)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            message,
            modifier =
            Modifier.weight(
                1f,
            ).semantics {
                this[SemanticsProperties.ContentDescription] =
                    listOf(
                        "$title: $message",
                        viewFullValue,
                    )
            }.clickable { expanded = true },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 19.2.sp,
        )
        IconButton({ scope.launch { copy(message) } }, Modifier.size(34.dp)) {
            Icon(actionIcon(ActionGlyph.COPY), stringResource(Res.string.copy))
        }
    }
    if (expanded) {
        BasicAlertDialog({ expanded = false }) {
            FieldValueDialogContent(title, message, { expanded = false }) {
                expanded = false
                scope.launch { copy(message) }
            }
        }
    }
}

@Composable
internal fun FieldValueDialogContent(title: String, message: String, dismiss: () -> Unit, copy: () -> Unit) {
    DialogContainer(onDismissRequest = dismiss) {
        Text(
            title,
            modifier = Modifier.padding(end = 40.dp, bottom = 20.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        Box(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            SelectionContainer { Text(message) }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(copy) { Text(stringResource(Res.string.copy)) }
        }
    }
}
