/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.NestedMemberInfo
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicReplyPreview
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.ChildAccountStorage
import com.storyteller_f.storage.CollectionListStorageWithDefault
import com.storyteller_f.storage.CommunityCollection
import com.storyteller_f.storage.CommunityInfoStorage
import com.storyteller_f.storage.DownloadInfoStorage
import com.storyteller_f.storage.FileInfoStorage
import com.storyteller_f.storage.FileRefInfoStorage
import com.storyteller_f.storage.MemberInfoStorage
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.OverviewStorage
import com.storyteller_f.storage.PanelLogInfoStorage
import com.storyteller_f.storage.ReactionInfoStorage
import com.storyteller_f.storage.RemoteKeyStorage
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.RoomInfoStorage
import com.storyteller_f.storage.TaskRecordInfoStorage
import com.storyteller_f.storage.TitleInfoStorage
import com.storyteller_f.storage.TopicCollection
import com.storyteller_f.storage.TopicInfoStorage
import com.storyteller_f.storage.UploadInfoStorage
import com.storyteller_f.storage.UploadRecordInfoStorage
import com.storyteller_f.storage.UserFavoriteStorage
import com.storyteller_f.storage.UserInfoStorage
import com.storyteller_f.storage.UserLogInfoStorage
import com.storyteller_f.storage.UserOverviewStorage
import com.storyteller_f.storage.UserReactionRecordStorage
import com.storyteller_f.storage.UserSubscriptionStorage
import com.storyteller_f.storage.mergeActivity
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EventTest {
    @Test
    fun communityCreationUpdatesJoinedCollection() =
        runTest {
            val storage = FakeCommunityStorage()
            val original = CommunityInfo.EMPTY
            val info =
                original.copy(
                    id = 123,
                    name = "Created community",
                    owner = 456,
                    memberCount = 1,
                    member = NestedMemberInfo(MemberStatus.JOINED, original.createdTime),
                )
            val bus = MutableSharedFlow<Any>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(FakeModelStorage(community = storage), bus)
                }
            try {
                bus.emit(OnCommunityCreated(info))
                assertEquals(listOf(info), storage.savedDefault)
                val joinedCollection: CommunityCollection =
                    CommunityCollection.SearchCommunity(
                        JoinStatusSearch.JOINED,
                        "",
                        null,
                        PosterSearch.UNSPECIFIED,
                    )
                assertEquals(listOf(joinedCollection to info), storage.savedFirst)
            } finally {
                job.cancel()
            }
        }

    @Test
    fun activityRefreshPreservesLoadedReplies() =
        runTest {
            val storage = FakeTopicStorage()
            val reply = TopicReplyPreview(TopicDetail.EMPTY.topicInfo.copy(id = 2), UserInfo.EMPTY)
            val cached = TopicDetail.EMPTY.copy(subTopics = persistentListOf(reply))
            storage.documents[TopicCollection.Topics to cached.id.toString()] = cached
            val summary = TopicActivity(cached.topicInfo.copy(isPin = true), null, persistentListOf(), null)
            val merged = storage.mergeActivity(summary)
            assertTrue(merged.isPin)
            assertEquals(cached.subTopics, merged.subTopics)
        }

    @Test
    fun compactCreationPreservesCachedRelations() =
        runTest {
            val storage = FakeTopicStorage()
            val cached = TopicDetail.EMPTY.copy(authorInfo = UserInfo.EMPTY.copy(nickname = "Author"))
            storage.documents[TopicCollection.Topics to cached.id.toString()] = cached
            val bus = MutableSharedFlow<Any>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(FakeModelStorage(topic = storage), bus)
                }
            try {
                bus.emit(OnTopicCreated(TopicCreated(cached.topicInfo, null)))
                assertEquals(cached, storage.savedDefault.single())
                assertEquals(cached, storage.savedFirst.single().second)
            } finally {
                job.cancel()
            }
        }

    @Test
    fun testProcessTopicCreated() =
        runTest {
            val fakeTopicStorage = FakeTopicStorage()
            val fakeStorage = FakeModelStorage(topic = fakeTopicStorage)
            val bus = MutableSharedFlow<Any>()
            val parentId: PrimaryKey = 100L
            val topicInfo =
                TopicDetail.EMPTY.copy(
                    topicInfo = TopicDetail.EMPTY.topicInfo.copy(id = 1L, parentId = parentId),
                )

            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(fakeStorage, bus)
                }
            bus.emit(OnTopicCreated(TopicCreated(topicInfo.topicInfo, topicInfo.authorInfo)))

            val savedFirst = fakeTopicStorage.savedFirst
            assertEquals(1, savedFirst.size)
            assertEquals(TopicCollection.ChildTopicList(parentId), savedFirst[0].first)
            assertEquals(topicInfo, savedFirst[0].second)

            assertEquals(1, fakeTopicStorage.savedDefault.size)
            assertEquals(topicInfo, fakeTopicStorage.savedDefault[0])

            job.cancel()
        }

    @Test
    fun pinChangePreservesEachCachedProjection() =
        runTest {
            val storage = FakeTopicStorage()
            val base =
                TopicDetail.EMPTY.topicInfo.copy(
                    id = 2,
                    parentId = 100,
                    aid = "topic-two",
                    content = TopicContent.Plain("# Heading\n\nFull body"),
                    reactionCount = 3,
                )
            val cached =
                TopicDetail.EMPTY.copy(
                    topicInfo = base,
                    authorInfo = UserInfo.EMPTY,
                    subTopics = persistentListOf(TopicReplyPreview(base.copy(id = 3), UserInfo.EMPTY)),
                )
            val recommended = cached.copy(topicInfo = base.copy(reactionCount = 4))
            val child =
                cached.copy(topicInfo = base.copy(content = TopicContent.Encrypted("ciphertext", emptyMap())))
            storage.documents[TopicCollection.Topics to "2"] = cached
            storage.documents[TopicCollection.Recommend to "2"] = recommended
            storage.documents[TopicCollection.ChildTopicList(100) to "2"] = child
            val bus = MutableSharedFlow<Any>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(FakeModelStorage(topic = storage), bus)
                }
            try {
                bus.emit(OnTopicPinChanged(2, 100, ObjectType.ROOM, true))
                assertEquals(listOf(cached.copy(topicInfo = base.copy(isPin = true))), storage.savedDefault)
                assertEquals(
                    listOf(
                        TopicCollection.Recommend to
                            recommended.copy(topicInfo = recommended.topicInfo.copy(isPin = true)),
                        TopicCollection.ChildTopicList(100) to
                            child.copy(topicInfo = child.topicInfo.copy(isPin = true)),
                    ),
                    storage.updatedDocuments,
                )
            } finally {
                job.cancel()
            }
        }

    @Test
    fun pinChangeDoesNotCreateMissingCacheEntries() =
        runTest {
            val storage = FakeTopicStorage()
            val bus = MutableSharedFlow<Any>()
            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(FakeModelStorage(topic = storage), bus)
                }
            try {
                bus.emit(OnTopicPinChanged(2, 100, ObjectType.ROOM, false))
                assertTrue(storage.savedDefault.isEmpty())
                assertTrue(storage.updatedDocuments.isEmpty())
                assertTrue(storage.savedFirst.isEmpty())
            } finally {
                job.cancel()
            }
        }

    @Test
    fun testProcessRoomCreated() =
        runTest {
            val fakeRoomStorage = FakeRoomStorage()
            val fakeStorage = FakeModelStorage(room = fakeRoomStorage)
            val bus = MutableSharedFlow<Any>()
            val communityId: PrimaryKey = 200L
            val roomInfo = RoomInfo.EMPTY.copy(id = 10L, communityId = communityId)

            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(fakeStorage, bus)
                }
            bus.emit(OnRoomCreated(roomInfo))

            assertEquals(1, fakeRoomStorage.savedDefault.size)
            assertEquals(roomInfo, fakeRoomStorage.savedDefault[0])

            val savedFirst = fakeRoomStorage.savedFirst
            assertEquals(1, savedFirst.size)
            assertEquals(RoomCollection.CommunityRooms(communityId), savedFirst[0].first)
            assertEquals(roomInfo, savedFirst[0].second)

            job.cancel()
        }

    @Test
    fun testProcessRoomUpdated() =
        runTest {
            val fakeRoomStorage = FakeRoomStorage()
            val fakeStorage = FakeModelStorage(room = fakeRoomStorage)
            val bus = MutableSharedFlow<Any>()
            val communityId: PrimaryKey = 200L
            val roomInfo = RoomInfo.EMPTY.copy(id = 10L, communityId = communityId)

            // Pre-populate CommunityRooms to allow update
            fakeRoomStorage.documents[RoomCollection.CommunityRooms(communityId) to roomInfo.id.toString()] = roomInfo

            val job =
                launch(UnconfinedTestDispatcher(testScheduler)) {
                    processEvent(fakeStorage, bus)
                }
            bus.emit(OnRoomUpdated(roomInfo))

            assertEquals(1, fakeRoomStorage.savedDefault.size)
            assertEquals(roomInfo, fakeRoomStorage.savedDefault[0])

            val updatedDocs = fakeRoomStorage.updatedDocuments
            val communityRoomUpdate = updatedDocs.find { it.first == RoomCollection.CommunityRooms(communityId) }
            assertTrue(communityRoomUpdate != null, "Should update CommunityRooms collection")
            assertEquals(roomInfo, communityRoomUpdate.second)

            job.cancel()
        }
}

// Fakes
open class FakeCollectionListStorage<C, I : Any> : CollectionListStorageWithDefault<C, I> {
    val savedDefault = mutableListOf<I>()
    val savedFirst = mutableListOf<Pair<C, I>>()
    val updatedDocuments = mutableListOf<Pair<C, I>>()
    val documents = mutableMapOf<Pair<C, String>, I>()

    override suspend fun saveToDefault(item: I) {
        savedDefault.add(item)
    }

    override suspend fun saveLast(collection: C, item: I) {
        // no-op
    }
    override suspend fun saveFirst(collection: C, item: I) {
        savedFirst.add(collection to item)
    }

    override fun observeData(collection: C): PagingSource<Int, I> = FakePagingSource()
    override suspend fun clean(collection: C) {
        // no-op
    }

    override suspend fun getDocument(collection: C, key: String): I? = documents[collection to key]

    override suspend fun updateDocument(collection: C, item: I) {
        updatedDocuments.add(collection to item)
    }

    override suspend fun delete(collection: C, key: String) {
        // no-op
    }
    override fun observeDatum(key: String): Flow<I?> = emptyFlow()
}

class FakeTopicStorage :
    FakeCollectionListStorage<TopicCollection, TopicDetail>(),
    TopicInfoStorage
class FakeRoomStorage :
    FakeCollectionListStorage<RoomCollection, RoomInfo>(),
    RoomInfoStorage
class FakeUserStorage :
    FakeCollectionListStorage<com.storyteller_f.storage.UserCollection, UserInfo>(),
    UserInfoStorage
class FakeCommunityStorage :
    FakeCollectionListStorage<com.storyteller_f.storage.CommunityCollection, CommunityInfo>(),
    CommunityInfoStorage

class FakePagingSource<I : Any> : PagingSource<Int, I>() {
    override fun getRefreshKey(state: PagingState<Int, I>): Int? = null
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, I> = LoadResult.Page(emptyList(), null, null)
}

private fun unavailableStorage(name: String): Nothing =
    throw UnsupportedOperationException("$name storage is not configured for this focused test")

class FakeModelStorage(
    override val topic: TopicInfoStorage = FakeTopicStorage(),
    override val room: RoomInfoStorage = FakeRoomStorage(),
    override val user: UserInfoStorage = FakeUserStorage(),
    override val community: CommunityInfoStorage = FakeCommunityStorage(),
) : ModelStorage {
    override val title: TitleInfoStorage get() = unavailableStorage("title")
    override val member: MemberInfoStorage get() = unavailableStorage("member")
    override val remoteKey: RemoteKeyStorage get() = unavailableStorage("remote key")
    override val reaction: ReactionInfoStorage get() = unavailableStorage("reaction")
    override val childAccount: ChildAccountStorage get() = unavailableStorage("child account")
    override val fileInfo: FileInfoStorage get() = unavailableStorage("file info")
    override val download: DownloadInfoStorage get() = unavailableStorage("download")
    override val upload: UploadInfoStorage get() = unavailableStorage("upload")
    override val overview: OverviewStorage get() = unavailableStorage("overview")
    override val userOverview: UserOverviewStorage get() = unavailableStorage("user overview")
    override val favorite: UserFavoriteStorage get() = unavailableStorage("favorite")
    override val subscription: UserSubscriptionStorage get() = unavailableStorage("subscription")
    override val userReactionRecord: UserReactionRecordStorage get() = unavailableStorage("reaction record")
    override val userLog: UserLogInfoStorage get() = unavailableStorage("user log")
    override val uploadRecord: UploadRecordInfoStorage get() = unavailableStorage("upload record")
    override val fileRef: FileRefInfoStorage get() = unavailableStorage("file reference")
    override val panelLog: PanelLogInfoStorage get() = unavailableStorage("panel log")
    override val taskRecord: TaskRecordInfoStorage get() = unavailableStorage("task record")
}
