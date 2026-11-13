/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StringListPreference(
    key: String,
    defaultValue: String,
    title: String,
    items: Map<String, String>,
    summary: @Composable (String) -> Unit,
    leadingIcon: @Composable () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val preferences = LocalAppPreferences.current
    val currentValue by rememberStringPreference(key, defaultValue)
    val scope = rememberCoroutineScope()
    var showOptions by remember {
        mutableStateOf(false)
    }

    ListItem(
        headlineContent = {
            Text(title)
        },
        modifier =
        Modifier.fillMaxWidth().clickable {
            showOptions = true
        },
        supportingContent = {
            summary(currentValue)
        },
        leadingContent = leadingIcon,
        trailingContent = trailingContent,
    )

    if (showOptions) {
        ModalBottomSheet(
            onDismissRequest = {
                showOptions = false
            },
        ) {
            Column {
                items.forEach { (label, value) ->
                    ListItem(
                        headlineContent = {
                            Text(label)
                        },
                        leadingContent = {
                            RadioButton(
                                selected = currentValue == value,
                                onClick = null,
                            )
                        },
                        modifier =
                        Modifier.fillMaxWidth().clickable {
                            showOptions = false
                            scope.launch {
                                preferences.setString(key, value)
                            }
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
