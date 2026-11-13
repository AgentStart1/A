/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.api.RoomActivityInfo
import com.storyteller_f.alphaheart.api.RoomActivityListResponse
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.TopicActivityListResponse
import com.storyteller_f.alphaheart.app.pages.room.latestVisibleRoomTopicId
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.shared.type.ObjectType
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityPreviewHostTest {
    @Test
    fun readMarkerUsesNewestVisibleMessage() {
        assertEquals(4L, latestVisibleRoomTopicId(listOf("anchor", 3L, 4L, 2L)))
        assertEquals(2L, latestVisibleRoomTopicId(listOf(1L, 2L, "loading")))
        assertEquals(null, latestVisibleRoomTopicId(listOf("anchor", "loading")))
    }

    @Test
    fun listPagesRetainActivityAcrossHundredRoomBoundary() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host = RoomListPreviewHost(dispatcher, dispatcher)
            host.acceptPage(null, roomPage(1L..100L))
            host.acceptPage("next-page", roomPage(101L..120L))
            host.publish()
            assertEquals((1L..120L).toSet(), host.rows.value.keys)
            for (id in 95L..105L) assertEquals(id, host.rows.value.getValue(id).unreadCount)
            host.acceptPage(null, roomPage(101L..105L))
            assertEquals((1L..120L).toSet(), host.rows.value.keys)
            host.publish()
            assertEquals((101L..105L).toSet(), host.rows.value.keys)
        }

    @Test
    fun cancelledPageCannotPublishPrivatePreview() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host = RoomListPreviewHost(dispatcher, dispatcher)
            val job =
                launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    host.acceptPage(null, roomPage(1L..1L))
                }
            job.cancel()
            runCurrent()
            host.publish()
            assertTrue(host.rows.value.isEmpty())
        }

    @Test
    fun contextStatesFailAndRetryIndependently() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            var failUnread = true
            val host =
                ActivityPreviewHost(this, dispatcher, dispatcher, {
                    if (failUnread) error("Synthetic failure")
                    CustomResult.success(
                        RoomActivityListResponse(persistentListOf(activity(1)), Pagination(null, null, 9)),
                    )
                }, {
                    CustomResult.success(
                        TopicActivityListResponse(
                            persistentListOf(
                                TopicActivity(
                                    TopicInfo.EMPTY.copy(content = TopicContent.Plain("Pin")),
                                    null,
                                    persistentListOf(),
                                    null,
                                ),
                            ),
                        ),
                    )
                })
            host.refreshUnread()
            host.refreshPinned()
            runCurrent()
            assertIs<PreviewState.Failed>(host.unread.value)
            assertEquals(1, assertIs<PreviewState.Ready<PinnedTopicPreview>>(host.pinned.value).items.size)
            failUnread = false
            host.refreshUnread()
            runCurrent()
            assertEquals(9L, assertIs<PreviewState.Ready<RoomActivityPreview>>(host.unread.value).total)
            host.pause()
            runCurrent()
            assertIs<PreviewState.Idle>(host.unread.value)
            assertIs<PreviewState.Idle>(host.pinned.value)
            host.close()
        }

    @Test
    fun pauseCancelsLoadingWithoutPublishingFailure() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val pending = CompletableDeferred<RoomActivityListResponse>()
            val host =
                ActivityPreviewHost(this, dispatcher, dispatcher, {
                    CustomResult.success(pending.await())
                }, {
                    CustomResult.success(TopicActivityListResponse(persistentListOf()))
                })
            host.refreshUnread()
            runCurrent()
            assertIs<PreviewState.Loading>(host.unread.value)
            host.pause()
            runCurrent()
            pending.complete(RoomActivityListResponse(persistentListOf(activity(1))))
            runCurrent()
            assertIs<PreviewState.Idle>(host.unread.value)
            host.close()
        }

    @Test
    fun roomEventsDoNotReloadPinnedTopics() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            var unreadLoads = 0
            var pinLoads = 0
            val host =
                ActivityPreviewHost(this, dispatcher, dispatcher, {
                    unreadLoads++
                    CustomResult.success(RoomActivityListResponse(persistentListOf()))
                }, {
                    pinLoads++
                    CustomResult.success(TopicActivityListResponse(persistentListOf()))
                })
            host.onEvent(OnRoomRead(1), true)
            runCurrent()
            assertEquals(1, unreadLoads)
            assertEquals(0, pinLoads)
            host.onEvent(
                OnTopicPinChanged(1, 2, ObjectType.COMMUNITY, true),
                true,
            )
            runCurrent()
            assertEquals(1, unreadLoads)
            assertEquals(1, pinLoads)
            host.close()
        }

    @Test
    fun unavailableAndEmptyMessagesAreDistinct() {
        val encrypted =
            activity(
                1,
            ).copy(
                latestMessage =
                TopicActivity(
                    TopicInfo.EMPTY.copy(content = TopicContent.Encrypted("secret", emptyMap())),
                    null,
                    persistentListOf(),
                    null,
                ),
            )
        assertIs<MessageSummary.Unavailable>(encrypted.toPreview().message?.summary)
        val failed =
            encrypted.copy(
                latestMessage =
                TopicActivity(
                    TopicInfo.EMPTY.copy(content = TopicContent.DecryptFailed("secret")),
                    null,
                    persistentListOf(),
                    null,
                ),
            )
        assertIs<MessageSummary.Unavailable>(failed.toPreview().message?.summary)
        val missing = activity(1).copy(latestMessageId = 12)
        assertIs<MessageSummary.Unavailable>(missing.toPreview().message?.summary)
        assertNull(activity(1).toPreview().message)
        assertIs<MessageSummary.Unavailable>(activity(1).copy(unreadMessageCount = 2).toPreview().message?.summary)
        val plain =
            activity(
                1,
            ).copy(
                latestMessage =
                TopicActivity(
                    TopicInfo.EMPTY.copy(content = TopicContent.Plain("hello\nworld")),
                    null,
                    persistentListOf(),
                    null,
                ),
            )
        assertEquals("hello world", assertIs<MessageSummary.Text>(plain.toPreview().message?.summary).value)
    }
}

private fun activity(id: Long) = RoomActivityInfo(RoomInfo.EMPTY.copy(id = id), null, null, 0)

private fun roomPage(ids: LongRange) =
    RoomInfoListResponse(
        ids.map { RoomInfo.EMPTY.copy(id = it) }.toImmutableList(),
        activities = ids.map { activity(it).copy(unreadMessageCount = it) }.toImmutableList(),
    )
