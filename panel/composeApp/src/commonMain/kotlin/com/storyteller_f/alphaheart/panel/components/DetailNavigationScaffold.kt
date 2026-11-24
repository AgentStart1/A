/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.navigation.navigationSpacing
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.launch
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingHorizontalPager as HorizontalPager

/** The same pager owns the selected section on both sides of the responsive breakpoint. */
@Composable
fun DetailNavigationScaffold(
    pagerState: PagerState,
    routes: List<NavRoute>,
    topBar: @Composable () -> Unit,
    content: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val select: (String) -> Unit = { path ->
        val index = routes.indexOfFirst { it.path == path }
        if (index >= 0) scope.launch { pagerState.scrollToPage(index) }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth <= 600.dp
        Scaffold(
            topBar = topBar,
            bottomBar = {
                if (compact) {
                    DetailBottomNav(routes[pagerState.currentPage].path, routes, select)
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (!compact) DetailRail(routes[pagerState.currentPage].path, routes, select)
                HorizontalPager(pagerState, Modifier.weight(1f).fillMaxSize()) { content(it) }
            }
        }
    }
}

@Composable
private fun DetailRail(path: String, routes: List<NavRoute>, select: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.width(84.dp).fillMaxHeight().padding(horizontal = 6.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            routes.forEach { route ->
                val selected = path == route.path
                Surface(
                    onClick = { select(route.path) },
                    modifier =
                    Modifier.semantics {
                        this.selected = selected
                        contentDescription = route.label
                    }
                        .appiumSemantics(
                            text = route.label,
                            description = route.label,
                            onClick = { select(route.path) },
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ) {
                    Column(
                        Modifier.width(72.dp).padding(horizontal = 5.dp, vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        ReadingIcon(route.icon, null, Modifier.size(17.dp))
                        Text(route.label, fontSize = 11.sp, lineHeight = 17.6.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBottomNav(path: String, routes: List<NavRoute>, select: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = navigationSpacing(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                routes.forEach { route ->
                    val active = path == route.path
                    Surface(
                        onClick = { select(route.path) },
                        modifier =
                        Modifier.semantics {
                            selected = active
                            contentDescription = route.label
                        }.appiumSemantics(
                            description = route.label,
                            text = route.label,
                            onClick = { select(route.path) },
                        ),
                        shape = RoundedCornerShape(8.dp),
                        color =
                        if (active) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        contentColor =
                        if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ) {
                        Column(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            ReadingIcon(route.icon, null, Modifier.size(17.dp))
                            Text(route.label, maxLines = 1, fontSize = 12.sp, lineHeight = 19.2.sp)
                        }
                    }
                }
            }
        }
    }
}
