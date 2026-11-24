/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.app_name
import com.storyteller_f.alphaheart.app.common.SearchScreen
import com.storyteller_f.alphaheart.app.search
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
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
    subtitle: String? = null,
) {
    val home = scope == SearchScope.World || scope == SearchScope.MyCommunity || scope == SearchScope.MyRoom
    val nav = LocalAppNavFactory.current
    val searchLabel = stringResource(Res.string.search)
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            BoxWithConstraints {
                val compact = maxWidth <= 600.dp
                val searchWidth =
                    when {
                        !compact -> 150.dp
                        home -> 85.dp
                        searchLabel.length > 3 -> 85.dp
                        else -> 64.dp
                    }
                val compactHome = home && compact
                Row(
                    Modifier.fillMaxWidth().padding(
                        horizontal = if (compactHome) 14.dp else 16.dp,
                        vertical = if (compactHome) 20.dp else 12.dp,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compactHome) 10.dp else 12.dp),
                ) {
                    if (!home) {
                        IconButton({ nav.newAppNav().back() }) { Icon(actionIcon(ActionGlyph.ARROW_LEFT), "back") }
                    }
                    leadingIcon()
                    ReadingToolbarTitle(
                        title = if (home) stringResource(Res.string.app_name) else title,
                        subtitle = subtitle,
                        modifier = Modifier.weight(1f),
                    )
                    ReadingToolbarSearchButton(searchLabel, searchWidth, compact) {
                        nav.newAppNav().backStack.add(SearchScreen(scope))
                    }
                    selfIcon()
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ReadingToolbarSearchButton(label: String, width: Dp, compact: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 12.dp, vertical = 8.dp),
        modifier = Modifier.width(width).appiumSemantics(description = "search query", onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(actionIcon(ActionGlyph.SEARCH), null, Modifier.size(if (compact) 16.dp else 18.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
        }
    }
}

@Composable
private fun ReadingToolbarTitle(title: String?, subtitle: String?, modifier: Modifier) {
    Column(modifier) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!subtitle.isNullOrBlank() && subtitle != title) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
