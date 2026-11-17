/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.panel.common.PanelCommunityDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelFileDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelFilePreviewScreen
import com.storyteller_f.alphaheart.panel.common.PanelRoomDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTitleDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTopicDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelUserDetailScreen
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey

internal fun selectedPanelEntityId(route: NavKey?, objectType: ObjectType): PrimaryKey? {
    val selection =
        when (route) {
            is PanelUserDetailScreen -> ObjectType.USER to route.uid
            is PanelCommunityDetailScreen -> ObjectType.COMMUNITY to route.id
            is PanelRoomDetailScreen -> ObjectType.ROOM to route.id
            is PanelTopicDetailScreen -> ObjectType.TOPIC to route.id
            is PanelFileDetailScreen -> ObjectType.FILE to route.id
            is PanelFilePreviewScreen -> ObjectType.FILE to route.id
            is PanelTitleDetailScreen -> ObjectType.TITLE to route.id
            else -> null
        }
    return selection?.takeIf { it.first == objectType }?.second
}

@Composable
internal fun Modifier.panelDirectorySelection(selected: Boolean): Modifier =
    clip(RoundedCornerShape(8.dp))
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .semantics { this.selected = selected }

@Composable
internal fun panelDirectoryRowColors(selected: Boolean): ListItemColors =
    ListItemDefaults.colors(
        containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    )
