/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
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
import com.storyteller_f.alphaheart.panel.common.PanelRoomDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTitleDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTopicDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelUserDetailScreen
import com.storyteller_f.alphaheart.panel.pages.failureTypeForStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppCommonTest {
    @Test
    fun listAndDetailRoutesUseMatchingScenes() {
        assertListDetailPair(
            PanelAllUsersScreen,
            PanelUserDetailScreen(1),
            PanelListDetailScene.Users,
        )
        assertListDetailPair(
            PanelAllCommunitiesScreen,
            PanelCommunityDetailScreen(1),
            PanelListDetailScene.Communities,
        )
        assertListDetailPair(
            PanelAllPublicRoomsScreen,
            PanelRoomDetailScreen(1),
            PanelListDetailScene.Rooms,
        )
        assertListDetailPair(
            PanelAllPrivateRoomsScreen,
            PanelRoomDetailScreen(1),
            PanelListDetailScene.Rooms,
        )
        assertListDetailPair(
            PanelAllTopicsScreen,
            PanelTopicDetailScreen(1),
            PanelListDetailScene.Topics,
        )
        assertListDetailPair(
            PanelAllFilesScreen,
            PanelFileDetailScreen(1),
            PanelListDetailScene.Files,
        )
        assertListDetailPair(
            PanelAllTitlesScreen,
            PanelTitleDetailScreen(1),
            PanelListDetailScene.Titles,
        )
    }

    @Test
    fun selectingSameSceneDetailReplacesCurrentDetail() {
        val backStack = NavBackStack<NavKey>(PanelAllUsersScreen, PanelUserDetailScreen(1))

        backStack.addPanelDetail(PanelUserDetailScreen(2))

        assertEquals(
            listOf(PanelAllUsersScreen, PanelUserDetailScreen(2)),
            backStack.toList(),
        )
    }

    @Test
    fun navigatingToAnotherDetailScenePreservesHistory() {
        val backStack = NavBackStack<NavKey>(PanelAllUsersScreen, PanelUserDetailScreen(1))

        backStack.addPanelDetail(PanelCommunityDetailScreen(2))

        assertEquals(
            listOf(
                PanelAllUsersScreen,
                PanelUserDetailScreen(1),
                PanelCommunityDetailScreen(2),
            ),
            backStack.toList(),
        )
    }

    @Test
    fun listDetailDirectiveUsesPostDrawerContentWidth() {
        val directive =
            calculatePanelPaneDirective(
                contentSize = DpSize(600.dp, 900.dp),
                windowPosture = Posture(),
            )

        assertEquals(1, directive.maxHorizontalPartitions)
        assertEquals(420.dp, directive.defaultPanePreferredWidth)
    }

    @Test
    fun expandedContentWidthEnablesListDetailPane() {
        val directive =
            calculatePanelPaneDirective(
                contentSize = DpSize(840.dp, 900.dp),
                windowPosture = Posture(),
            )

        assertEquals(2, directive.maxHorizontalPartitions)
        assertEquals(420.dp, directive.defaultPanePreferredWidth)
    }

    @Test
    fun taskRecordStatusClearsIncompatibleFailureType() {
        assertEquals(null, failureTypeForStatus(status = null, failureType = "failure"))
        assertEquals(null, failureTypeForStatus(status = true, failureType = "failure"))
        assertEquals("failure", failureTypeForStatus(status = false, failureType = "failure"))
    }
}

private fun assertListDetailPair(list: NavKey, detail: NavKey, scene: PanelListDetailScene) {
    assertEquals(
        PanelListDetailDestination(scene, PanelListDetailPane.List),
        list.panelListDetailDestination(),
    )
    assertEquals(
        PanelListDetailDestination(scene, PanelListDetailPane.Detail),
        detail.panelListDetailDestination(),
    )
}
