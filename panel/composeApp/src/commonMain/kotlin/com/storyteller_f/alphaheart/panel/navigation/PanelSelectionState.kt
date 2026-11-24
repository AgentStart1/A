/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.navigation

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
