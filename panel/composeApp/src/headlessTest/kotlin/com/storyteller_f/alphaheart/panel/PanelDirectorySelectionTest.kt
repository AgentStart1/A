/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.panel.common.PanelAllCommunitiesScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllFilesScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllPrivateRoomsScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllPublicRoomsScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllTitlesScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllTopicsScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllUsersScreen
import com.storyteller_f.alphaheart.panel.common.PanelCommunityDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelFileDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelFilePreviewScreen
import com.storyteller_f.alphaheart.panel.common.PanelRoomDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTitleDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTopicDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelUserDetailScreen
import com.storyteller_f.alphaheart.panel.components.selectedPanelEntityId
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class PanelDirectorySelectionTest {
    @Test
    fun allSevenDirectoriesMatchTheirOwnEntityAndIgnoreEqualIdsOfOtherTypes() {
        val destinations =
            listOf<Triple<NavKey, NavKey, ObjectType>>(
                Triple(PanelAllUsersScreen, PanelUserDetailScreen(41), ObjectType.USER),
                Triple(PanelAllCommunitiesScreen, PanelCommunityDetailScreen(41), ObjectType.COMMUNITY),
                Triple(PanelAllPublicRoomsScreen, PanelRoomDetailScreen(41), ObjectType.ROOM),
                Triple(PanelAllPrivateRoomsScreen, PanelRoomDetailScreen(41), ObjectType.ROOM),
                Triple(PanelAllTopicsScreen, PanelTopicDetailScreen(41), ObjectType.TOPIC),
                Triple(PanelAllFilesScreen, PanelFileDetailScreen(41), ObjectType.FILE),
                Triple(PanelAllTitlesScreen, PanelTitleDetailScreen(41), ObjectType.TITLE),
            )
        destinations.forEach { (directory, detail, type) ->
            assertNull(selectedPanelEntityId(directory, type))
            assertEquals(41L, selectedPanelEntityId(detail, type))
            ObjectType.entries.filter { it != type }.forEach { otherType ->
                assertNull(selectedPanelEntityId(detail, otherType))
            }
        }
    }

    @Test
    fun filePreviewRetainsTheSameFileIdentityAndNullRouteClearsSelection() {
        assertEquals(41L, selectedPanelEntityId(PanelFilePreviewScreen(41), ObjectType.FILE))
        assertNull(selectedPanelEntityId(null, ObjectType.FILE))
    }
}
