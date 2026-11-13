/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.core.PanelSessionManager
import com.storyteller_f.alphaheart.panel.LocalPanelUiViewModel
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.log_supporting
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.ModelStorage
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.stringResource

@Composable
fun createPanelAllUserViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllUsersViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelOverviewViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        OverviewViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelAllCommunitiesViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllCommunitiesViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelAllPublicRoomsViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllPublicRoomsViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelAllPrivateRoomsViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllPrivateRoomsViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelAllTopicsViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllTopicsViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelAllTitlesViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllTitlesViewModel(sessionManager, modelStorage)
    }

@Composable
fun createPanelAllFilesViewModel() =
    panelViewModel { sessionManager, modelStorage ->
        AllFilesViewModel(sessionManager, modelStorage)
    }

@Composable
inline fun <reified VM : ViewModel> panelViewModel(
    keys: List<Comparable<*>?>? = null,
    crossinline factory: (PanelSessionManager, ModelStorage) -> VM,
): VM {
    val instance by LocalPanelUiViewModel.current.instance.collectAsState()
    val sessionManager = instance.sessionManager
    val modelStorage = instance.database
    SideEffect {
        Napier.i {
            "viewModel ${VM::class.simpleName ?: "<none>"}${keys ?: "<none>"} composable"
        }
    }
    return viewModel(key = "${keys?.joinToString() ?: "<none>"}") {
        Napier.i {
            "viewModel ${VM::class.simpleName ?: "<none>"}${keys ?: "<none>"} build"
        }
        factory(sessionManager, modelStorage)
    }
}

@Composable
fun createPanelUserViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("user", id)) { sessionManager, modelStorage ->
        IdUserViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelUserOverviewViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("user-overview", id)) { sessionManager, modelStorage ->
        IdUserOverviewViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelJoinedCommunitiesViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-communities", uid)) { sessionManager, modelStorage ->
        UserJoinedCommunitiesViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelJoinedRoomsViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-rooms", uid)) { sessionManager, modelStorage ->
        UserJoinedRoomsViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelReceivedTitlesViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-titles", uid)) { sessionManager, modelStorage ->
        UserReceivedTitlesViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserFilesViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-files", uid)) { sessionManager, modelStorage ->
        UserFilesViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserLogsViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-logs", uid)) { sessionManager, modelStorage ->
        UserLogsViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserUploadRecordsViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-upload-records", uid)) { sessionManager, modelStorage ->
        UserUploadRecordsViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserReactionsViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-reactions", uid)) { sessionManager, modelStorage ->
        UserReactionsViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserCommentsViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-comments", uid)) { sessionManager, modelStorage ->
        UserCommentsViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserFavoritesViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-favorites", uid)) { sessionManager, modelStorage ->
        UserFavoritesViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelUserSubscriptionsViewModel(uid: PrimaryKey) =
    panelViewModel(keys = listOf("user-subscriptions", uid)) { sessionManager, modelStorage ->
        UserSubscriptionsViewModel(sessionManager, modelStorage, uid)
    }

@Composable
fun createPanelCommunityViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("community", id)) { sessionManager, modelStorage ->
        IdCommunityViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelCommunityMembersViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("community-members", id)) { sessionManager, modelStorage ->
        CommunityMembersViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelRoomViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("room", id)) { sessionManager, modelStorage ->
        IdRoomViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelRoomMembersViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("room-members", id)) { sessionManager, modelStorage ->
        RoomMembersViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelRoomFilesViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("room-files", id)) { sessionManager, modelStorage ->
        RoomFilesViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelTopicViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("topic", id)) { sessionManager, modelStorage ->
        IdTopicViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelFileViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("file", id)) { sessionManager, modelStorage ->
        IdFileViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelFileRefsViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("file-refs", id)) { sessionManager, modelStorage ->
        FileRefsViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelTitleViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("title", id)) { sessionManager, modelStorage ->
        IdTitleViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelTopicTopicsViewModel(id: PrimaryKey) =
    panelViewModel(keys = listOf("topic-topics", id)) { sessionManager, modelStorage ->
        TopicTopicsViewModel(sessionManager, modelStorage, id)
    }

@Composable
fun createPanelLogsViewModel(targetId: PrimaryKey, objectType: com.storyteller_f.shared.type.ObjectType) =
    panelViewModel(keys = listOf("panel-logs", targetId, objectType)) { sessionManager, modelStorage ->
        PanelLogsViewModel(sessionManager, modelStorage, targetId, objectType)
    }

@Composable
internal fun CreatePanelTaskRecordsViewModel(type: TaskRecordType?, isSuccess: Boolean?, failureType: String?) =
    panelViewModel(keys = listOf("task-records", type, isSuccess, failureType)) { sessionManager, modelStorage ->
        TaskRecordsViewModel(
            sessionManager = sessionManager,
            modelStorage = modelStorage,
            type = type,
            isSuccess = isSuccess,
            failureType = failureType,
        )
    }

@Composable
internal fun CreateTaskRecordSummariesViewModel() =
    panelViewModel(keys = listOf("task-record-summaries")) { sessionManager, _ ->
        TaskRecordSummariesViewModel(sessionManager)
    }

@Composable
internal fun CreateWorkerTasksViewModel(): WorkerTasksViewModel =
    panelViewModel(keys = listOf("worker-tasks")) { sessionManager, _ ->
        WorkerTasksViewModel(sessionManager)
    }

@Composable
fun PanelLogsTab(targetId: PrimaryKey, objectType: ObjectType) {
    val vm = createPanelLogsViewModel(targetId, objectType)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    PanelLogItem(info)
                } else {
                    ListItem(headlineContent = { Text("") })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PanelLogItem(info: PanelLogInfo) {
    ListItem(
        headlineContent = { Text(info.action) },
        supportingContent = {
            Text(
                stringResource(
                    Res.string.log_supporting,
                    info.objectType,
                    info.targetId.toString(),
                    info.adminId.toString(),
                    info.createdTime.toString(),
                ),
            )
        },
    )
    HorizontalDivider()
}
