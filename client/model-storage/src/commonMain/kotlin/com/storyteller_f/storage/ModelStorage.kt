/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.storage

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FileRefInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.model.PanelOverview
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UploadRecordInfo
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogItem
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

sealed interface UserCollection {
    data object Users : UserCollection
    data class SearchUser(val word: String) : UserCollection
    data class Members(val word: String, val objectId: PrimaryKey) : UserCollection
    data object AllUsers : UserCollection
}

interface CollectionListStorage<C, I : Any> {
    suspend fun saveLast(collection: C, item: I)
    suspend fun saveFirst(collection: C, item: I)
    fun observeData(collection: C): PagingSource<Int, I>
    suspend fun clean(collection: C)

    /** Replaces a complete refresh window; database implementations should commit it atomically. */
    suspend fun replace(collection: C, items: List<I>) {
        clean(collection)
        items.forEach { saveLast(collection, it) }
    }
    suspend fun getDocument(collection: C, id: PrimaryKey): I? = getDocument(collection, id.toString())
    suspend fun getDocument(collection: C, key: String): I?
    suspend fun updateDocument(collection: C, item: I)
    suspend fun delete(collection: C, key: String)
}

interface CollectionListStorageWithDefault<C, I : Any> : CollectionListStorage<C, I> {
    suspend fun saveToDefault(item: I)
    fun observeDatum(id: PrimaryKey): Flow<I?> = observeDatum(id.toString())
    fun observeDatum(key: String): Flow<I?>
}

interface GlobalListStorage<I : Any> {
    suspend fun save(item: I)
    fun observeData(): PagingSource<Int, I>
    suspend fun clean()
    fun observeDatum(id: PrimaryKey): Flow<I?> = observeDatum(id.toString())
    fun observeDatum(key: String): Flow<I?>
}

interface SingletonItemStorage<I : Any> {
    suspend fun save(item: I)
    fun observeDatum(): Flow<I?>
}

sealed interface TopicCollection {
    data object Topics : TopicCollection
    data object Recommend : TopicCollection
    data object AllTopics : TopicCollection
    data class SearchTopic(val word: String, val parentId: PrimaryKey?) : TopicCollection
    data class ChildTopicList(val objectId: PrimaryKey) : TopicCollection
    data class UserComments(val uid: PrimaryKey) : TopicCollection
}

sealed interface TitleCollection {
    data object Titles : TitleCollection
    data class SearchTitle(
        val uid: PrimaryKey,
        val searchType: TitleSearchType,
        val status: TitleWorkStatus? = null,
        val type: TitleType? = null,
        val scopeId: PrimaryKey? = null,
    ) : TitleCollection

    data object AllTitles : TitleCollection
}

sealed interface RoomCollection {
    data object Rooms : RoomCollection
    data class SearchRoom(val word: String, val communityId: PrimaryKey?, val joinStatusSearch: JoinStatusSearch) :
        RoomCollection

    data class AllRooms(val isPrivate: Boolean) : RoomCollection

    // 添加社区房间相关的集合定义
    data class CommunityRooms(val communityId: PrimaryKey) : RoomCollection
    data class CommunityRoomSearch(val communityId: PrimaryKey, val word: String) : RoomCollection
}

sealed interface CommunityCollection {
    data object Communities : CommunityCollection

    data class SearchCommunity(
        val joinStatusSearch: JoinStatusSearch,
        val word: String,
        val target: PrimaryKey? = null,
        val hasPointer: PosterSearch? = null,
    ) : CommunityCollection

    data object AllCommunities : CommunityCollection
}

sealed interface ReactionCollection {
    data object Reactions : ReactionCollection

    data class ReactionList(val objectId: PrimaryKey) : ReactionCollection
}

sealed interface UserReactionRecordCollection {
    data class UserReactionRecords(val uid: PrimaryKey) : UserReactionRecordCollection
}

data class UploadCollection(val objectId: PrimaryKey) {
    fun getName() = "upload_$objectId"

    companion object {
        fun fromInfo(uploadInfo: UploadInfo) = UploadCollection(uploadInfo.objectId)
    }
}

sealed interface FileCollection {
    data class FileList(val objectId: PrimaryKey) : FileCollection
    data class FileSearch(val objectId: PrimaryKey, val word: String) : FileCollection
    data object Files : FileCollection
}

sealed interface UserLogCollection {
    data class UserLogs(val uid: PrimaryKey) : UserLogCollection
}

sealed interface UserFavoriteCollection {
    data class UserFavorites(val uid: PrimaryKey) : UserFavoriteCollection
}

sealed interface UserSubscriptionCollection {
    data class UserSubscriptions(val uid: PrimaryKey) : UserSubscriptionCollection
}

sealed interface UploadRecordCollection {
    data class UserUploadRecords(val uid: PrimaryKey) : UploadRecordCollection
}

sealed interface FileRefCollection {
    data class FileRefs(val fileId: PrimaryKey) : FileRefCollection
}

sealed interface PanelLogCollection {
    data class PanelLogs(val targetId: PrimaryKey, val objectType: com.storyteller_f.shared.type.ObjectType) :
        PanelLogCollection
}

sealed interface TaskRecordCollection {
    data class TaskRecords(
        val type: TaskRecordType?,
        /** Filters cached task records by their execution outcome. */
        val isSuccess: Boolean?,
        /** Filters cached failed task records by failure classification. */
        val failureType: String?,
    ) : TaskRecordCollection
}

fun FileCollection.getName(): String =
    when (this) {
        FileCollection.Files -> "files"
        is FileCollection.FileList -> "files_$objectId"
        is FileCollection.FileSearch -> "files_${objectId}_$word"
    }

fun UserCollection.getName(): String =
    when (this) {
        is UserCollection.SearchUser -> "users_$word"
        UserCollection.Users -> "users"
        is UserCollection.Members -> "members_${objectId}_$word"
        UserCollection.AllUsers -> "all_users"
    }

fun RoomCollection.getName(): String =
    when (this) {
        RoomCollection.Rooms -> "rooms"

        is RoomCollection.SearchRoom -> "rooms_${word}_${joinStatusSearch}_${communityId ?: "<none>"}"

        is RoomCollection.AllRooms -> "all_rooms_$isPrivate"

        // 添加社区房间相关的名称生成
        is RoomCollection.CommunityRooms -> "community_rooms_$communityId"

        is RoomCollection.CommunityRoomSearch -> "community_rooms_${communityId}_$word"
    }

fun ReactionCollection.getName(): String =
    when (this) {
        ReactionCollection.Reactions -> "reactions"
        is ReactionCollection.ReactionList -> "reactions_$objectId"
    }

fun UserReactionRecordCollection.getName(): String =
    when (this) {
        is UserReactionRecordCollection.UserReactionRecords -> "user_reaction_records_$uid"
    }

fun TopicCollection.getName(): String =
    when (this) {
        is TopicCollection.SearchTopic -> "topics_${word}_${parentId ?: "<none>"}"
        is TopicCollection.ChildTopicList -> "topics_$objectId"
        TopicCollection.Topics -> "topics"
        TopicCollection.Recommend -> "topics_recommend"
        TopicCollection.AllTopics -> "all_topics"
        is TopicCollection.UserComments -> "user_comments"
    }

fun CommunityCollection.getName(): String =
    when (this) {
        CommunityCollection.Communities -> "communities"

        is CommunityCollection.SearchCommunity ->
            "communities_${word}_${target ?: "<none>"}_${joinStatusSearch}_${hasPointer ?: "<none>"}"

        CommunityCollection.AllCommunities -> "all_communities"
    }

fun TitleCollection.getName(): String =
    when (this) {
        is TitleCollection.SearchTitle ->
            "titles_${uid}_${searchType}_${status ?: "<none>"}_${type ?: "<none>"}_${scopeId ?: "<none>"}"

        TitleCollection.Titles -> "title"

        TitleCollection.AllTitles -> "all_titles"
    }

fun UserLogCollection.getName(): String =
    when (this) {
        is UserLogCollection.UserLogs -> "user_logs_$uid"
    }

fun UserFavoriteCollection.getName(): String =
    when (this) {
        is UserFavoriteCollection.UserFavorites -> "user_favorites_$uid"
    }

fun UserSubscriptionCollection.getName(): String =
    when (this) {
        is UserSubscriptionCollection.UserSubscriptions -> "user_subscriptions_$uid"
    }

fun UploadRecordCollection.getName(): String =
    when (this) {
        is UploadRecordCollection.UserUploadRecords -> "upload_records_$uid"
    }

fun FileRefCollection.getName(): String =
    when (this) {
        is FileRefCollection.FileRefs -> "file_refs_$fileId"
    }

fun PanelLogCollection.getName(): String =
    when (this) {
        is PanelLogCollection.PanelLogs -> "panel_logs_${targetId}_$objectType"
    }

fun TaskRecordCollection.getName(): String {
    val name =
        when (this) {
            is TaskRecordCollection.TaskRecords ->
                "task_records_${type ?: "all"}_${isSuccess ?: "all"}_${failureType ?: "all"}"
        }
    return name
}

@Serializable
data class RemoteKeys(val collectionName: String, val key: String?)

interface ModelStorage {
    val user: UserInfoStorage
    val community: CommunityInfoStorage
    val topic: TopicInfoStorage
    val title: TitleInfoStorage
    val room: RoomInfoStorage
    val member: MemberInfoStorage
    val remoteKey: RemoteKeyStorage
    val reaction: ReactionInfoStorage
    val childAccount: ChildAccountStorage
    val fileInfo: FileInfoStorage
    val download: DownloadInfoStorage
    val upload: UploadInfoStorage
    val overview: OverviewStorage
    val userOverview: UserOverviewStorage
    val favorite: UserFavoriteStorage
    val subscription: UserSubscriptionStorage
    val userReactionRecord: UserReactionRecordStorage
    val userLog: UserLogInfoStorage
    val uploadRecord: UploadRecordInfoStorage
    val fileRef: FileRefInfoStorage
    val panelLog: PanelLogInfoStorage
    val taskRecord: TaskRecordInfoStorage
}

interface UserInfoStorage : CollectionListStorageWithDefault<UserCollection, UserInfo>

interface CommunityInfoStorage : CollectionListStorageWithDefault<CommunityCollection, CommunityInfo>

interface TopicInfoStorage : CollectionListStorageWithDefault<TopicCollection, TopicDetail>

interface TitleInfoStorage : CollectionListStorageWithDefault<TitleCollection, TitleDetail>

interface RoomInfoStorage : CollectionListStorageWithDefault<RoomCollection, RoomInfo>

interface ReactionInfoStorage : CollectionListStorage<ReactionCollection, ReactionInfo>

sealed interface MemberCollection {
    data class CommunityMembers(val objectId: PrimaryKey, val word: String? = null) : MemberCollection
    data class RoomMembers(val objectId: PrimaryKey, val word: String? = null) : MemberCollection
}

fun MemberCollection.getName(): String =
    when (this) {
        is MemberCollection.CommunityMembers ->
            if (word.isNullOrBlank()) {
                "community_members_$objectId"
            } else {
                "community_members_${objectId}_$word"
            }

        is MemberCollection.RoomMembers ->
            if (word.isNullOrBlank()) {
                "room_members_$objectId"
            } else {
                "room_members_${objectId}_$word"
            }
    }

interface MemberInfoStorage : CollectionListStorage<MemberCollection, MemberInfo>

interface ChildAccountStorage : GlobalListStorage<ChildAccountInfo> {
    companion object {
        const val COLLECTION_NAME = "child-account"
    }
}

interface FileInfoStorage : CollectionListStorageWithDefault<FileCollection, FileInfo>

interface DownloadInfoStorage {
    /** Isolate public resources by server without reading or migrating legacy collections. */
    fun forServer(server: String): DownloadInfoStorage

    suspend fun save(item: DownloadInfo)
    fun observeData(): PagingSource<Int, DownloadInfo>
    suspend fun clean()
    suspend fun getDocumentByFileId(fileId: PrimaryKey): DownloadInfo?
    fun observeDocumentByFileId(fileId: PrimaryKey): Flow<DownloadInfo?>

    companion object {
        const val COLLECTION_NAME = "download"
    }
}

interface UploadInfoStorage : CollectionListStorage<UploadCollection, UploadInfo> {
    fun observeDatumByHash(collection: UploadCollection, pathHash: String): Flow<UploadInfo?>
}

interface OverviewStorage : SingletonItemStorage<PanelOverview> {
    companion object {
        const val COLLECTION_NAME = "panel-overview"
    }
}

interface UserOverviewStorage : SingletonItemStorage<UserOverview> {
    companion object {
        const val COLLECTION_NAME = "user-overview"
    }
}

interface UserFavoriteStorage : CollectionListStorageWithDefault<UserFavoriteCollection, UserFavoriteItem> {
    companion object {
        const val COLLECTION_NAME = "user-favorite"
    }
}

interface UserSubscriptionStorage : CollectionListStorageWithDefault<UserSubscriptionCollection, UserSubscriptionItem> {
    companion object {
        const val COLLECTION_NAME = "user-subscription"
    }
}

interface UserReactionRecordStorage : CollectionListStorage<UserReactionRecordCollection, ReactionRecordInfo>

interface UserLogInfoStorage : CollectionListStorage<UserLogCollection, UserLogItem>

interface UploadRecordInfoStorage : CollectionListStorage<UploadRecordCollection, UploadRecordInfo>

interface FileRefInfoStorage : CollectionListStorage<FileRefCollection, FileRefInfo>

interface PanelLogInfoStorage : CollectionListStorage<PanelLogCollection, PanelLogInfo>

interface TaskRecordInfoStorage : CollectionListStorage<TaskRecordCollection, TaskRecordInfo>

interface RemoteKeyStorage {
    suspend fun getPreRemoteKey(collection: String): RemoteKeys?
    suspend fun getNextRemoteKey(collection: String): RemoteKeys?
    suspend fun savePreRemoteKey(remoteKeys: RemoteKeys)
    suspend fun saveNextRemoteKey(remoteKeys: RemoteKeys)

    suspend fun deletePreRemoteKey(collection: String)
    suspend fun deleteNextRemoteKey(collection: String)

    companion object {
        const val PRE_COLLECTION = "pre_remote_keys"
        const val NEXT_COLLECTION = "next_remote_keys"
    }
}

data class RemoteKeyStorageWrapper(val remoteKeyStorage: RemoteKeyStorage, val collectionName: String) {
    suspend fun getPreRemoteKey() = remoteKeyStorage.getPreRemoteKey(collectionName)
    suspend fun getNextRemoteKey() = remoteKeyStorage.getNextRemoteKey(collectionName)
    suspend fun savePreRemoteKey(remoteKeys: RemoteKeys) = remoteKeyStorage.savePreRemoteKey(remoteKeys)
    suspend fun saveNextRemoteKey(remoteKeys: RemoteKeys) = remoteKeyStorage.saveNextRemoteKey(remoteKeys)
    suspend fun deletePreRemoteKey() = remoteKeyStorage.deletePreRemoteKey(collectionName)
    suspend fun deleteNextRemoteKey() = remoteKeyStorage.deleteNextRemoteKey(collectionName)
}

fun RemoteKeyStorage.wrap(collectionName: String) = RemoteKeyStorageWrapper(this, collectionName)

suspend fun TopicInfoStorage.update(collection: TopicCollection, id: PrimaryKey, block: (TopicDetail) -> TopicDetail) {
    val document = getDocument(collection, id) ?: return
    val value = block(document)
    updateDocument(collection, value)
}

suspend fun TitleInfoStorage.update(collection: TitleCollection, id: PrimaryKey, block: (TitleDetail) -> TitleDetail) {
    val document = getDocument(collection, id) ?: return
    val value = block(document)
    updateDocument(collection, value)
}

suspend fun RoomInfoStorage.update(collection: RoomCollection, id: PrimaryKey, block: (RoomInfo) -> RoomInfo) {
    val document = getDocument(collection, id) ?: return
    val value = block(document)
    updateDocument(collection, value)
}

suspend fun CommunityInfoStorage.update(
    collection: CommunityCollection,
    id: PrimaryKey,
    block: (CommunityInfo) -> CommunityInfo,
) {
    val document = getDocument(collection, id) ?: return
    val value = block(document)
    updateDocument(collection, value)
}

suspend fun FileInfoStorage.update(collection: FileCollection, id: PrimaryKey, block: (FileInfo) -> FileInfo) {
    val document = getDocument(collection, id) ?: return
    val value = block(document)
    updateDocument(collection, value)
}

suspend fun UserInfoStorage.update(collection: UserCollection, id: PrimaryKey, block: (UserInfo) -> UserInfo) {
    val document = getDocument(collection, id) ?: return
    val value = block(document)
    updateDocument(collection, value)
}

/** A type-changing mapping supplies a policy that can read the previous generation's mapped pages. */
class WrappedPagingSource<K : Any, T : Any, M : Any>(
    val rawSource: PagingSource<K, T>,
    private val refreshKey: (PagingState<K, M>) -> K? = { null },
    val process: suspend (List<T>) -> List<M>,
) : PagingSource<K, M>() {
    init {
        rawSource.registerInvalidatedCallback { invalidate() }
    }

    override fun getRefreshKey(state: PagingState<K, M>): K? = refreshKey(state)

    override suspend fun load(params: LoadParams<K>): LoadResult<K, M> =
        when (val result = rawSource.load(params)) {
            is LoadResult.Page<K, T> ->
                LoadResult.Page(
                    process(result.data),
                    result.prevKey,
                    result.nextKey,
                    result.itemsBefore,
                    result.itemsAfter,
                )

            is LoadResult.Error<K, T> -> LoadResult.Error(result.throwable)

            is LoadResult.Invalid<K, T> -> LoadResult.Invalid()
        }
}
