/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.search
import com.storyteller_f.alphaheart.app.search_start_hint
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

/** Search states occupy one results pane, and the result source owns its refresh indicator. */
@Composable
fun SearchPageLayout(
    query: String,
    searchQuery: String,
    updateQuery: (String) -> Unit,
    submit: () -> Unit,
    close: () -> Unit,
    scopeContent: @Composable () -> Unit,
    results: @Composable () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 960.dp).fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(close) { Icon(actionIcon(ActionGlyph.ARROW_LEFT), "back") }
                    Text(stringResource(Res.string.search), style = MaterialTheme.typography.headlineSmall)
                }
                scopeContent()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = updateQuery,
                        singleLine = true,
                        modifier =
                        Modifier.weight(1f).appiumSemantics(
                            description = "search query",
                            input = true,
                            inputValue = query,
                            onInputValueChange = updateQuery,
                            onInputSubmit = submit,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() }),
                        placeholder = { Text(stringResource(Res.string.search)) },
                    )
                    Button(submit) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(actionIcon(ActionGlyph.SEARCH), null)
                            Text(stringResource(Res.string.search))
                        }
                    }
                }
                HorizontalDivider()
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 20.dp)) {
                    if (searchQuery.isBlank()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(Res.string.search_start_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        results()
                    }
                }
            }
        }
    }
}

/** Result tabs occupy their own row instead of sharing the result box's origin. */
@Composable
internal fun SearchResultsLayout(filters: @Composable () -> Unit, results: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        filters()
        Box(Modifier.weight(1f).fillMaxWidth()) { results() }
    }
}
