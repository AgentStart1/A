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
import com.storyteller_f.alphaheart.panel.common.PanelTaskRecordDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTaskRecordsScreen
import com.storyteller_f.alphaheart.panel.common.PanelTitleDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTopicDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelUserDetailScreen
import com.storyteller_f.shared.model.TaskRecordType
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelDrawerSelectionTest {
    @Test
    fun taskRecordDetailsKeepTheirDirectorySelected() {
        val stack = listOf(PanelTaskRecordsScreen, PanelTaskRecordDetailScreen(TaskRecordType.TOPIC_ACG))
        assertEquals(PanelDrawerSection.TaskRecords, panelDrawerSection(stack))
    }

    @Test
    fun everyEntityDetailKeepsItsDirectorySelected() {
        val routes =
            listOf(
                Triple(PanelAllUsersScreen, PanelUserDetailScreen(1), PanelDrawerSection.Users),
                Triple(PanelAllCommunitiesScreen, PanelCommunityDetailScreen(1), PanelDrawerSection.Communities),
                Triple(PanelAllTopicsScreen, PanelTopicDetailScreen(1), PanelDrawerSection.Topics),
                Triple(PanelAllTitlesScreen, PanelTitleDetailScreen(1), PanelDrawerSection.Titles),
                Triple(PanelAllFilesScreen, PanelFileDetailScreen(1), PanelDrawerSection.Files),
                Triple(PanelAllPublicRoomsScreen, PanelRoomDetailScreen(1), PanelDrawerSection.PublicRooms),
                Triple(PanelAllPrivateRoomsScreen, PanelRoomDetailScreen(1), PanelDrawerSection.PrivateRooms),
            )
        routes.forEach { (directory, detail, section) ->
            assertEquals(section, panelDrawerSection(listOf<NavKey>(directory, detail)))
        }
    }

    @Test
    fun filePreviewKeepsFilesSelected() {
        assertEquals(
            PanelDrawerSection.Files,
            panelDrawerSection(listOf(PanelAllFilesScreen, PanelFileDetailScreen(1), PanelFilePreviewScreen(1))),
        )
    }
}
