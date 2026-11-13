/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.storage.ModelStorage
import io.github.aakira.napier.Napier

@Composable
fun createSearchCommunitiesViewModel(finalOption: JoinStatusSearch, query: String) =
    customViewModel(
        listOf("search-community", finalOption.name, query),
    ) { client, databaseSource ->
        CommunitiesViewModel(client, databaseSource, finalOption, query)
    }

@Composable
fun createJoinedCommunitiesViewModel() =
    customViewModel(
        listOf("joined-communities"),
    ) { client, databaseSource ->
        UserJoinedCommunitiesViewModel(client, databaseSource)
    }

@Composable
fun createJoinedCommunitiesWithPosterViewModel() =
    customViewModel(
        listOf("joined-communities-with-poster"),
    ) { client, databaseSource ->
        UserJoinedCommunitiesWithPosterViewModel(client, databaseSource)
    }

@Composable
fun createTargetUserJoinedCommunitiesViewModel(target: PrimaryKey, word: String = "") =
    customViewModel(
        listOf("communities", target, word),
    ) { client, databaseSource ->
        if (word.isNotBlank()) {
            CommunitiesViewModel(client, databaseSource, JoinStatusSearch.JOINED, word, target)
        } else {
            UserJoinedCommunitiesViewModel(client, databaseSource, target)
        }
    }

@Composable
fun createCommunityViewModel(communityId: PrimaryKey) =
    customViewModel(
        listOf("community", communityId),
    ) { client, databaseSource ->
        IdCommunityViewModel(client, databaseSource, communityId)
    }

@Composable
fun createCommunityViewModel(communityAid: String) =
    customViewModel(
        listOf("community", communityAid),
    ) { client, databaseSource ->
        AidCommunityViewModel(client, databaseSource, communityAid)
    }

@Composable
fun createCommunityRoomsViewModel(communityId: PrimaryKey) =
    customViewModel(
        listOf("community-rooms", communityId),
    ) { client, databaseSource ->
        // 使用专门的社区房间ViewModel
        CommunityRoomsViewModel(client, databaseSource, communityId)
    }

@Composable
fun createJoinedRoomsViewModel() =
    customViewModel { client, databaseSource ->
        UserJoinedRoomsViewModel(client, databaseSource)
    }

@Composable
fun createRoomViewModel(roomId: PrimaryKey) =
    customViewModel(
        listOf("room", roomId),
    ) { client, databaseSource ->
        IdRoomViewModel(client, databaseSource, roomId)
    }

@Composable
fun createRoomSearchInCommunityViewModel(scope: SearchScope.CommunityRoom, current: String) =
    customViewModel(
        listOf("rooms", scope.communityId, current),
    ) { client, databaseSource ->
        // 使用专门的社区房间搜索ViewModel
        CommunityRoomSearchViewModel(client, databaseSource, scope.communityId, current)
    }

@Composable
fun createRoomSearchViewModel(finalOption: JoinStatusSearch, current: String) =
    customViewModel(listOf("my-rooms", finalOption.name, current)) { client, databaseSource ->
        RoomsViewModel(client, databaseSource, finalOption, current)
    }

@Composable
fun createRoomKeysViewModel(roomId: PrimaryKey, roomInfo: RoomInfo) =
    customViewModel(
        listOf("room-keys", roomId),
    ) { client, _ ->
        RoomKeysViewModel(client, roomId, roomInfo.isPrivate)
    }

@Composable
fun createRoomViewModel(roomAid: String) =
    customViewModel(
        listOf("room", roomAid),
    ) { client, databaseSource ->
        AidRoomViewModel(client, databaseSource, roomAid)
    }

@Composable
fun createRoomTopicsViewModel(roomId: PrimaryKey): TopicsViewModel =
    customViewModel(
        listOf("room-topics", roomId),
    ) { sessionManager, databaseSource ->
        TopicsViewModel(
            sessionManager,
            databaseSource,
            roomId,
            ObjectType.ROOM,
        )
    }

@Composable
fun createCommunityTopicsViewModel(communityId: PrimaryKey): TopicsViewModel =
    customViewModel(
        listOf("community-topics", communityId),
    ) { sessionManager, databaseSource ->
        TopicsViewModel(
            sessionManager,
            databaseSource,
            communityId,
            ObjectType.COMMUNITY,
        )
    }

@Composable
fun createUserTopicsViewModel(uid: PrimaryKey): TopicsViewModel =
    customViewModel(
        listOf("user-topics", uid),
    ) { sessionManager, databaseSource ->
        TopicsViewModel(
            sessionManager,
            databaseSource,
            uid,
            ObjectType.USER,
        )
    }

@Composable
fun createTopicSearchViewModel(current: String) =
    customViewModel(
        listOf("topic", current),
    ) { sessionManager, databaseSource ->
        TopicSearchViewModel(sessionManager, databaseSource, current, null, null)
    }

@Composable
fun createTopicSearchInTopicViewModel(scope: SearchScope.TopicTopic, current: String) =
    customViewModel(
        listOf("topic", scope.topicId, current),
    ) { sessionManager, databaseSource ->
        TopicSearchViewModel(sessionManager, databaseSource, current, scope.topicId, ObjectType.TOPIC)
    }

@Composable
fun createTopicSearchInUserViewModel(scope: SearchScope.UserTopic, current: String) =
    customViewModel(
        listOf("topic", scope.userId, current),
    ) { sessionManager, databaseSource ->
        TopicSearchViewModel(sessionManager, databaseSource, current, scope.userId, ObjectType.USER)
    }

@Composable
fun createMemberSearchViewModel(word: String) =
    customViewModel(
        listOf("members", word),
    ) { sessionManager, databaseSource ->
        UserSearchViewModel(sessionManager, databaseSource, word)
    }

@Composable
fun createTopicSearchInCommunityViewModel(scope: SearchScope.CommunityTopic, current: String) =
    customViewModel(
        listOf("topic", scope.communityId, current),
    ) { sessionManager, databaseSource ->
        TopicSearchViewModel(sessionManager, databaseSource, current, scope.communityId, ObjectType.COMMUNITY)
    }

@Composable
fun createTopicViewModel(topicId: PrimaryKey): IdTopicViewModel =
    customViewModel(
        listOf("topic", topicId),
    ) { sessionManager, databaseSource ->
        IdTopicViewModel(sessionManager, databaseSource, topicId)
    }

@Composable
fun createTopicsInTopicViewModel(topicId: PrimaryKey): TopicsViewModel =
    customViewModel(
        listOf("topic-topics", topicId),
    ) { sessionManager, databaseSource ->
        TopicsViewModel(
            sessionManager,
            databaseSource,
            topicId,
            ObjectType.TOPIC,
        )
    }

@Composable
fun createTopicViewModel(topicAid: String): AidTopicViewModel =
    customViewModel(
        listOf("topic", topicAid),
    ) { sessionManager, databaseSource ->
        AidTopicViewModel(sessionManager, databaseSource, topicAid)
    }

@Composable
fun createTopicSearchInRoomViewModel(scope: SearchScope.RoomTopic, current: String) =
    customViewModel(
        listOf("topic", scope.roomId, current),
    ) { sessionManager, databaseSource ->
        TopicSearchViewModel(sessionManager, databaseSource, current, scope.roomId, ObjectType.ROOM)
    }

@Composable
fun createSearchMemberInRoomViewModel(scope: SearchScope.RoomMember, current: String) =
    customViewModel(
        listOf("members", scope.roomId, current),
    ) { sessionManager, databaseSource ->
        ContainerMemberViewModel(sessionManager, databaseSource, scope.roomId, current, ObjectType.ROOM)
    }

@Composable
fun createMemberSearchInCommunityViewModel(scope: SearchScope.CommunityMember, current: String) =
    customViewModel(
        listOf("members", scope.communityId, current),
    ) { sessionManager, databaseSource ->
        ContainerMemberViewModel(sessionManager, databaseSource, scope.communityId, current, ObjectType.COMMUNITY)
    }

@Composable
fun createMediaListViewModel(objectTuple: ObjectTuple) =
    customViewModel(
        listOf("media", objectTuple.objectId),
    ) { sessionManager, databaseSource ->
        MediaListViewModel(sessionManager, databaseSource, objectTuple.objectId, objectTuple.objectType)
    }

@Composable
fun createFileSearchViewModel(objectId: PrimaryKey, objectType: ObjectType, word: String) =
    customViewModel(
        listOf("file-search", objectId, word),
    ) { sessionManager, databaseSource ->
        FileSearchViewModel(sessionManager, databaseSource, word, objectId, objectType)
    }

@Composable
fun createMemberViewModel(objectId: PrimaryKey, objectType: ObjectType) =
    customViewModel(
        listOf("members", objectId),
    ) { sessionManager, databaseSource ->
        ContainerMemberViewModel(sessionManager, databaseSource, objectId, "", objectType)
    }

@Composable
fun createUserViewModel(userAid: String) =
    customViewModel(
        listOf("user", userAid),
    ) { sessionManager, databaseSource ->
        AidUserViewModel(sessionManager, databaseSource, userAid)
    }

@Composable
fun createUserViewModel(userId: PrimaryKey) =
    customViewModel(
        listOf("user", userId),
    ) { sessionManager, databaseSource ->
        IdUserViewModel(sessionManager, databaseSource, userId)
    }

@Composable
fun createWorldViewModel(): WorldViewModel =
    customViewModel(
        listOf("world"),
    ) { sessionManager, databaseSource ->
        WorldViewModel(sessionManager, databaseSource)
    }

@Composable
fun createReactionsViewModel(objectId: PrimaryKey): ReactionsViewModel =
    customViewModel(
        listOf("reactions", objectId),
    ) { sessionManager, databaseSource ->
        ReactionsViewModel(sessionManager, objectId, databaseSource)
    }

@Composable
fun createUserTitlesViewModel(
    uid: PrimaryKey,
    searchType: TitleSearchType,
    status: TitleWorkStatus? = null,
    type: TitleType? = null,
    scopeId: PrimaryKey? = null,
) = customViewModel(
    listOf("user-titles", uid),
) { sessionManager, databaseSource ->
    TitlesViewModel(sessionManager, databaseSource, uid, searchType, status, type, scopeId)
}

@Composable
fun createUploadViewModel(myUid: PrimaryKey) =
    customViewModel(
        listOf("upload"),
    ) { _, model ->
        UploadViewModel(myUid, model)
    }

@Composable
fun getUploadViewModel(pathHash: String, myUid: PrimaryKey): UploadDetailViewModel =
    customViewModel(
        listOf("upload-detail", pathHash),
    ) { _, modelStorage ->
        UploadDetailViewModel(modelStorage, pathHash, myUid)
    }

@Composable
fun getMarkdownMediasViewModel(input: String, objectTuple: ObjectTuple): MarkdownMediasViewModel =
    customViewModel(
        listOf("content", md5(input)),
    ) { sessionManager, _ ->
        MarkdownMediasViewModel(sessionManager, input, objectTuple)
    }

@Composable
fun getDownloadViewModel(fileId: PrimaryKey?): DownloadViewModel {
    val records = LocalUiViewModel.current.resourceRecords
    return customViewModel(
        listOf("download", fileId),
    ) { _, _ ->
        DownloadViewModel(records, fileId)
    }
}

@Composable
fun getDownloadListViewModel(): DownloadListViewModel {
    val records = LocalUiViewModel.current.resourceRecords
    return customViewModel(
        listOf("download-list"),
    ) { _, _ ->
        DownloadListViewModel(records)
    }
}

@Composable
fun getQuotaViewModel(objectTuple: ObjectTuple, quotaType: QuotaType = QuotaType.FILE): QuotaViewModel =
    customViewModel(
        listOf("quota", objectTuple.objectId, quotaType.name),
    ) { sessionManager, _ ->
        QuotaViewModel(sessionManager, objectTuple, quotaType)
    }

@Composable
fun getChildAccountsViewModel(): ChildAccountsViewModel =
    customMainViewModel(
        listOf("child-accounts"),
    ) { sessionManager, storageSource ->
        ChildAccountsViewModel(storageSource, sessionManager)
    }

@Composable
fun getLoginHistoryViewModel(): SessionHistoryViewModel {
    val uIViewModel = LocalUiViewModel.current
    return customViewModel(listOf("login-history")) { _, _ ->
        SessionHistoryViewModel(uIViewModel)
    }
}

@Composable
fun getFavoriteViewModel() =
    customViewModel(listOf("favorite")) { sessionManager, modelStorage ->
        FavoritesViewModel(sessionManager, modelStorage)
    }

@Composable
fun getSubscriptionViewModel(): SubscriptionsViewModel =
    customViewModel(listOf("subscription")) {
            sessionManager,
            modelStorage,
        ->
        SubscriptionsViewModel(sessionManager, modelStorage)
    }

@Composable
fun getUserReactionRecordsViewModel() =
    customViewModel(
        listOf("user-reaction-records"),
    ) { sessionManager, modelStorage ->
        UserReactionRecordsViewModel(sessionManager, modelStorage)
    }

@Composable
fun getUserCommentsViewModel(): UserCommentsViewModel =
    customViewModel(listOf("user-comments")) {
            sessionManager,
            modelStorage,
        ->
        UserCommentsViewModel(sessionManager, modelStorage)
    }

@Composable
fun getUserOverviewViewModel() =
    customViewModel(listOf("user-overview")) { sessionManager, modelStorage ->
        UserOverviewViewModel(sessionManager, modelStorage)
    }

@Composable
fun getUnreadMessageCountViewModel(): UnreadMessageCountViewModel =
    customViewModel(listOf("unread-message-count")) { sessionManager, _ ->
        UnreadMessageCountViewModel(sessionManager)
    }

@Composable
fun createTitleComposeViewModel(
    initialScope: ObjectTuple? = null,
    initialType: TitleType? = null,
    lockScope: Boolean = false,
    lockType: Boolean = false,
): TitleComposeViewModel =
    customViewModel(listOf("title-compose")) { _, _ ->
        TitleComposeViewModel(initialScope, initialType, lockScope, lockType)
    }

@Composable
fun createFileViewModel(objectId: PrimaryKey) =
    customViewModel(
        listOf("files", objectId),
    ) { sessionManager, databaseSource ->
        FileViewViewModel(objectId, sessionManager, databaseSource)
    }

@Composable
fun createFileRefsViewModel(fileId: PrimaryKey) =
    customViewModel(
        listOf("file-refs", fileId),
    ) { sessionManager, databaseSource ->
        FileRefsViewModel(sessionManager, databaseSource, fileId)
    }

@Composable
inline fun <reified VM : ViewModel> customViewModel(
    keys: List<Comparable<*>?>? = null,
    crossinline factory: (SimpleUserSessionManager, ModelStorage) -> VM,
): VM {
    val uiViewModel = LocalUiViewModel.current
    val instance by uiViewModel.instance.collectAsState()
    return customViewModel<VM>(keys, instance, factory)
}

@Composable
inline fun <reified VM : ViewModel> customViewModel(
    keys: List<Comparable<*>?>?,
    accountInstance: IAccountInstance,
    crossinline factory: (SimpleUserSessionManager, ModelStorage) -> VM,
): VM {
    val address = accountInstance.address
    val databaseSource = accountInstance.database
    val sessionManager = accountInstance.sessionManager
    SideEffect {
        Napier.i {
            "viewModel $address ${VM::class.simpleName ?: "<none>"}${keys ?: "<none>"} composable"
        }
    }
    return viewModel(key = "$address::${keys?.joinToString() ?: "<none>"}") {
        Napier.i {
            "viewModel $address ${VM::class.simpleName ?: "<none>"}${keys ?: "<none>"} build"
        }
        factory(sessionManager, databaseSource)
    }
}

@Composable
inline fun <reified VM : ViewModel> customMainViewModel(
    keys: List<Comparable<*>?>? = null,
    crossinline factory: (UserSessionManager, ModelStorage) -> VM,
): VM {
    val uiViewModel = LocalUiViewModel.current
    val instance by uiViewModel.instance.collectAsState()
    return customViewModel<VM>(keys, getMainSessionManager(instance), factory)
}

fun getMainSessionManager(instance: IAccountInstance): IAccountInstance =
    if (instance is IAccountInstance.Child) {
        instance.main
    } else {
        instance
    }
