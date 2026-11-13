/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.UnreadMessageCountResponse
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.SimpleViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.buildPager
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.SimpleLoadingHandler
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getCommunityRooms
import com.storyteller_f.alphaheart.client.core.getUnreadMessageCount
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.searchCommunityRooms
import com.storyteller_f.alphaheart.client.core.searchCurrentUserRooms
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.getName
import com.storyteller_f.storage.wrap
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

// One application-owned serial dispatcher; requests and text processing run on workers.
internal val previewCoordination = Dispatchers.Default.limitedParallelism(1)

/** Local room paging persists metadata only; activity stays in its session's memory. */
abstract class RoomListViewModel : PagingViewModel<RoomInfo>() {
    internal val previews = RoomListPreviewHost(previewCoordination, Dispatchers.Default)

    protected suspend fun <E> captureRoomPage(key: String?, response: CustomResult<RoomInfoListResponse, E>) =
        response.onSuccess { previews.acceptPage(key, it) }
}

/** Receives complete page responses; it never makes a supplementary room request. */
internal class RoomListPreviewHost(
    private val coordination: CoroutineDispatcher,
    private val worker: CoroutineDispatcher,
) {
    private val mutableRows = MutableStateFlow<ImmutableMap<PrimaryKey, RoomActivityPreview>>(persistentMapOf())
    val rows = mutableRows.asStateFlow()
    private var stagedRows: ImmutableMap<PrimaryKey, RoomActivityPreview>? = null

    suspend fun acceptPage(key: String?, response: RoomInfoListResponse) {
        val previews = withContext(worker) { response.activities.associate { it.room.id to it.toPreview() } }
        withContext(coordination) {
            val previous = if (key == null) persistentMapOf() else stagedRows ?: mutableRows.value
            stagedRows = (previous + previews).toImmutableMap()
        }
    }
    suspend fun publish() =
        withContext(coordination) {
            stagedRows?.let { mutableRows.value = it }
            stagedRows = null
        }
}

@OptIn(ExperimentalPagingApi::class)
class RoomsViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    joinStatusSearch: JoinStatusSearch,
    word: String,
) : RoomListViewModel() {
    private val modelCollection = RoomCollection.SearchRoom(word, null, joinStatusSearch)

    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
            onUpdated = previews::publish,
        ) { key, size ->
            captureRoomPage(key, sessionManager.searchCurrentUserRooms(word, size, key))
        }.flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class UserJoinedRoomsViewModel(sessionManager: UserSessionManager, modelStorage: ModelStorage) :
    RoomListViewModel() {
    private val modelCollection = RoomCollection.SearchRoom("", null, JoinStatusSearch.JOINED)

    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
            onUpdated = previews::publish,
        ) { key, size ->
            captureRoomPage(key, sessionManager.getUserRooms(PaginationQuery(key, size = size)))
        }.flow.cachedIn(viewModelScope)
}

class UnreadMessageCountViewModel(sessionManager: UserSessionManager) : SimpleViewModel<UnreadMessageCountResponse>() {
    override val handler: LoadingHandler<UnreadMessageCountResponse> =
        SimpleLoadingHandler(viewModelScope) {
            if (sessionManager.currentIsAlreadySignUp) {
                sessionManager.getUnreadMessageCount()
            } else {
                CustomResult.success(UnreadMessageCountResponse(0))
            }
        }
}

@OptIn(ExperimentalPagingApi::class)
class CommunityRoomsViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    communityId: PrimaryKey,
) : RoomListViewModel() {
    private val modelCollection = RoomCollection.CommunityRooms(communityId)

    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
            onUpdated = previews::publish,
        ) { key, size ->
            captureRoomPage(
                key,
                sessionManager.getCommunityRooms(
                    communityId,
                    CustomApi.Communities.Id.Rooms.CommunityRoomQuery(key, size = size),
                ),
            )
        }.flow.cachedIn(viewModelScope)
}

@OptIn(ExperimentalPagingApi::class)
class CommunityRoomSearchViewModel(
    sessionManager: UserSessionManager,
    modelStorage: ModelStorage,
    communityId: PrimaryKey,
    word: String,
) : RoomListViewModel() {
    private val modelCollection = RoomCollection.CommunityRoomSearch(communityId, word)

    override val flow: Flow<PagingData<RoomInfo>> =
        buildPager(
            modelCollection,
            modelStorage.remoteKey.wrap(modelCollection.getName()),
            modelStorage.room,
            onUpdated = previews::publish,
        ) { key, size ->
            captureRoomPage(
                key,
                sessionManager.searchCommunityRooms(
                    communityId,
                    word,
                    JoinStatusSearch.UNSPECIFIED,
                    size,
                    key,
                ),
            )
        }.flow.cachedIn(viewModelScope)
}
