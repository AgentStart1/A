/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.RequestError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.obj.ListResponse
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.shared.type.UserStatus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
internal class UserDetailCacheIsolationTest {
    @Test
    fun lateUserOverviewCannotReplaceAnotherOwnersCachedDetail() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val firstResponse = CompletableDeferred<CustomResult<UserOverview, RequestError>>()
            val secondResponse = CompletableDeferred<CustomResult<UserOverview, RequestError>>()
            val first = IdUserOverviewViewModel(1) { firstResponse.await() }
            val second = IdUserOverviewViewModel(2) { secondResponse.await() }
            try {
                backgroundScope.launch { first.handler.data.collect {} }
                backgroundScope.launch { second.handler.data.collect {} }
                runCurrent()
                assertNull(first.handler.data.value)
                assertNull(second.handler.data.value)
                secondResponse.complete(CustomResult.Success(overview(2)))
                runCurrent()
                assertEquals(overview(2), second.handler.data.value)
                assertNull(first.handler.data.value)
                firstResponse.complete(CustomResult.Success(overview(1)))
                runCurrent()
                assertEquals(overview(1), first.handler.data.value)
                assertEquals(overview(2), second.handler.data.value)
            } finally {
                first.viewModelScope.cancel()
                second.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedRefreshPreservesOnlyTheSelectedOwnersOverview() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val response = MutableStateFlow<CustomResult<UserOverview, RequestError>>(CustomResult.Success(overview(3)))
            val owner = IdUserOverviewViewModel(3) { response.value }
            try {
                backgroundScope.launch { owner.handler.data.collect {} }
                runCurrent()
                assertEquals(overview(3), owner.handler.data.value)
                response.value = CustomResult.Failure(RequestError.TransportFailure(Exception("refresh failed")))
                owner.handler.refresh()
                runCurrent()
                assertIs<LoadingState.Error>(owner.handler.state.value)
                assertEquals(overview(3), owner.handler.data.value)
            } finally {
                owner.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun matchingStatusEventUpdatesOnlyItsOwnerAndWinsOverAnOlderRefreshResponse() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val events = MutableSharedFlow<Any>()
            val lateResponse = CompletableDeferred<CustomResult<UserOverview, RequestError>>()
            var firstLoads = 0
            val first =
                IdUserOverviewViewModel(1, events) {
                    if (firstLoads++ == 0) CustomResult.Success(overview(1)) else lateResponse.await()
                }
            val second = IdUserOverviewViewModel(2, events) { CustomResult.Success(overview(2)) }
            try {
                backgroundScope.launch { first.handler.data.collect {} }
                backgroundScope.launch { second.handler.data.collect {} }
                runCurrent()
                first.handler.refresh()
                runCurrent()
                events.emit(OnUserStatusUpdated(1, UserStatus.READ_ONLY))
                runCurrent()
                assertEquals(
                    UserStatus.READ_ONLY,
                    first.handler.data.value
                        ?.userInfo?.status,
                )
                assertEquals(
                    UserStatus.NORMAL,
                    second.handler.data.value
                        ?.userInfo?.status,
                )
                lateResponse.complete(CustomResult.Success(overview(1)))
                runCurrent()
                assertEquals(
                    UserStatus.READ_ONLY,
                    first.handler.data.value
                        ?.userInfo?.status,
                )
                assertEquals(
                    UserStatus.NORMAL,
                    second.handler.data.value
                        ?.userInfo?.status,
                )
                first.handler.refresh()
                runCurrent()
                assertEquals(
                    UserStatus.NORMAL,
                    first.handler.data.value
                        ?.userInfo?.status,
                )
            } finally {
                first.viewModelScope.cancel()
                second.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun statusEventBeforeInitialDataIsRetainedWhenTheInitialResponseArrives() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val events = MutableSharedFlow<Any>()
            val response = CompletableDeferred<CustomResult<UserOverview, RequestError>>()
            val owner = IdUserOverviewViewModel(1, events) { response.await() }
            try {
                backgroundScope.launch { owner.handler.data.collect {} }
                runCurrent()
                events.emit(OnUserStatusUpdated(1, UserStatus.READ_ONLY))
                response.complete(CustomResult.Success(overview(1)))
                runCurrent()
                assertEquals(
                    UserStatus.READ_ONLY,
                    owner.handler.data.value
                        ?.userInfo?.status,
                )
            } finally {
                owner.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun parallelJoinedRoomPagesRemainOwnerScopedWhenFirstResponseArrivesLate() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val firstResponse = CompletableDeferred<CustomResult<ListResponse<RoomInfo>, RequestError>>()
            val secondResponse = CompletableDeferred<CustomResult<ListResponse<RoomInfo>, RequestError>>()
            val requests = mutableListOf<Pair<String?, Int>>()
            val first =
                UserJoinedRoomsViewModel { key, size ->
                    requests += key to size
                    firstResponse.await()
                }
            val second =
                UserJoinedRoomsViewModel { key, size ->
                    requests += key to size
                    secondResponse.await()
                }
            val firstPresenter =
                object : PagingDataPresenter<RoomInfo>(dispatcher) {
                    override suspend fun presentPagingDataEvent(event: PagingDataEvent<RoomInfo>) = Unit
                }
            val secondPresenter =
                object : PagingDataPresenter<RoomInfo>(dispatcher) {
                    override suspend fun presentPagingDataEvent(event: PagingDataEvent<RoomInfo>) = Unit
                }
            try {
                backgroundScope.launch { first.flow.collectLatest(firstPresenter::collectFrom) }
                backgroundScope.launch { second.flow.collectLatest(secondPresenter::collectFrom) }
                runCurrent()
                assertEquals(2, requests.size)
                secondResponse.complete(CustomResult.Success(rooms(22)))
                runCurrent()
                assertEquals(listOf(22L), secondPresenter.snapshot().items.map { it.id })
                assertEquals(emptyList(), firstPresenter.snapshot().items)
                firstResponse.complete(CustomResult.Success(rooms(11)))
                runCurrent()
                assertEquals(listOf(11L), firstPresenter.snapshot().items.map { it.id })
                assertEquals(listOf(22L), secondPresenter.snapshot().items.map { it.id })
            } finally {
                first.viewModelScope.cancel()
                second.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}

private fun overview(id: Long) = UserOverview(0, 0, 0, 0, 0, 0, userInfo = UserInfo.EMPTY.copy(id = id))

private fun rooms(id: Long): ListResponse<RoomInfo> =
    object : ListResponse<RoomInfo> {
        override val data = listOf(RoomInfo.EMPTY.copy(id = id)).toImmutableList()
        override val pagination = Pagination<String>(null, null, 1)
    }
