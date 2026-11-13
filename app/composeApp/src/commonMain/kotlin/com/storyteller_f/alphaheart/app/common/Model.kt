/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.storyteller_f.alphaheart.api.CommunityInfoListResponse
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.error.FormError
import com.storyteller_f.alphaheart.client.compose_core.common.CachedLoadingHandler
import com.storyteller_f.alphaheart.client.compose_core.common.CompatPagingSource
import com.storyteller_f.alphaheart.client.compose_core.common.IntKeyConverter
import com.storyteller_f.alphaheart.client.compose_core.common.IntermediatePagingSource
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.RegularPagingSource
import com.storyteller_f.alphaheart.client.compose_core.common.SectionLoadParams
import com.storyteller_f.alphaheart.client.compose_core.common.SectionPagingSource
import com.storyteller_f.alphaheart.client.compose_core.common.SimpleViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.buildPager
import com.storyteller_f.alphaheart.client.compose_core.components.DialogSaveState
import com.storyteller_f.alphaheart.client.compose_core.utils.SavedSession
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.SimpleLoadingHandler
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getChildAccounts
import com.storyteller_f.alphaheart.client.core.getComments
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getCommunityInfoByAid
import com.storyteller_f.alphaheart.client.core.getFavorites
import com.storyteller_f.alphaheart.client.core.getFileInfo
import com.storyteller_f.alphaheart.client.core.getFileList
import com.storyteller_f.alphaheart.client.core.getFileRefs
import com.storyteller_f.alphaheart.client.core.getMediaByName
import com.storyteller_f.alphaheart.client.core.getQuotaInfo
import com.storyteller_f.alphaheart.client.core.getReactionRecords
import com.storyteller_f.alphaheart.client.core.getReactions
import com.storyteller_f.alphaheart.client.core.getRecommendTopics
import com.storyteller_f.alphaheart.client.core.getRoomInfo
import com.storyteller_f.alphaheart.client.core.getRoomInfoByAid
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getSubscriptions
import com.storyteller_f.alphaheart.client.core.getTopicInfo
import com.storyteller_f.alphaheart.client.core.getTopicInfoByAid
import com.storyteller_f.alphaheart.client.core.getTopicList
import com.storyteller_f.alphaheart.client.core.getUserCommunities
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.getUserInfoByAid
import com.storyteller_f.alphaheart.client.core.getUserJoinedCommunities
import com.storyteller_f.alphaheart.client.core.getUserOverview
import com.storyteller_f.alphaheart.client.core.processEncryptedTopic
import com.storyteller_f.alphaheart.client.core.searchAllMembers
import com.storyteller_f.alphaheart.client.core.searchCommunity
import com.storyteller_f.alphaheart.client.core.searchCommunityMembers
import com.storyteller_f.alphaheart.client.core.searchCommunityTopics
import com.storyteller_f.alphaheart.client.core.searchFiles
import com.storyteller_f.alphaheart.client.core.searchRoomMembers
import com.storyteller_f.alphaheart.client.core.searchRoomTopics
import com.storyteller_f.alphaheart.client.core.searchUserTopics
import com.storyteller_f.alphaheart.client.core.userTitles
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FileRefInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.QuotaDetail
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.model.UserPubKeyInfo
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.extractMarkdownHeadline
import com.storyteller_f.shared.utils.extractMarkdownMediaLink
import com.storyteller_f.storage.ChildAccountStorage
import com.storyteller_f.storage.CommunityCollection
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.FileCollection
import com.storyteller_f.storage.FileRefCollection
import com.storyteller_f.storage.MemberCollection
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.ReactionCollection
import com.storyteller_f.storage.TitleCollection
import com.storyteller_f.storage.TopicCollection
import com.storyteller_f.storage.UploadCollection
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UserCollection
import com.storyteller_f.storage.UserFavoriteCollection
import com.storyteller_f.storage.UserReactionRecordCollection
import com.storyteller_f.storage.UserSubscriptionCollection
import com.storyteller_f.storage.WrappedPagingSource
import com.storyteller_f.storage.getName
import com.storyteller_f.storage.mergeActivity
import com.storyteller_f.storage.wrap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

abstract class CommunityViewModel : SimpleViewModel<CommunityInfo>() {
    val dialog = DialogSaveState()
}

class IdCommunityViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, communityId: PrimaryKey) :
    CommunityViewModel() {
    override val handler: LoadingHandler<CommunityInfo> =
        CachedLoadingHandler(
            modelStorage.community.observeDatum(communityId),
            viewModelScope,
            { t ->
                modelStorage.community.saveToDefault(t)
            },
        ) { sessionManager.getCommunityInfo(communityId) }
}

class AidCommunityViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, aid: String) :
    CommunityViewModel() {
    override val handler: LoadingHandler<CommunityInfo> =
        CachedLoadingHandler(
            modelStorage.community.observeDatum(aid),
            viewModelScope,
            { t ->
                modelStorage.community.saveToDefault(t)
            },
        ) { sessionManager.getCommunityInfoByAid(aid) }
}

@OptIn(ExperimentalPagingApi::class)
class CommunitiesViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    joinStatusSearch: JoinStatusSearch,
    word: String,
    target: PrimaryKey? = null,
) : PagingViewModel<CommunityInfo>() {
    private val modelCollection =
        CommunityCollection.SearchCommunity(joinStatusSearch, word, target, PosterSearch.UNSPECIFIED)

    override val flow: Flow<PagingData<CommunityInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.community,
            RegularPagingSource { key, size ->
                sessionManager.searchCommunity(size, joinStatusSearch, word, target, key, PosterSearch.UNSPECIFIED)
                    .map {
                        CommunityInfoListResponse(
                            it.data.map { item -> item.communityInfo }.toImmutableList(),
                            it.pagination,
                        )
                    }
            },
        ).flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class UserJoinedCommunitiesViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    target: PrimaryKey? = null,
) : PagingViewModel<CommunityInfo>() {
    private val modelCollection =
        CommunityCollection.SearchCommunity(JoinStatusSearch.JOINED, "", target, PosterSearch.UNSPECIFIED)

    override val flow: Flow<PagingData<CommunityInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.community,
            RegularPagingSource { key, size ->
                if (target == null) {
                    sessionManager.getUserCommunities(
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(PosterSearch.UNSPECIFIED, key, size),
                    ).map { response ->
                        CommunityInfoListResponse(
                            response.data.map { it.communityInfo }.toImmutableList(),
                            response.pagination,
                        )
                    }
                } else {
                    sessionManager.getUserJoinedCommunities(
                        target,
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(PosterSearch.UNSPECIFIED, key, size),
                    ).map { response ->
                        CommunityInfoListResponse(
                            response.data.map { it.communityInfo }.toImmutableList(),
                            response.pagination,
                        )
                    }
                }
            },
        ).flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class UserJoinedCommunitiesWithPosterViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<CommunityInfo>() {
    private val modelCollection =
        CommunityCollection.SearchCommunity(JoinStatusSearch.JOINED, "", null, PosterSearch.HAS_POSTER)

    override val flow: Flow<PagingData<CommunityInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.community,
        ) { key, size ->
            sessionManager.getUserCommunities(
                CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(PosterSearch.HAS_POSTER, key, size),
            ).map { response ->
                CommunityInfoListResponse(
                    response.data.map { it.communityInfo }.toImmutableList(),
                    response.pagination,
                )
            }
        }.flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class WorldViewModel(val sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<TopicDetail>() {
    private val modelCollection = TopicCollection.Recommend

    @OptIn(FlowPreview::class)
    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.topic,
            IntermediatePagingSource(
                SectionLoadParams::class,
                SectionPagingSource(
                    listOf(
                        RegularPagingSource { loadKey, size ->
                            sessionManager.getRecommendTopics(PaginationQuery(loadKey, size = size))
                        },
                    ),
                ),
            ),
        ).flow.map { pagingData ->
            pagingData.map {
                extractHeadlineIfPlain(it)
            }
        }.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class TopicsViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    id: PrimaryKey,
    type: ObjectType,
) : PagingViewModel<TopicDetail>() {
    private val modelCollection = TopicCollection.ChildTopicList(id)

    @OptIn(FlowPreview::class)
    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.topic,
            IntermediatePagingSource(
                SectionLoadParams::class,
                SectionPagingSource(
                    listOf(
                        RegularPagingSource { loadKey, size ->
                            sessionManager.getTopicList(
                                type,
                                id,
                                TopicPinSearch.PINNED,
                                PaginationQuery(loadKey, null, size = size),
                            )
                        },
                        RegularPagingSource { loadKey, size ->
                            sessionManager.getTopicList(
                                type,
                                id,
                                TopicPinSearch.UNPINNED,
                                PaginationQuery(loadKey, null, size = size),
                            )
                        },
                    ),
                ),
            ),
        ) {
            WrappedPagingSource(
                CompatPagingSource(modelStorage.topic.observeData(modelCollection), IntKeyConverter),
            ) { topicInfos ->
                processEncryptedTopic(topicInfos, sessionManager).map { topicInfo ->
                    extractHeadlineIfPlain(topicInfo)
                }
            }
        }.flow.cachedIn(viewModelScope)
}

private fun extractHeadlineIfPlain(topic: TopicDetail): TopicDetail {
    val content = topic.content
    return if (content is TopicContent.Plain) {
        val preview = TopicContent.Extracted(extractMarkdownHeadline(content.plain), content.fileInfos, content.plain)
        topic.copy(topicInfo = topic.topicInfo.copy(content = preview))
    } else {
        topic
    }
}

abstract class RoomViewModel : SimpleViewModel<RoomInfo>() {
    val dialog = DialogSaveState()
}

class IdRoomViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, communityId: PrimaryKey) :
    RoomViewModel() {
    override val handler: LoadingHandler<RoomInfo> =
        CachedLoadingHandler(
            modelStorage.room.observeDatum(communityId),
            viewModelScope,
            { t ->
                modelStorage.room.saveToDefault(t)
            },
        ) {
            sessionManager.getRoomInfo(communityId)
        }
}

class AidRoomViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, aid: String) : RoomViewModel() {
    override val handler: LoadingHandler<RoomInfo> =
        CachedLoadingHandler(
            modelStorage.room.observeDatum(aid),
            viewModelScope,
            { t ->
                modelStorage.room.saveToDefault(t)
            },
        ) {
            sessionManager.getRoomInfoByAid(aid)
        }
}

@OptIn(ExperimentalPagingApi::class)
class TopicSearchViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    word: String,
    parentId: PrimaryKey?,
    parentType: ObjectType?,
) : PagingViewModel<TopicDetail>() {
    private val modelCollection = TopicCollection.SearchTopic(word, parentId)

    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.topic,
        ) { key, size ->
            // 内联并确保调用正确的endpoint
            if (parentId != null && parentType != null) {
                when (parentType) {
                    ObjectType.COMMUNITY -> sessionManager.searchCommunityTopics(parentId, size, word, key)
                    ObjectType.ROOM -> sessionManager.searchRoomTopics(parentId, size, word, key)
                    ObjectType.USER -> sessionManager.searchUserTopics(parentId, size, word, key)
                    else -> throw IllegalArgumentException("Unsupported parentType: $parentType")
                }
            } else {
                // 如果没有指定父对象，则搜索推荐主题
                sessionManager.getRecommendTopics(PaginationQuery(key, size = size))
            }
        }.flow.cachedIn(viewModelScope)
}

class MediaListViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    objectId: PrimaryKey,
    objectType: ObjectType,
) : PagingViewModel<FileInfo>() {
    private val modelCollection = FileCollection.FileList(objectId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<FileInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileInfo,
        ) { key, size ->
            sessionManager.getFileList(objectId, objectType, key, size)
        }.flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class FileSearchViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    word: String,
    objectId: PrimaryKey,
    objectType: ObjectType,
) : PagingViewModel<FileInfo>() {
    private val modelCollection = FileCollection.FileSearch(objectId, word)

    override val flow: Flow<PagingData<FileInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileInfo,
        ) { key, size ->
            sessionManager.searchFiles(
                SearchQuery(
                    word = word,
                    nextPageToken = key,
                    size = size,
                ),
                objectId,
                objectType,
            )
        }.flow.cachedIn(viewModelScope)
}

abstract class UserViewModel : SimpleViewModel<UserInfo>()

class IdUserViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, id: PrimaryKey) :
    UserViewModel() {
    override val handler: LoadingHandler<UserInfo> =
        CachedLoadingHandler(
            modelStorage.user.observeDatum(id),
            viewModelScope,
            { t ->
                modelStorage.user.saveToDefault(t)
            },
        ) {
            sessionManager.getUserInfo(id)
        }
}

class AidUserViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, aid: String) : UserViewModel() {
    override val handler: LoadingHandler<UserInfo> =
        CachedLoadingHandler(
            modelStorage.user.observeDatum(aid),
            viewModelScope,
            { t ->
                modelStorage.user.saveToDefault(t)
            },
        ) {
            sessionManager.getUserInfoByAid(aid)
        }
}

@OptIn(ExperimentalPagingApi::class)
class ContainerMemberViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    objectId: PrimaryKey,
    word: String,
    objectType: ObjectType,
) : PagingViewModel<MemberInfo>() {
    private val modelCollection =
        when (objectType) {
            ObjectType.COMMUNITY -> MemberCollection.CommunityMembers(objectId, word)
            ObjectType.ROOM -> MemberCollection.RoomMembers(objectId, word)
            else -> throw IllegalArgumentException("Unsupported objectType: $objectType")
        }

    override val flow: Flow<PagingData<MemberInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.member,
        ) { key, size ->
            sessionManager.run {
                when (objectType) {
                    ObjectType.COMMUNITY -> searchCommunityMembers(objectId, key, size, word)
                    ObjectType.ROOM -> searchRoomMembers(objectId, key, size, word)
                    else -> throw IllegalArgumentException("Unsupported objectType: $objectType")
                }
            }
        }.flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class UserSearchViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, word: String) :
    PagingViewModel<UserInfo>() {
    private val modelCollection = UserCollection.SearchUser(word)

    override val flow: Flow<PagingData<UserInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.user,
        ) { key, size ->
            sessionManager.searchAllMembers(key, size, word)
        }.flow.cachedIn(viewModelScope)
}

class ReactionsViewModel(sessionManager: UserSessionManager, objectId: PrimaryKey, modelStorage: ModelStorage) :
    PagingViewModel<ReactionInfo>() {
    val modelCollection = ReactionCollection.ReactionList(objectId)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<ReactionInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.reaction,
        ) { key, size ->
            sessionManager.getReactions(objectId, size, key)
        }.flow.cachedIn(viewModelScope)
}

abstract class TopicViewModel : SimpleViewModel<TopicDetail>()

class IdTopicViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, topicId: PrimaryKey) :
    TopicViewModel() {
    override val handler: LoadingHandler<TopicDetail> =
        CachedLoadingHandler(
            modelStorage.topic.observeDatum(topicId),
            viewModelScope,
            { t ->
                modelStorage.topic.saveToDefault(t)
            },
        ) {
            sessionManager.getTopicInfo(topicId).map { topicInfo ->
                processEncryptedTopic(listOf(topicInfo), sessionManager).first()
            }
        }
}

class AidTopicViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, aid: String) :
    TopicViewModel() {
    override val handler: LoadingHandler<TopicDetail> =
        CachedLoadingHandler(
            modelStorage.topic.observeDatum(aid),
            viewModelScope,
            { t ->
                modelStorage.topic.saveToDefault(t)
            },
        ) {
            sessionManager.getTopicInfoByAid(aid).map { topicInfo ->
                processEncryptedTopic(listOf(topicInfo), sessionManager).first()
            }
        }
}

class RoomKeysViewModel(sessionManager: UserSessionManager, private val id: PrimaryKey, val private: Boolean) :
    SimpleViewModel<List<UserPubKeyInfo>>() {
    override val handler: LoadingHandler<List<UserPubKeyInfo>> =
        SimpleLoadingHandler(viewModelScope) {
            captureFailure(FormError::from) {
                if (!private) return@captureFailure emptyList()
                val result = mutableListOf<UserPubKeyInfo>()
                var last: String? = null
                while (true) {
                    val list =
                        sessionManager.getRoomMembersPublicKeys(
                            this@RoomKeysViewModel.id,
                            PaginationQuery(last, size = 100),
                        )
                            .getOrThrow()
                    result.addAll(list.data)
                    val nextKey = list.pagination?.nextPageToken ?: break
                    last = nextKey
                }
                result
            }
        }
}

@OptIn(ExperimentalPagingApi::class)
class TitlesViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    uid: PrimaryKey,
    searchType: TitleSearchType,
    status: TitleWorkStatus? = null,
    type: TitleType? = null,
    scopeId: PrimaryKey? = null,
) : PagingViewModel<TitleDetail>() {
    private val modelCollection = TitleCollection.SearchTitle(uid, searchType, status, type, scopeId)

    override val flow: Flow<PagingData<TitleDetail>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.title,
        ) { key, size ->
            sessionManager.userTitles(uid, size, searchType, key, status, type, scopeId)
        }.flow.cachedIn(viewModelScope)
}

class UploadHandler(private val scope: CoroutineScope, private val load: suspend UploadHandler.() -> Unit) {
    val state: MutableStateFlow<LoadingState?> = MutableStateFlow(null)

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            load()
        }
    }
}

class UploadViewModel(myUid: PrimaryKey, modelStorage: ModelStorage) : PagingViewModel<UploadInfo>() {
    override val flow: Flow<PagingData<UploadInfo>> =
        Pager(PagingConfig(10)) {
            modelStorage.upload.observeData(UploadCollection(myUid))
        }.flow.cachedIn(viewModelScope)
}

class UploadDetailViewModel(modelStorage: ModelStorage, pathHash: String, myUid: PrimaryKey) : ViewModel() {
    val data = modelStorage.upload.observeDatumByHash(UploadCollection(myUid), pathHash)
}

class DownloadListViewModel(records: com.storyteller_f.storage.DownloadInfoStorage) : PagingViewModel<DownloadInfo>() {
    override val flow =
        Pager(PagingConfig(10)) {
            records.observeData()
        }.flow.cachedIn(viewModelScope)
}

class QuotaViewModel(sessionManager: UserSessionManager, objectTuple: ObjectTuple, quotaType: QuotaType) :
    SimpleViewModel<QuotaDetail>() {
    override val handler: LoadingHandler<QuotaDetail> =
        SimpleLoadingHandler(viewModelScope) {
            sessionManager.getQuotaInfo(objectTuple, quotaType)
        }
}

class DownloadHandler<T>(
    flow: Flow<T?>,
    private val scope: CoroutineScope,
    private val load: suspend DownloadHandler<T>.() -> Unit,
) : LoadingHandler<T> {
    override val state: MutableStateFlow<LoadingState?> = MutableStateFlow(null)

    @OptIn(FlowPreview::class)
    override val data = flow.stateIn(scope, SharingStarted.Lazily, null)

    init {
        refresh()
    }

    override fun refresh() {
        scope.launch {
            load()
        }
    }
}

class DownloadViewModel(records: com.storyteller_f.storage.DownloadInfoStorage, fileId: PrimaryKey?) : ViewModel() {
    val data = fileId?.let { records.observeDocumentByFileId(it) } ?: MutableStateFlow(null)
}

class MarkdownMediasViewModel(val sessionManager: UserSessionManager, content: String, objectTuple: ObjectTuple) :
    SimpleViewModel<List<FileInfo>>() {
    override val handler: LoadingHandler<List<FileInfo>> =
        SimpleLoadingHandler(viewModelScope) {
            captureFailure(FormError::from) {
                extractMarkdownMediaLink(content).map {
                    sessionManager.getMediaByName(it, objectTuple.objectId, objectTuple.objectType)
                        .getOrThrow()
                }
            }
        }
}

class ChildAccountsViewModel(modelStorage: ModelStorage, sessionManager: UserSessionManager) :
    PagingViewModel<ChildAccountInfo>() {
    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<ChildAccountInfo>> =
        buildPager(
            modelStorage.remoteKey.wrap(ChildAccountStorage.COLLECTION_NAME),
            modelStorage.childAccount,
        ) { key, size ->
            sessionManager.getChildAccounts(key, size)
        }.flow.cachedIn(viewModelScope)
}

class SessionHistoryViewModel(uIViewModel: UIViewModel) : SimpleViewModel<SavedSession>() {
    val manager = uIViewModel.historyManager

    override val handler: LoadingHandler<SavedSession> =
        SimpleLoadingHandler(viewModelScope) {
            captureFailure(FormError::from) {
                manager.getSavedSession()
            }
        }

    fun deleteSession(alias: String) {
        viewModelScope.launch { manager.removeSession(alias) }
    }
}

class FavoritesViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<UserFavoriteItem>() {
    private val collection = UserFavoriteCollection.UserFavorites(0)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UserFavoriteItem>> =
        buildPager(
            collection,
            modelStorage.remoteKey.wrap(collection.getName()),
            modelStorage.favorite,
        ) { key, size ->
            sessionManager.getFavorites(PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class SubscriptionsViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<UserSubscriptionItem>() {
    private val collection = UserSubscriptionCollection.UserSubscriptions(0)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<UserSubscriptionItem>> =
        buildPager(
            collection,
            modelStorage.remoteKey.wrap(collection.getName()),
            modelStorage.subscription,
            RegularPagingSource { key, size ->
                sessionManager.getSubscriptions(PaginationQuery(key, size = size))
            },
        ) {
            WrappedPagingSource(
                CompatPagingSource(modelStorage.subscription.observeData(collection), IntKeyConverter),
            ) { list ->
                list.map { subscriptionInfo ->
                    val topicInfo = subscriptionInfo.topicInfo
                    if (topicInfo != null) {
                        val newTopic = processEncryptedTopic(listOf(topicInfo), sessionManager).first()
                        subscriptionInfo.copy(topicInfo = newTopic)
                    } else {
                        subscriptionInfo
                    }
                }
            }
        }.flow.cachedIn(viewModelScope)
}

class UserReactionRecordsViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<ReactionRecordInfo>() {
    val collection = UserReactionRecordCollection.UserReactionRecords(0)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<ReactionRecordInfo>> =
        buildPager(
            collection,
            modelStorage.remoteKey.wrap(collection.getName()),
            modelStorage.userReactionRecord,
            RegularPagingSource { key, size ->
                sessionManager.getReactionRecords(PaginationQuery(key, size = size))
            },
        ).flow.cachedIn(viewModelScope)
}

class UserCommentsViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    PagingViewModel<TopicDetail>() {
    val collection = TopicCollection.UserComments(0)

    @OptIn(ExperimentalPagingApi::class)
    override val flow: Flow<PagingData<TopicDetail>> =
        buildPager(
            collection,
            modelStorage.remoteKey.wrap(collection.getName()),
            modelStorage.topic,
            RegularPagingSource { key, size ->
                sessionManager.getComments(PaginationQuery(key, size = size)).map { response ->
                    com.storyteller_f.alphaheart.api.TopicInfoListResponse(
                        response.data.map { summary ->
                            modelStorage.topic.mergeActivity(summary)
                        }.toImmutableList(),
                        response.pagination,
                    )
                }
            },
        ) {
            WrappedPagingSource(
                CompatPagingSource(modelStorage.topic.observeData(collection), IntKeyConverter),
            ) { list ->
                processEncryptedTopic(list, sessionManager)
            }
        }.flow.cachedIn(viewModelScope)
}

class UserOverviewViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    SimpleViewModel<UserOverview>() {
    override val handler: LoadingHandler<UserOverview> =
        CachedLoadingHandler(
            modelStorage.userOverview.observeDatum(),
            viewModelScope,
            {
                modelStorage.userOverview.save(it)
            },
        ) {
            if (sessionManager.currentIsAlreadySignUp) {
                sessionManager.getUserOverview()
            } else {
                CustomResult.Failure(FormError.from(IllegalStateException("not logged in")))
            }
        }
}

@OptIn(ExperimentalPagingApi::class)
class FileRefsViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage, fileId: PrimaryKey) :
    PagingViewModel<FileRefInfo>() {
    private val modelCollection = FileRefCollection.FileRefs(fileId)

    override val flow: Flow<PagingData<FileRefInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.fileRef,
        ) { key, size ->
            sessionManager.getFileRefs(fileId, PaginationQuery(key, size = size))
        }.flow.cachedIn(viewModelScope)
}

class FileViewViewModel(fileId: PrimaryKey, sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    SimpleViewModel<FileInfo>() {
    override val handler: LoadingHandler<FileInfo> =
        CachedLoadingHandler(
            modelStorage.fileInfo.observeDatum(fileId),
            viewModelScope,
            {
                modelStorage.fileInfo.saveToDefault(it)
            },
        ) {
            sessionManager.getFileInfo(fileId)
        }
}

enum class TitleComposeSheetType { NONE, SCOPE, RECEIVER }

class TitleComposeViewModel(
    initialScope: ObjectTuple? = null,
    initialType: TitleType? = null,
    lockScope: Boolean = false,
    lockType: Boolean = false,
) : ViewModel() {
    val name = MutableStateFlow("")
    val showSheetType = MutableStateFlow(TitleComposeSheetType.NONE)
    val titleScope = MutableStateFlow<ObjectTuple?>(initialScope)
    val titleType = MutableStateFlow(initialType ?: TitleType.REGULAR)
    val receiver = MutableStateFlow<PrimaryKey?>(null)
    val content = MutableStateFlow("")
    val expiresAtText = MutableStateFlow("")
    val isScopeLocked = MutableStateFlow(lockScope)
    val isTypeLocked = MutableStateFlow(lockType)

    fun setName(value: String) {
        name.value = value
    }

    fun setTitleType(value: TitleType) {
        titleType.value = value
    }

    fun setTitleScope(value: ObjectTuple?) {
        titleScope.value = value
    }

    fun clearTitleScope() {
        titleScope.value = null
    }

    fun setReceiver(value: PrimaryKey?) {
        receiver.value = value
    }

    fun setContent(value: String) {
        content.value = value
    }

    fun setExpiresAtText(value: String) {
        expiresAtText.value = value
    }

    fun setShowSheetType(value: TitleComposeSheetType) {
        showSheetType.value = value
    }

    fun clearShowSheetType() {
        showSheetType.value = TitleComposeSheetType.NONE
    }

    // Readability helpers (Flow-based for Compose collectAsState)
    val isSheetVisibleFlow: StateFlow<Boolean> =
        showSheetType
            .map { it != TitleComposeSheetType.NONE }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val supportedObjectTypesFlow: StateFlow<List<ObjectType>> =
        showSheetType
            .map {
                if (it == TitleComposeSheetType.SCOPE) {
                    listOf(ObjectType.COMMUNITY, ObjectType.ROOM)
                } else {
                    listOf(ObjectType.USER)
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(ObjectType.USER))

    fun openScopeSheet() = setShowSheetType(TitleComposeSheetType.SCOPE)
    fun openReceiverSheet() = setShowSheetType(TitleComposeSheetType.RECEIVER)

    fun buildNewTitle(): CustomResult<NewTitle, FormError> {
        val t = titleType.value
        val r = receiver.value
        val s = titleScope.value
        val expiresAt = expiresAtText.value.trim()
        val parsedExpiresAt =
            if (expiresAt.isEmpty()) {
                CustomResult.success(null)
            } else {
                captureFailure(FormError::from) {
                    LocalDateTime.parse(expiresAt)
                }.mapError {
                    FormError.InvalidInput(
                        IllegalArgumentException("Invalid expiresAt format, use yyyy-MM-ddTHH:mm:ss"),
                    )
                }
            }
        return parsedExpiresAt.andThen(FormError::fromError) { expiresAtValue ->
            captureFailure(FormError::from) {
                check(r != null && s != null) {
                    "titleType/receiver/titleScope must not be null"
                }
                NewTitle(name.value, t, r, s.objectId, s.objectType, content.value, expiresAtValue)
            }
        }.mapError(FormError::fromError)
    }
}
