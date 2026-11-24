/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_ROOMS
import com.storyteller_f.alphaheart.app.navigation.navigationDescription
import com.storyteller_f.alphaheart.app.unread_messages
import com.storyteller_f.alphaheart.app.unread_rooms
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.client.compose_core.icons.compactNavigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.navigation.navigationSpacing
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBadge as Badge
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun CustomRailNav(
    currentEntry: String?,
    navRoutes: List<NavRoute>,
    unreadRoomsBadge: Boolean = false,
    unreadMessageCount: Long = 0,
    navigate: (String) -> Unit = {},
) {
    val unreadLabel =
        stringResource(
            if (unreadMessageCount > 0) Res.string.unread_messages else Res.string.unread_rooms,
        )
    Column(
        Modifier.width(170.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        navRoutes.forEach { route ->
            val showBadge = route.path == HOME_START_DESTINATION_ROOMS && unreadRoomsBadge
            val onClick = { navigate(route.path) }
            val selected = currentEntry == route.path
            val label = navigationDescription(route.label, showBadge, unreadMessageCount, unreadLabel)
            Surface(
                onClick = onClick,
                modifier =
                Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics {
                    this.selected = selected
                    contentDescription = label
                }.appiumSemantics(
                    testTag = route.label,
                    description = label,
                    text = label,
                    onClick = onClick,
                ),
                shape = MaterialTheme.shapes.small,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                contentColor =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(compactNavigationIcon(route), null, Modifier.size(17.dp))
                    Text(route.label, Modifier.weight(1f), maxLines = 1)
                    if (showBadge) {
                        Badge {
                            if (unreadMessageCount > 0) {
                                Text(
                                    unreadMessageCount.toString(),
                                    Modifier.appiumSemantics(text = unreadMessageCount.toString()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNav(
    path: String,
    navRoutes: List<NavRoute>,
    unreadRoomsBadge: Boolean = false,
    unreadMessageCount: Long = 0,
    navigate: (String) -> Unit = { },
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    MaterialTheme(
        colorScheme =
        colors.copy(
            surfaceContainer = if (dark) Color(0xFF1D2A23) else Color.White,
            onSurface = if (dark) Color(0xFFE2EDE5) else Color(0xFF22382E),
            primary = if (dark) Color(0xFFAED4B5) else Color(0xFF32634D),
            primaryContainer = if (dark) Color(0xFF2D4633) else Color(0xFFE4EEE3),
            outlineVariant = if (dark) Color(0xFF3A4B40) else Color(0xFFDCE4DD),
        ),
    ) {
        ReadingInteractions {
            CompactNavigationBar(path, navRoutes, unreadRoomsBadge, unreadMessageCount, navigate)
        }
    }
}

@Composable
private fun CompactNavigationBar(
    path: String,
    navRoutes: List<NavRoute>,
    unreadRoomsBadge: Boolean,
    unreadMessageCount: Long,
    navigate: (String) -> Unit,
) {
    val unreadLabel =
        stringResource(
            if (unreadMessageCount > 0) Res.string.unread_messages else Res.string.unread_rooms,
        )
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).testTag("reading-bottom-nav"),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = navigationSpacing(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navRoutes.forEach { route ->
                val showBadge = route.path == HOME_START_DESTINATION_ROOMS && unreadRoomsBadge
                CompactNavigationItem(route, path == route.path, showBadge, unreadMessageCount, unreadLabel) {
                    navigate(route.path)
                }
            }
        }
    }
}

@Composable
private fun CompactNavigationItem(
    route: NavRoute,
    active: Boolean,
    showBadge: Boolean,
    unreadMessageCount: Long,
    unreadLabel: String,
    onClick: () -> Unit,
) {
    val label = navigationDescription(route.label, showBadge, unreadMessageCount, unreadLabel)
    Surface(
        onClick = onClick,
        modifier =
        Modifier.semantics {
            selected = active
            contentDescription = label
        }.appiumSemantics(testTag = route.label, description = label, text = label, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(17.dp).testTag("bottom-nav-icon-${route.path}")) {
                Icon(compactNavigationIcon(route), null, Modifier.size(17.dp))
                if (showBadge) {
                    val badgeModifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-6).dp)
                    if (unreadMessageCount > 0) {
                        Badge(badgeModifier.testTag("bottom-nav-unread")) {
                            Text(unreadMessageCount.toString(), fontSize = 11.sp, lineHeight = 17.6.sp)
                        }
                    } else {
                        Badge(badgeModifier.testTag("bottom-nav-unread"), content = null)
                    }
                }
            }
            Text(
                route.label,
                maxLines = 1,
                style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 19.2.sp,
                    letterSpacing = 0.sp,
                ),
            )
        }
    }
}
