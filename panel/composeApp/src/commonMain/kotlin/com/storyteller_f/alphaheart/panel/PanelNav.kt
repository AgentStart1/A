/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.runtime.compositionLocalOf
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
import com.storyteller_f.alphaheart.panel.common.PanelNav
import com.storyteller_f.alphaheart.panel.common.PanelRoomDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTitleDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelTopicDetailScreen
import com.storyteller_f.alphaheart.panel.common.PanelUserDetailScreen

val LocalPanelNav = compositionLocalOf<PanelNav> { error("no nav") }

internal enum class PanelListDetailScene {
    Users,
    Communities,
    Rooms,
    Topics,
    Files,
    Titles,
}

internal enum class PanelListDetailPane {
    List,
    Detail,
}

internal data class PanelListDetailDestination(val scene: PanelListDetailScene, val pane: PanelListDetailPane)

internal fun NavKey.panelListDetailDestination(): PanelListDetailDestination? {
    val destination =
        when (this) {
            PanelAllUsersScreen -> listRoute(PanelListDetailScene.Users)
            is PanelUserDetailScreen -> detailRoute(PanelListDetailScene.Users)
            PanelAllCommunitiesScreen -> listRoute(PanelListDetailScene.Communities)
            is PanelCommunityDetailScreen -> detailRoute(PanelListDetailScene.Communities)
            PanelAllPublicRoomsScreen, PanelAllPrivateRoomsScreen -> listRoute(PanelListDetailScene.Rooms)
            is PanelRoomDetailScreen -> detailRoute(PanelListDetailScene.Rooms)
            PanelAllTopicsScreen -> listRoute(PanelListDetailScene.Topics)
            is PanelTopicDetailScreen -> detailRoute(PanelListDetailScene.Topics)
            PanelAllFilesScreen -> listRoute(PanelListDetailScene.Files)
            is PanelFileDetailScreen -> detailRoute(PanelListDetailScene.Files)
            PanelAllTitlesScreen -> listRoute(PanelListDetailScene.Titles)
            is PanelTitleDetailScreen -> detailRoute(PanelListDetailScene.Titles)
            else -> null
        }
    return destination
}

private fun listRoute(scene: PanelListDetailScene) = PanelListDetailDestination(scene, PanelListDetailPane.List)

private fun detailRoute(scene: PanelListDetailScene) = PanelListDetailDestination(scene, PanelListDetailPane.Detail)

internal fun NavBackStack<NavKey>.addPanelDetail(detail: NavKey) {
    val detailDestination = detail.panelListDetailDestination()
    require(detailDestination?.pane == PanelListDetailPane.Detail) {
        "Only panel detail destinations can be added with addPanelDetail"
    }
    val currentDestination = lastOrNull()?.panelListDetailDestination()
    val previousDestination = getOrNull(lastIndex - 1)?.panelListDetailDestination()
    val isReplacingCurrentDetail =
        currentDestination?.pane == PanelListDetailPane.Detail &&
            currentDestination.scene == detailDestination.scene &&
            previousDestination?.pane == PanelListDetailPane.List &&
            previousDestination.scene == detailDestination.scene
    if (isReplacingCurrentDetail) {
        removeLastOrNull()
    }
    add(detail)
}

internal enum class PanelDrawerSection {
    Overview,
    Users,
    Communities,
    PublicRooms,
    PrivateRooms,
    Topics,
    Files,
    Titles,
    TaskRecords,
    WorkerTasks,
}

/** Detail destinations retain their owning directory's highlight. */
internal fun panelDrawerSection(stack: List<NavKey>): PanelDrawerSection? {
    val route = stack.lastOrNull()
    return when (route?.panelListDetailDestination()?.scene) {
        PanelListDetailScene.Users -> PanelDrawerSection.Users

        PanelListDetailScene.Communities -> PanelDrawerSection.Communities

        PanelListDetailScene.Rooms -> {
            val directory = stack.lastOrNull { it == PanelAllPrivateRoomsScreen || it == PanelAllPublicRoomsScreen }
            if (directory == PanelAllPrivateRoomsScreen) {
                PanelDrawerSection.PrivateRooms
            } else {
                PanelDrawerSection.PublicRooms
            }
        }

        PanelListDetailScene.Topics -> PanelDrawerSection.Topics

        PanelListDetailScene.Files -> PanelDrawerSection.Files

        PanelListDetailScene.Titles -> PanelDrawerSection.Titles

        null ->
            when (route) {
                com.storyteller_f.alphaheart.panel.common.PanelOverviewScreen -> PanelDrawerSection.Overview

                com.storyteller_f.alphaheart.panel.common.PanelTaskRecordsScreen,
                is com.storyteller_f.alphaheart.panel.common.PanelTaskRecordDetailScreen,
                -> PanelDrawerSection.TaskRecords

                com.storyteller_f.alphaheart.panel.common.PanelWorkerTasksScreen -> PanelDrawerSection.WorkerTasks

                is com.storyteller_f.alphaheart.panel.common.PanelFilePreviewScreen -> PanelDrawerSection.Files

                else -> null
            }
    }
}
