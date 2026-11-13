/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.unread_messages
import com.storyteller_f.alphaheart.client.compose_core.unread_rooms
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import org.jetbrains.compose.resources.stringResource

data class NavRoute(val path: String, val icon: ImageVector, val label: String)

internal const val HOME_START_DESTINATION_ROOMS = "/rooms"

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
    NavigationRail(modifier = Modifier.padding(horizontal = 8.dp)) {
        navRoutes.forEach { route ->
            val showBadge = route.path == HOME_START_DESTINATION_ROOMS && unreadRoomsBadge
            val onClick = { navigate(route.path) }
            NavigationRailItem(
                selected = currentEntry == route.path,
                onClick = onClick,
                icon = {
                    if (showBadge) {
                        BadgedBox(badge = {
                            Badge {
                                if (unreadMessageCount > 0) {
                                    Text(
                                        unreadMessageCount.toString(),
                                        Modifier.appiumSemantics(text = unreadMessageCount.toString()),
                                    )
                                }
                            }
                        }) {
                            Icon(imageVector = route.icon, contentDescription = route.label)
                        }
                    } else {
                        Icon(imageVector = route.icon, contentDescription = route.label)
                    }
                },
                label = { Text(route.label) },
                modifier =
                Modifier.semantics {
                    contentDescription = if (showBadge) "${route.label}, $unreadLabel" else route.label
                }.appiumSemantics(
                    testTag = route.label,
                    description = if (showBadge) "${route.label}, $unreadLabel" else route.label,
                    text = route.label,
                    onClick = onClick,
                ),
            )
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
    val unreadLabel =
        stringResource(
            if (unreadMessageCount > 0) Res.string.unread_messages else Res.string.unread_rooms,
        )
    NavigationBar {
        navRoutes.forEach { route ->
            val showBadge = route.path == HOME_START_DESTINATION_ROOMS && unreadRoomsBadge
            val onClick = { navigate(route.path) }
            NavigationBarItem(
                selected = path == route.path,
                onClick = onClick,
                icon = {
                    if (showBadge) {
                        BadgedBox(badge = {
                            Badge {
                                if (unreadMessageCount > 0) {
                                    Text(
                                        unreadMessageCount.toString(),
                                        Modifier.appiumSemantics(text = unreadMessageCount.toString()),
                                    )
                                }
                            }
                        }) {
                            Icon(imageVector = route.icon, contentDescription = route.label)
                        }
                    } else {
                        Icon(imageVector = route.icon, contentDescription = route.label)
                    }
                },
                label = { Text(route.label) },
                modifier =
                Modifier.semantics {
                    contentDescription = if (showBadge) "${route.label}, $unreadLabel" else route.label
                }.appiumSemantics(
                    testTag = route.label,
                    description = if (showBadge) "${route.label}, $unreadLabel" else route.label,
                    text = route.label,
                    onClick = onClick,
                ),
            )
        }
    }
}
