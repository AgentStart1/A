/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.ContainerDatabase
import com.storyteller_f.alphaheart.backend.core.FavoriteDatabase
import com.storyteller_f.alphaheart.backend.core.FileDatabase
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.ReactionDatabase
import com.storyteller_f.alphaheart.backend.core.RoomDatabase
import com.storyteller_f.alphaheart.backend.core.SubscriptionDatabase
import com.storyteller_f.alphaheart.backend.core.TopicDatabase
import com.storyteller_f.alphaheart.backend.core.UserDatabase
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.getLatestRawTopics
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.cloud.core.service.error.UserSubscriptionError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import java.lang.reflect.Proxy
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class SubscriptionProjectionTest {
    @Test
    fun mixedSubscriptionsKeepOrderMissingRowsAndReplyOrder() =
        runBlocking {
            val fixture = SubscriptionProjectionFixture()
            val rows =
                listOf(
                    subscription(1, 22),
                    subscription(2, 7, ObjectType.USER),
                    subscription(3, 11),
                    subscription(4, 22),
                    subscription(5, 99),
                )

            val result = fixture.project(rows).getOrThrow()

            assertEquals(rows.map { it.id }, result.map { it.id })
            assertEquals(listOf(22L, null, 11L, 22L, null), result.map { it.topicInfo?.id })
            assertEquals(listOf(221L, 222L), result.first().topicInfo?.subTopics?.map { it.id })
            assertEquals(listOf(22L, 11L, 99L), fixture.permissions)
            assertEquals(listOf(listOf(22L, 11L)), fixture.parentBatches)
            assertEquals(2, fixture.contentBatches.size)
            assertEquals(listOf(111L, 112L, 221L, 222L), fixture.contentBatches.last())
        }

    @Test
    fun unreadableTopicFailsBeforeReadingAnyContent() =
        runBlocking {
            val fixture = SubscriptionProjectionFixture(readable = false)
            val failure =
                assertIs<CustomResult.Failure<UserSubscriptionError>>(
                    fixture.project(listOf(subscription(1, 22))),
                )
            assertIs<UserSubscriptionError.Forbidden>(failure.error)
            assertEquals(emptyList(), fixture.contentBatches)
        }

    @Test
    fun permissionExceptionFailsRatherThanOmittingTheRow() =
        runBlocking {
            val cause = IllegalStateException("permission failed")
            val fixture = SubscriptionProjectionFixture(permissionFailure = cause)
            val failure =
                assertIs<CustomResult.Failure<UserSubscriptionError>>(
                    fixture.project(listOf(subscription(1, 22))),
                )
            assertSame(cause, failure.error.cause)
            assertEquals(emptyList(), fixture.contentBatches)
        }

    @Test
    fun databasePermissionFailureRetainsItsCause() =
        runBlocking {
            val cause = IllegalStateException("database permission failed")
            val fixture = SubscriptionProjectionFixture(permissionFailure = cause, returnFailure = true)
            val failure =
                assertIs<CustomResult.Failure<UserSubscriptionError>>(
                    fixture.project(listOf(subscription(1, 22))),
                )
            assertSame(cause, failure.error.cause)
            assertEquals(emptyList(), fixture.contentBatches)
        }

    @Test
    fun permissionCancellationPropagates() {
        val cancellation = CancellationException("cancel subscription projection")
        val fixture = SubscriptionProjectionFixture(permissionFailure = cancellation)
        val thrown =
            assertFailsWith<CancellationException> {
                runBlocking { fixture.project(listOf(subscription(1, 22))) }
            }
        assertSame(cancellation, thrown)
        assertEquals(emptyList(), fixture.contentBatches)
    }

    @Test
    fun latestRepliesEnrichOnceAndDeduplicateParents() =
        runBlocking {
            val fixture = SubscriptionProjectionFixture()
            val result = fixture.latest(listOf(22, 11, 22)).getOrThrow()
            assertEquals(listOf(22L, 11L), fixture.latestParents)
            assertEquals(listOf(221L, 222L, 111L, 112L), result.map { it.topic.id })
            assertEquals(listOf(result.map { it.topic.id }), fixture.contentBatches)
        }

    @Test
    fun emptyParentsAndNonTopicRowsDoNotLoadContent() =
        runBlocking {
            val fixture = SubscriptionProjectionFixture()
            assertEquals(emptyList(), fixture.latest(emptyList()).getOrThrow())
            val result = fixture.project(listOf(subscription(1, 7, ObjectType.USER))).getOrThrow()
            assertEquals(listOf(1L), result.map { it.id })
            assertEquals(emptyList(), fixture.permissions)
            assertEquals(emptyList(), fixture.latestParents)
            assertEquals(emptyList(), fixture.contentBatches)
        }

    @Test
    fun latestReplyCancellationStopsBeforeEnrichment() {
        val cancellation = CancellationException("cancel latest replies")
        val fixture = SubscriptionProjectionFixture(latestFailure = cancellation)
        val thrown =
            assertFailsWith<CancellationException> {
                runBlocking { fixture.latest(listOf(22, 11)) }
            }
        assertSame(cancellation, thrown)
        assertEquals(listOf(22L, 11L), fixture.latestParents)
        assertEquals(emptyList(), fixture.contentBatches)
    }
}

private class SubscriptionProjectionFixture(
    private val readable: Boolean = true,
    private val permissionFailure: Throwable? = null,
    private val latestFailure: Throwable? = null,
    private val returnFailure: Boolean = false,
) {
    val permissions = mutableListOf<Long>()
    val parentBatches = mutableListOf<List<Long>>()
    val contentBatches = mutableListOf<List<Long>>()
    val latestParents = mutableListOf<Long>()
    private val topic = createTopicDatabase()
    private val room = createRoomDatabase()
    private val container = createContainerDatabase()
    private val database = createDatabase()
    private val backend = createBackend()

    suspend fun project(rows: List<UserSubscription>) = backend.buildSubscriptionItems(1, rows)

    suspend fun latest(parents: List<Long>) = database.getLatestRawTopics(1, parents)

    private fun createTopicDatabase() =
        projectionProxy(TopicDatabase::class.java) { name, args ->
            when (name) {
                "getTopic" -> {
                    val fetch = requireNotNull(args[0]) as ObjectFetch.IdFetch
                    CustomResult.success(projectionTopic(fetch.id, 10))
                }

                "getTopicRootTuple" -> topicPermission(requireNotNull(args[0]) as Long)

                "getTopicListByIds" -> {
                    val ids = (requireNotNull(args[0]) as List<*>).map { requireNotNull(it) as Long }
                    parentBatches.add(ids)
                    CustomResult.success(ids.reversed().map { projectionTopic(it, 10) })
                }

                "getLatestTopic" -> latestTopics(requireNotNull(args[0]) as Long)

                "getTopicContentFromByteArray" -> {
                    val topics = (requireNotNull(args[0]) as List<*>).map { requireNotNull(it) as Topic }
                    contentBatches.add(topics.map { it.id })
                    CustomResult.success(topics.associate { it.id to TopicContent.Plain("synthetic ${it.id}") })
                }

                "isUserCommented", "getTopicCommentCount" -> CustomResult.success(emptyList<Any>())

                else -> error("Unexpected topic call: $name")
            }
        }

    private fun topicPermission(id: Long): CustomResult<ObjectTuple?, DatabaseError> {
        permissionFailure?.let {
            if (returnFailure) return CustomResult.failure(DatabaseError.Unexpected(it))
            throw it
        }
        permissions.add(id)
        return CustomResult.success(if (id == 99L) null else ObjectTuple(10, ObjectType.ROOM))
    }

    private fun latestTopics(parent: Long): CustomResult<List<Topic>, DatabaseError> {
        latestParents.add(parent)
        if (parent == 11L) latestFailure?.let { throw it }
        return CustomResult.success(
            listOf(projectionTopic(parent * 10 + 1, parent, true), projectionTopic(parent * 10 + 2, parent)),
        )
    }

    private fun createRoomDatabase() =
        projectionProxy(RoomDatabase::class.java) { name, _ ->
            when (name) {
                "getRoomCommunityId" -> CustomResult.success(if (readable) 7L else null)
                "getRawRooms" -> CustomResult.success(emptyList<Any>())
                else -> error("Unexpected room call: $name")
            }
        }

    private fun createContainerDatabase() =
        projectionProxy(ContainerDatabase::class.java) { name, _ ->
            when (name) {
                "isMemberJoined" -> CustomResult.success(readable)
                "getTopicReadList" -> CustomResult.success(emptyList<Any>())
                else -> error("Unexpected container call: $name")
            }
        }

    private fun createDatabase() =
        projectionProxy(CombinedDatabase::class.java) { name, _ ->
            when (name) {
                "getTopic" -> topic

                "getRoom" -> room

                "getContainer" -> container

                "getUser" -> emptyProjectionProxy(UserDatabase::class.java, setOf("getRawUsers"))

                "getFile" ->
                    emptyProjectionProxy(FileDatabase::class.java, setOf("getFileRecordByIds", "getFileRecordByNames"))

                "getFavorite" -> emptyProjectionProxy(FavoriteDatabase::class.java, setOf("getHasFavorite"))

                "getSubscription" -> emptyProjectionProxy(SubscriptionDatabase::class.java, setOf("getHasSubscription"))

                "getReaction" -> createReactionDatabase()

                else -> error("Unexpected database call: $name")
            }
        }

    private fun createReactionDatabase() =
        projectionProxy(ReactionDatabase::class.java) { method, _ ->
            when (method) {
                "getReactionInfoPaginationResult" -> CustomResult.success(PaginationResult(emptyList<Any>(), 0))
                "getReactionCount" -> CustomResult.success(emptyList<Any>())
                else -> error("Unexpected reaction call: $method")
            }
        }

    private fun createBackend() =
        projectionProxy(Backend::class.java) { name, _ ->
            when (name) {
                "getDatabase" -> database

                "getObjectStorageService" ->
                    emptyProjectionProxy(ObjectStorageService::class.java, setOf("getWithPresignContext"))

                else -> error("Unexpected backend call: $name")
            }
        }
}

private fun <T> projectionProxy(type: Class<T>, invoke: (String, Array<out Any?>) -> Any?): T {
    val instance =
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
            invoke(method.name, args.orEmpty())
        }
    return type.cast(instance)
}

private fun <T> emptyProjectionProxy(type: Class<T>, methods: Set<String>) =
    projectionProxy(type) { name, _ ->
        check(name in methods) { "Unexpected ${type.simpleName} call: $name" }
        CustomResult.success(emptyList<Any>())
    }

private val projectionTime = LocalDateTime(2026, 11, 17, 0, 0)

private fun subscription(id: Long, objectId: Long, type: ObjectType = ObjectType.TOPIC) =
    UserSubscription(id, 1, objectId, type, projectionTime)

private fun projectionTopic(id: Long, parentId: Long, pinned: Boolean = false) =
    Topic(id, projectionTime, 1, parentId, ObjectType.TOPIC, 10, ObjectType.ROOM, byteArrayOf(), false, 1, pinned)
