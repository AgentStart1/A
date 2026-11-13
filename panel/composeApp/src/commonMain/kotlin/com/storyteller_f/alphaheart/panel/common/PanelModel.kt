/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import androidx.lifecycle.*
import androidx.paging.*
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.compose_core.common.CachedLoadingHandler
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.SimpleViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.buildPager
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.PanelSessionManager
import com.storyteller_f.alphaheart.client.core.SimpleLoadingHandler
import com.storyteller_f.alphaheart.client.core.getAllCommunities
import com.storyteller_f.alphaheart.client.core.getAllFiles
import com.storyteller_f.alphaheart.client.core.getAllPrivateRooms
import com.storyteller_f.alphaheart.client.core.getAllPublicRooms
import com.storyteller_f.alphaheart.client.core.getAllTitles
import com.storyteller_f.alphaheart.client.core.getAllTopics
import com.storyteller_f.alphaheart.client.core.getAllUsers
import com.storyteller_f.alphaheart.client.core.getCommunityById
import com.storyteller_f.alphaheart.client.core.getCommunityMembers
import com.storyteller_f.alphaheart.client.core.getFileById
import com.storyteller_f.alphaheart.client.core.getFileRefs
import com.storyteller_f.alphaheart.client.core.getPanelLogs
import com.storyteller_f.alphaheart.client.core.getRoomById
import com.storyteller_f.alphaheart.client.core.getRoomFiles
import com.storyteller_f.alphaheart.client.core.getRoomMembers
import com.storyteller_f.alphaheart.client.core.getTaskRecordSummaries
import com.storyteller_f.alphaheart.client.core.getTaskRecords
import com.storyteller_f.alphaheart.client.core.getTitleById
import com.storyteller_f.alphaheart.client.core.getTopicById
import com.storyteller_f.alphaheart.client.core.getTopicTopics
import com.storyteller_f.alphaheart.client.core.getUserById
import com.storyteller_f.alphaheart.client.core.getUserComments
import com.storyteller_f.alphaheart.client.core.getUserFavorites
import com.storyteller_f.alphaheart.client.core.getUserFiles
import com.storyteller_f.alphaheart.client.core.getUserJoinedCommunities
import com.storyteller_f.alphaheart.client.core.getUserJoinedRooms
import com.storyteller_f.alphaheart.client.core.getUserLogs
import com.storyteller_f.alphaheart.client.core.getUserOverview
import com.storyteller_f.alphaheart.client.core.getUserReactions
import com.storyteller_f.alphaheart.client.core.getUserReceivedTitles
import com.storyteller_f.alphaheart.client.core.getUserSubscriptions
import com.storyteller_f.alphaheart.client.core.getUserUploadRecords
import com.storyteller_f.alphaheart.client.core.getWorkerTasks
import com.storyteller_f.alphaheart.client.core.markTaskRecordForRetry
import com.storyteller_f.alphaheart.client.core.overview
import com.storyteller_f.alphaheart.client.core.updateWorkerTask
import com.storyteller_f.result.fold
import com.storyteller_f.result.map
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.Type2Algo
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.model.PanelOverview
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TaskRecordSummary
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.model.UploadRecordInfo
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogItem
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.CommunityCollection
import com.storyteller_f.storage.FileCollection
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.TitleCollection
import com.storyteller_f.storage.TopicCollection
import com.storyteller_f.storage.UserCollection
import com.storyteller_f.storage.UserFavoriteCollection
import com.storyteller_f.storage.UserReactionRecordCollection
import com.storyteller_f.storage.UserSubscriptionCollection
import com.storyteller_f.storage.getName
import com.storyteller_f.storage.mergeActivity
import com.storyteller_f.storage.wrap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AllUsersViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<UserInfo>() {
    val modelCollection = UserCollection.AllUsers

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UserInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.user,
        ) { key, size ->
            sessionManager.getAllUsers(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class AllCommunitiesViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<CommunityInfo>() {
    private val modelCollection = CommunityCollection.AllCommunities

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<CommunityInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.community,
        ) { key, size ->
            sessionManager.getAllCommunities(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class AllPublicRoomsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<RoomInfo>() {
    private val modelCollection = RoomCollection.AllRooms(false)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
        ) { key, size ->
            sessionManager.getAllPublicRooms(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class AllPrivateRoomsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<RoomInfo>() {
    private val modelCollection = RoomCollection.AllRooms(true)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
        ) { key, size ->
            sessionManager.getAllPrivateRooms(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class AllTopicsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<TopicDetail>() {
    private val modelCollection = TopicCollection.AllTopics

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.topic,
        ) { key, size ->
            sessionManager.getAllTopics(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class AllTitlesViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<TitleDetail>() {
    private val modelCollection = TitleCollection.AllTitles

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TitleDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.title,
        ) { key, size ->
            sessionManager.getAllTitles(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class AllFilesViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<FileInfo>() {
    private val modelCollection = FileCollection.FileList(0)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<FileInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileInfo,
        ) { key, size ->
            sessionManager.getAllFiles(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class OverviewViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage) :
    SimpleViewModel<PanelOverview>() {
    override val handler: LoadingHandler<PanelOverview> =
        CachedLoadingHandler(
            modelStorage.overview.observeDatum(),
            viewModelScope,
            {
                modelStorage.overview.save(it)
            },
        ) {
            sessionManager.overview()
        }
}

class AddUserViewModel : ViewModel() {
    val privateKey = MutableStateFlow("")
    val nickname = MutableStateFlow("")
    val aid = MutableStateFlow("")
    val algoType = MutableStateFlow(AlgoType.P256)
    val encryptionPrivateKey = MutableStateFlow("")
    val encryptionPublicKey =
        encryptionPrivateKey.combine(algoType) { pk, algo ->
            if (algo != AlgoType.DILITHIUM || pk.isBlank()) return@combine null
            val encryptionAlgo = getAlgo(algo).encryptionAlgo as? Type2Algo
            encryptionAlgo?.getDerEncryptionPublicKeyFromPemPrivateKey(pk)?.getOrNull()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val publicKey =
        privateKey.combine(algoType) { pk, algo ->
            getAlgo(algo).run {
                getDerPublicKeyFromPrivateKey(pk).getOrNull()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val address =
        publicKey.combine(algoType) { pub, algo ->
            getAlgo(algo).run {
                pub?.let { derPublicKeyStr -> calcAddress(derPublicKeyStr).getOrNull() }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updateNickname(nickname: String) {
        this.nickname.value = nickname
    }

    fun updateAid(aid: String) {
        this.aid.value = aid
    }

    fun updatePrivateKey(privateKey: String) {
        this.privateKey.value = privateKey
    }

    fun updateEncryptionPrivateKey(privateKey: String) {
        this.encryptionPrivateKey.value = privateKey
    }

    fun updateAlgoType(algoType: AlgoType) {
        this.algoType.value = algoType
        if (algoType != AlgoType.DILITHIUM) {
            encryptionPrivateKey.value = ""
        }
    }

    fun autoGeneratePrivateKey() {
        viewModelScope.launch {
            val algo = getAlgo(algoType.value)
            algo.generatePemKeyPair().onSuccess { (privateKey, _) ->
                updatePrivateKey(privateKey)
            }
            val encryptionAlgo = algo.encryptionAlgo as Type2Algo
            encryptionAlgo.generateEncryptionPemKeyPair().onSuccess { (pemPrivateKey, _) ->
                updateEncryptionPrivateKey(pemPrivateKey)
            }
        }
    }
}

class IdUserViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<UserInfo>() {
    override val handler: LoadingHandler<UserInfo> =
        CachedLoadingHandler(
            modelStorage.user.observeDatum(id),
            viewModelScope,
            { modelStorage.user.saveToDefault(it) },
        ) {
            sessionManager.getUserById(id)
        }
}

class IdUserOverviewViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<UserOverview>() {
    override val handler: LoadingHandler<UserOverview> =
        CachedLoadingHandler(
            modelStorage.userOverview.observeDatum(),
            viewModelScope,
            { modelStorage.userOverview.save(it) },
        ) {
            sessionManager.getUserOverview(id)
        }
}

class IdCommunityViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<CommunityInfo>() {
    override val handler: LoadingHandler<CommunityInfo> =
        CachedLoadingHandler(
            modelStorage.community.observeDatum(id),
            viewModelScope,
            { modelStorage.community.saveToDefault(it) },
        ) {
            sessionManager.getCommunityById(id)
        }
}

class IdRoomViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<RoomInfo>() {
    override val handler: LoadingHandler<RoomInfo> =
        CachedLoadingHandler(
            modelStorage.room.observeDatum(id),
            viewModelScope,
            { modelStorage.room.saveToDefault(it) },
        ) {
            sessionManager.getRoomById(id)
        }
}

class IdTopicViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<TopicDetail>() {
    override val handler: LoadingHandler<TopicDetail> =
        CachedLoadingHandler(
            modelStorage.topic.observeDatum(id),
            viewModelScope,
            { modelStorage.topic.saveToDefault(it) },
        ) {
            sessionManager.getTopicById(id)
        }
}

class IdFileViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<FileInfo>() {
    override val handler: LoadingHandler<FileInfo> =
        CachedLoadingHandler(
            modelStorage.fileInfo.observeDatum(id),
            viewModelScope,
            { modelStorage.fileInfo.saveToDefault(it) },
        ) {
            sessionManager.getFileById(id)
        }
}

class FileRefsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, fileId: PrimaryKey) :
    PagingViewModel<com.storyteller_f.shared.model.FileRefInfo>() {
    private val modelCollection = com.storyteller_f.storage.FileRefCollection.FileRefs(fileId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<com.storyteller_f.shared.model.FileRefInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileRef,
        ) { key, size ->
            sessionManager.getFileRefs(fileId, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class IdTitleViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    SimpleViewModel<TitleDetail>() {
    override val handler: LoadingHandler<TitleDetail> =
        CachedLoadingHandler(
            modelStorage.title.observeDatum(id),
            viewModelScope,
            { modelStorage.title.saveToDefault(it) },
        ) {
            sessionManager.getTitleById(id)
        }
}

class UserJoinedCommunitiesViewModel(
    sessionManager: PanelSessionManager,
    modelStorage: ModelStorage,
    uid: PrimaryKey,
) : PagingViewModel<CommunityInfo>() {
    private val modelCollection =
        CommunityCollection.SearchCommunity(
            joinStatusSearch = JoinStatusSearch.JOINED,
            word = "",
            target = uid,
        )

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<CommunityInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.community,
        ) { key, size ->
            sessionManager.getUserJoinedCommunities(
                uid,
                CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(
                    com.storyteller_f.shared.model.PosterSearch.UNSPECIFIED,
                    key,
                    size = size,
                ),
            ).map { response ->
                com.storyteller_f.alphaheart.api.CommunityInfoListResponse(
                    response.data.map { it.communityInfo }.toImmutableList(),
                    response.pagination,
                )
            }
        }.flow.cachedIn(viewModelScope)
}

class UserJoinedRoomsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<RoomInfo>() {
    private val modelCollection =
        RoomCollection.SearchRoom(
            word = "",
            communityId = null,
            joinStatusSearch = JoinStatusSearch.JOINED,
        )

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
        ) { key, size ->
            sessionManager.getUserJoinedRooms(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class UserReceivedTitlesViewModel(
    sessionManager: PanelSessionManager,
    modelStorage: ModelStorage,
    private val uid: PrimaryKey,
) : PagingViewModel<TitleDetail>() {
    private val modelCollection =
        TitleCollection.SearchTitle(uid = uid, searchType = TitleSearchType.RECEIVER)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TitleDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.title,
        ) { key, size ->
            sessionManager.getUserReceivedTitles(
                uid,
                CustomApi.Users.Id.Titles.TitleQuery(
                    searchType = TitleSearchType.RECEIVER,
                    nextPageToken = key,
                    size = size,
                ),
            )
        }.flow.cachedIn(viewModelScope)
}

class UserFilesViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<FileInfo>() {
    private val modelCollection = FileCollection.FileList(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<FileInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileInfo,
        ) { key, size ->
            sessionManager.getUserFiles(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class UserLogsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<UserLogItem>() {
    private val modelCollection = com.storyteller_f.storage.UserLogCollection.UserLogs(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UserLogItem>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.userLog,
        ) { key, size ->
            sessionManager.getUserLogs(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class UserUploadRecordsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<UploadRecordInfo>() {
    private val modelCollection =
        com.storyteller_f.storage.UploadRecordCollection.UserUploadRecords(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UploadRecordInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.uploadRecord,
        ) { key, size ->
            sessionManager.getUserUploadRecords(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class UserReactionsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<ReactionRecordInfo>() {
    private val modelCollection = UserReactionRecordCollection.UserReactionRecords(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<ReactionRecordInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.userReactionRecord,
        ) { key, size ->
            sessionManager.getUserReactions(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class UserCommentsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<TopicDetail>() {
    private val modelCollection = TopicCollection.UserComments(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.topic,
        ) { key, size ->
            sessionManager.getUserComments(uid, PaginationQuery(key, size = size)).map { response ->
                com.storyteller_f.alphaheart.api.TopicInfoListResponse(
                    response.data.map { summary ->
                        modelStorage.topic.mergeActivity(summary)
                    }.toImmutableList(),
                    response.pagination,
                )
            }
        }.flow.cachedIn(viewModelScope)
}

class UserFavoritesViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<UserFavoriteItem>() {
    private val modelCollection = UserFavoriteCollection.UserFavorites(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UserFavoriteItem>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.favorite,
        ) { key, size ->
            sessionManager.getUserFavorites(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class UserSubscriptionsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, uid: PrimaryKey) :
    PagingViewModel<UserSubscriptionItem>() {
    private val modelCollection = UserSubscriptionCollection.UserSubscriptions(uid)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UserSubscriptionItem>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.subscription,
        ) { key, size ->
            sessionManager.getUserSubscriptions(uid, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class CommunityMembersViewModel(
    sessionManager: PanelSessionManager,
    modelStorage: ModelStorage,
    communityId: PrimaryKey,
) : PagingViewModel<MemberInfo>() {
    private val modelCollection =
        com.storyteller_f.storage.MemberCollection.CommunityMembers(communityId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<MemberInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.member,
        ) { key, size ->
            sessionManager.getCommunityMembers(communityId, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class RoomMembersViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, roomId: PrimaryKey) :
    PagingViewModel<MemberInfo>() {
    private val modelCollection = com.storyteller_f.storage.MemberCollection.RoomMembers(roomId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<MemberInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.member,
        ) { key, size ->
            sessionManager.getRoomMembers(roomId, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class RoomFilesViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, roomId: PrimaryKey) :
    PagingViewModel<FileInfo>() {
    private val modelCollection = FileCollection.FileList(roomId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<FileInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileInfo,
        ) { key, size ->
            sessionManager.getRoomFiles(roomId, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class TopicTopicsViewModel(sessionManager: PanelSessionManager, modelStorage: ModelStorage, topicId: PrimaryKey) :
    PagingViewModel<TopicDetail>() {
    private val modelCollection = TopicCollection.ChildTopicList(topicId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.topic,
        ) { key, size ->
            sessionManager.getTopicTopics(
                topicId,
                TopicPinSearch.UNSPECIFIED,
                PaginationQuery(key, size = size),
            )
        }.flow.cachedIn(viewModelScope)
}

class PanelLogsViewModel(
    sessionManager: PanelSessionManager,
    modelStorage: ModelStorage,
    targetId: PrimaryKey,
    objectType: com.storyteller_f.shared.type.ObjectType,
) : PagingViewModel<PanelLogInfo>() {
    private val modelCollection = com.storyteller_f.storage.PanelLogCollection.PanelLogs(targetId, objectType)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<PanelLogInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.panelLog,
        ) { key, size ->
            sessionManager.getPanelLogs(targetId, objectType, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class TaskRecordsViewModel(
    sessionManager: PanelSessionManager,
    modelStorage: ModelStorage,
    type: TaskRecordType?,
    isSuccess: Boolean?,
    failureType: String?,
) : PagingViewModel<TaskRecordInfo>() {
    private val modelCollection =
        com.storyteller_f.storage.TaskRecordCollection.TaskRecords(
            type,
            isSuccess,
            failureType,
        )
    private val sessionManager = sessionManager
    private val _retryRequestedIds = MutableStateFlow(emptySet<PrimaryKey>())

    /** IDs optimistically marked for retry during this screen session. */
    val retryRequestedIds: StateFlow<Set<PrimaryKey>> = _retryRequestedIds.asStateFlow()

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TaskRecordInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.taskRecord,
        ) { key, size ->
            sessionManager.getTaskRecords(
                type = type,
                query = PaginationQuery(key, size = size),
                isSuccess = isSuccess,
                failureType = failureType,
            )
        }.flow.cachedIn(viewModelScope)

    /** Requests that the worker retry a failed task execution. */
    fun markForRetry(id: PrimaryKey) {
        viewModelScope.launch {
            sessionManager.markTaskRecordForRetry(id).onSuccess {
                _retryRequestedIds.value = _retryRequestedIds.value + id
            }
        }
    }
}

internal class TaskRecordSummariesViewModel(sessionManager: PanelSessionManager) :
    SimpleViewModel<List<TaskRecordSummary>>() {
    override val handler: LoadingHandler<List<TaskRecordSummary>> =
        SimpleLoadingHandler(viewModelScope) {
            sessionManager.getTaskRecordSummaries().map { it.data }
        }
}

internal data class WorkerTaskEditorState(
    val type: TaskRecordType,
    val isEnabled: Boolean,
    val fetchSize: String,
    val waitDurationMillis: String,
    val isSaving: Boolean = false,
)

internal data class WorkerTasksUiState(
    val isLoading: Boolean = true,
    val configs: List<WorkerTaskEditorState> = emptyList(),
    val error: String? = null,
)

internal class WorkerTasksViewModel(private val sessionManager: PanelSessionManager) : ViewModel() {
    private val mutableUiState = MutableStateFlow(WorkerTasksUiState())
    val uiState: StateFlow<WorkerTasksUiState> = mutableUiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, error = null) }
            sessionManager.getWorkerTasks().fold(
                onSuccess = { response ->
                    mutableUiState.value =
                        WorkerTasksUiState(
                            isLoading = false,
                            configs = response.data.sortedBy { it.type.ordinal }.map(WorkerTask::toEditorState),
                        )
                },
                onFailure = { failure ->
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            error = failure.cause.message ?: "Failed to load task configurations",
                        )
                    }
                },
            )
        }
    }

    fun updateEnabled(type: TaskRecordType, isEnabled: Boolean) {
        updateEditor(type) { it.copy(isEnabled = isEnabled) }
    }

    fun updateFetchSize(type: TaskRecordType, fetchSize: String) {
        updateEditor(type) { it.copy(fetchSize = fetchSize) }
    }

    fun updateWaitDuration(type: TaskRecordType, waitDurationMillis: String) {
        updateEditor(type) { it.copy(waitDurationMillis = waitDurationMillis) }
    }

    fun save(type: TaskRecordType) {
        val editor = mutableUiState.value.configs.firstOrNull { it.type == type } ?: return
        val config = editor.toWorkerTaskOrNull()
        if (config == null) {
            mutableUiState.update { it.copy(error = "Fetch size and wait duration must be greater than zero") }
            return
        }
        viewModelScope.launch {
            setSaving(type, true)
            sessionManager.updateWorkerTask(config).fold(
                onSuccess = { savedConfig ->
                    updateEditor(type) { savedConfig.toEditorState() }
                    mutableUiState.update { it.copy(error = null) }
                },
                onFailure = { failure ->
                    setSaving(type, false)
                    mutableUiState.update {
                        it.copy(error = failure.cause.message ?: "Failed to save task configuration")
                    }
                },
            )
        }
    }

    private fun setSaving(type: TaskRecordType, isSaving: Boolean) {
        updateEditor(type) { it.copy(isSaving = isSaving) }
    }

    private fun updateEditor(type: TaskRecordType, transform: (WorkerTaskEditorState) -> WorkerTaskEditorState) {
        mutableUiState.update { state ->
            val configs =
                state.configs.map { editor ->
                    if (editor.type == type) transform(editor) else editor
                }
            state.copy(configs = configs)
        }
    }
}

private fun WorkerTask.toEditorState(): WorkerTaskEditorState {
    val editorState =
        WorkerTaskEditorState(
            type = type,
            isEnabled = isEnabled,
            fetchSize = fetchSize.toString(),
            waitDurationMillis = waitDurationMillis.toString(),
        )
    return editorState
}

private fun WorkerTaskEditorState.toWorkerTaskOrNull(): WorkerTask? {
    val parsedFetchSize = fetchSize.toIntOrNull()?.takeIf { it > 0 } ?: return null
    val parsedWaitDuration = waitDurationMillis.toLongOrNull()?.takeIf { it > 0 } ?: return null
    return WorkerTask(
        type = type,
        isEnabled = isEnabled,
        fetchSize = parsedFetchSize,
        waitDurationMillis = parsedWaitDuration,
    )
}
