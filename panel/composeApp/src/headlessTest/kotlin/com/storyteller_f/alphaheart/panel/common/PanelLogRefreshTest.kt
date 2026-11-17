/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import com.storyteller_f.storage.PanelLogCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
internal class PanelLogRefreshTest {
    @Test
    fun eachStatusEventTargetsOnlyItsOwnNamedLogCollection() {
        val events =
            listOf(
                OnUserStatusUpdated(7, UserStatus.READ_ONLY) to ObjectType.USER,
                OnCommunityStatusUpdated(7, ObjectStatus.READ_ONLY) to ObjectType.COMMUNITY,
                OnRoomStatusUpdated(7, ObjectStatus.READ_ONLY) to ObjectType.ROOM,
                OnTopicStatusUpdated(7, ObjectStatus.READ_ONLY) to ObjectType.TOPIC,
                OnTitleStatusUpdated(7, ObjectStatus.READ_ONLY) to ObjectType.TITLE,
                OnFileStatusUpdated(7, ObjectStatus.READ_ONLY) to ObjectType.FILE,
            )
        events.forEach { (event, type) ->
            assertEquals(PanelLogCollection.PanelLogs(7, type), event.statusLogCollection())
        }
        assertNull(Unit.statusLogCollection())
    }

    @Test
    fun previouslyEmptyLogsRefreshAfterStatusChangesWhileTheirTabIsOffscreen() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val events = MutableSharedFlow<Any>()
            val fixture = LogPages(ObjectType.TOPIC, 7)
            val viewModel = PanelLogsViewModel(7, ObjectType.TOPIC, events, fixture::pages)
            val presenter =
                object : PagingDataPresenter<PanelLogInfo>(dispatcher) {
                    override suspend fun presentPagingDataEvent(event: PagingDataEvent<PanelLogInfo>) = Unit
                }
            try {
                val firstVisit = backgroundScope.launch { viewModel.flow.collectLatest(presenter::collectFrom) }
                runCurrent()
                assertEquals(emptyList(), presenter.snapshot().items)
                assertEquals(1, fixture.generations)
                firstVisit.cancel()
                runCurrent()
                fixture.logs = listOf(log(1, ObjectType.TOPIC, 7))
                events.emit(OnTopicStatusUpdated(7, ObjectStatus.READ_ONLY))
                runCurrent()
                assertEquals(2, fixture.generations)
                backgroundScope.launch { viewModel.flow.collectLatest(presenter::collectFrom) }
                runCurrent()
                assertEquals(listOf(1L), presenter.snapshot().items.map { it.id })
                fixture.logs = listOf(log(2, ObjectType.TOPIC, 7), log(1, ObjectType.TOPIC, 7))
                events.emit(OnTopicStatusUpdated(7, ObjectStatus.NORMAL))
                runCurrent()
                assertEquals(3, fixture.generations)
                assertEquals(listOf(2L, 1L), presenter.snapshot().items.map { it.id })
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun refreshingOneOwnerDoesNotReloadAnotherOwnerOrObjectType() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val events = MutableSharedFlow<Any>()
            val fixtures =
                listOf(
                    LogPages(ObjectType.TOPIC, 7),
                    LogPages(ObjectType.TOPIC, 8),
                    LogPages(ObjectType.ROOM, 7),
                )
            val viewModels =
                fixtures.map { fixture ->
                    PanelLogsViewModel(fixture.targetId, fixture.type, events, fixture::pages)
                }
            val presenters =
                fixtures.map {
                    object : PagingDataPresenter<PanelLogInfo>(dispatcher) {
                        override suspend fun presentPagingDataEvent(event: PagingDataEvent<PanelLogInfo>) = Unit
                    }
                }
            try {
                viewModels.zip(presenters).forEach { (viewModel, presenter) ->
                    backgroundScope.launch { viewModel.flow.collectLatest(presenter::collectFrom) }
                }
                runCurrent()
                fixtures.forEachIndexed { index, fixture ->
                    fixture.logs = listOf(log(index.toLong() + 1, fixture.type, fixture.targetId))
                }
                events.emit(OnTopicStatusUpdated(7, ObjectStatus.READ_ONLY))
                runCurrent()
                assertEquals(listOf(2, 1, 1), fixtures.map { it.generations })
                assertEquals(listOf(1L), presenters[0].snapshot().items.map { it.id })
                assertEquals(emptyList(), presenters[1].snapshot().items)
                assertEquals(emptyList(), presenters[2].snapshot().items)
                events.emit(OnRoomStatusUpdated(7, ObjectStatus.READ_ONLY))
                runCurrent()
                assertEquals(listOf(2, 1, 2), fixtures.map { it.generations })
                assertEquals(listOf(3L), presenters[2].snapshot().items.map { it.id })
                assertEquals(listOf(1L), presenters[0].snapshot().items.map { it.id })
            } finally {
                viewModels.forEach { it.viewModelScope.cancel() }
                Dispatchers.resetMain()
            }
        }
}

private class LogPages(val type: ObjectType, val targetId: Long) {
    var generations = 0
    var logs: List<PanelLogInfo> = emptyList()

    fun pages(): Flow<PagingData<PanelLogInfo>> {
        generations++
        return Pager(PagingConfig(pageSize = 20)) {
            object : PagingSource<String, PanelLogInfo>() {
                override suspend fun load(params: LoadParams<String>): LoadResult<String, PanelLogInfo> =
                    LoadResult.Page(logs, prevKey = null, nextKey = null)

                override fun getRefreshKey(state: PagingState<String, PanelLogInfo>): String? = null
            }
        }.flow
    }
}

private fun log(id: Long, type: ObjectType, targetId: Long) =
    PanelLogInfo(
        id = id,
        adminId = 1,
        targetId = targetId,
        objectType = type,
        action = "Update status",
        createdTime = LocalDateTime(2026, 11, 16, 12, 0),
    )
