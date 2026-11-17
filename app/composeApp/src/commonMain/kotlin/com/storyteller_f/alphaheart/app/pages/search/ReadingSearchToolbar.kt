/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.app_name
import com.storyteller_f.alphaheart.app.common.SearchScreen
import com.storyteller_f.alphaheart.app.search
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton as OutlinedButton

@Composable
internal fun ReadingSearchToolbar(
    scope: SearchScope,
    title: String?,
    leadingIcon: @Composable () -> Unit,
    selfIcon: @Composable () -> Unit,
) {
    val home = scope == SearchScope.World || scope == SearchScope.MyCommunity || scope == SearchScope.MyRoom
    val nav = LocalAppNavFactory.current
    val searchLabel = stringResource(Res.string.search)
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            BoxWithConstraints {
                val compactSearchWidth = if (searchLabel.length > 3) 104.dp else 88.dp
                val searchWidth = if (maxWidth < 600.dp) compactSearchWidth else 188.dp
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!home) {
                        IconButton({ nav.newAppNav().back() }) { Icon(actionIcon(ActionGlyph.ARROW_LEFT), "back") }
                    }
                    leadingIcon()
                    if (home || title != null) {
                        Text(
                            if (home) stringResource(Res.string.app_name) else title.orEmpty(),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                    OutlinedButton(
                        onClick = { nav.newAppNav().backStack.add(SearchScreen(scope)) },
                        modifier =
                        Modifier.width(searchWidth).appiumSemantics(
                            description = "search query",
                            onClick = { nav.newAppNav().backStack.add(SearchScreen(scope)) },
                        ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(actionIcon(ActionGlyph.SEARCH), null)
                            Text(stringResource(Res.string.search), maxLines = 1, textAlign = TextAlign.Start)
                        }
                    }
                    selfIcon()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
internal fun ReadingSearchPage(scope: SearchScope) {
    var query by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val nav = LocalAppNavFactory.current
    SearchPageLayout(
        query = query,
        searchQuery = searchQuery,
        updateQuery = { query = it },
        submit = { searchQuery = query },
        close = { nav.newAppNav().back() },
        scopeContent = { SearchScopeContext(scope) { SearchScopeIdentity(scope) } },
        results = { SearchContent(scope, searchQuery) },
    )
}
