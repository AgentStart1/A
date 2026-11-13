/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.types.AssetTransaction
import com.storyteller_f.alphaheart.backend.core.types.ChildAccount
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.core.types.FileRecord
import com.storyteller_f.alphaheart.backend.core.types.FileRef
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.PanelAccount
import com.storyteller_f.alphaheart.backend.core.types.PanelLog
import com.storyteller_f.alphaheart.backend.core.types.Quota
import com.storyteller_f.alphaheart.backend.core.types.RawChildAccount
import com.storyteller_f.alphaheart.backend.core.types.RawCommunity
import com.storyteller_f.alphaheart.backend.core.types.RawPanelAccount
import com.storyteller_f.alphaheart.backend.core.types.RawRoom
import com.storyteller_f.alphaheart.backend.core.types.RawTitle
import com.storyteller_f.alphaheart.backend.core.types.RawUser
import com.storyteller_f.alphaheart.backend.core.types.ReactionRecord
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.core.types.RoomActivityRecord
import com.storyteller_f.alphaheart.backend.core.types.SubscriptionSentLog
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.Title
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.UserDevice
import com.storyteller_f.alphaheart.backend.core.types.UserFavorite
import com.storyteller_f.alphaheart.backend.core.types.UserLog
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.core.types.UserTopicRead
import com.storyteller_f.alphaheart.backend.core.types.UserTwoFactor
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.model.NestedMemberInfo
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.QuotaInfo
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.TaskRecordSummary
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.model.UserPubKeyInfo
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.PresetCommunity
import com.storyteller_f.shared.obj.PresetRoom
import com.storyteller_f.shared.obj.PresetTopic
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class PaginationResult<T>(val list: List<T>, val total: Long)

fun <T, E> CustomResult<List<T>?, E>.paging(total: Long) = mapPresent { PaginationResult(it, total) }

fun <T, E> CustomResult<List<T>, E>.pagingNotNull(total: Long) = map { PaginationResult(it, total) }

inline fun <T, E : FailureDetails, R, F> CustomResult<PaginationResult<T>, E>.mapPagingResultNotNull(
    mapError: (FailureDetails) -> F,
    block: (List<T>) -> CustomResult<List<R>, FailureDetails>,
): CustomResult<PaginationResult<R>, F> =
    andThen(mapError) { page ->
        block(page.list).map { PaginationResult(it, page.total) }
    }

inline fun <T, E, R> CustomResult<PaginationResult<T>, E>.mapPagingNotNull(
    block: (List<T>) -> List<R>,
): CustomResult<PaginationResult<R>, E> = map { PaginationResult(block(it.list), it.total) }

inline fun <T, E : FailureDetails, R, F> CustomResult<PaginationResult<T>?, E>.mapPagingIfNotNull(
    mapError: (FailureDetails) -> F,
    block: (List<T>) -> CustomResult<List<R>, FailureDetails>,
): CustomResult<PaginationResult<R>?, F> =
    andThenPresent(mapError) { page ->
        block(page.list).map { PaginationResult(it, page.total) }
    }

inline fun <T, E : FailureDetails, R, F> CustomResult<PaginationResult<T>?, E>.mapPagingResultIfNotNullNullable(
    mapError: (FailureDetails) -> F,
    block: (List<T>) -> CustomResult<List<R>?, FailureDetails>,
): CustomResult<PaginationResult<R>?, F> =
    andThenPresent(mapError) { page ->
        block(page.list).mapPresent { PaginationResult(it, page.total) }
    }

inline fun <T, E : FailureDetails, R, F> CustomResult<PaginationResult<T>, E>.mapPagingResultNullable(
    mapError: (FailureDetails) -> F,
    block: (List<T>) -> CustomResult<List<R>?, FailureDetails>,
): CustomResult<PaginationResult<R>?, F> =
    andThen(mapError) { page ->
        block(page.list).mapPresent { PaginationResult(it, page.total) }
    }

fun <T> paginationFromResults(
    listResult: CustomResult<List<T>, DatabaseError>,
    totalResult: CustomResult<Long, DatabaseError>,
): CustomResult<PaginationResult<T>, DatabaseError> =
    listResult.andThen(DatabaseError::fromError) { list ->
        totalResult.map { total -> PaginationResult(list, total) }
    }

fun <T> paginationNullableFromResults(
    listResult: CustomResult<List<T>?, DatabaseError>,
    totalResult: CustomResult<Long, DatabaseError>,
): CustomResult<PaginationResult<T>?, DatabaseError> =
    listResult.andThenPresent(DatabaseError::fromError) { list ->
        totalResult.map { total -> PaginationResult(list, total) }
    }

data class ContainerInfo(
    val member: Member?,
    val userTopicRead: UserTopicRead?,
    val memberCount: Long?,
    val latestTopicId: PrimaryKey?,
    val favoriteId: PrimaryKey? = null,
    val subscriptionId: PrimaryKey? = null,
)

data class UserAuthData(val publicKey: String, val userId: PrimaryKey, val algoType: AlgoType)
data class MemberAuthData(
    val publicKey: String,
    val encryptionPublicKey: String?,
    val userId: PrimaryKey,
    val roomAid: String,
    val algoType: AlgoType,
)

sealed interface JoinSearch {
    data class Joined(val uid: PrimaryKey) : JoinSearch
    data class Unspecified(val uid: PrimaryKey?) : JoinSearch
}

data class InsertTopicTuple(
    val topic: PresetTopic,
    val originalIndex: Int,
    val level: Int,
    val id: PrimaryKey,
    val content: ByteArray,
    val isEncrypted: Boolean,
    val rootId: PrimaryKey,
)

data class InsertCommunityTuple(
    val community: PresetCommunity,
    val icon: PrimaryKey?,
    val id: PrimaryKey,
    val fontSettings: FontSettings?,
    val createdTime: LocalDateTime,
)

data class InsertRoomTuple(
    val room: PresetRoom,
    val icon: PrimaryKey?,
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
)

/** Database access for persisted worker task configurations. */
interface WorkerTaskDatabase {
    /** Returns the persisted configuration for one worker task, or null when it has not been configured. */
    suspend fun getWorkerTask(type: TaskRecordType): CustomResult<WorkerTask?, DatabaseError>

    /** Returns all persisted worker task configurations. */
    suspend fun getWorkerTasks(): CustomResult<List<WorkerTask>, DatabaseError>

    /** Creates or replaces worker task configurations by task type. */
    suspend fun upsertWorkerTasks(configs: List<WorkerTask>): CustomResult<Unit, DatabaseError>
}

interface CombinedDatabase {
    val user: UserDatabase
    val topic: TopicDatabase
    val title: TitleDatabase
    val community: CommunityDatabase
    val room: RoomDatabase
    val file: FileDatabase
    val container: ContainerDatabase
    val admin: AdminDatabase
    val panelAccount: PanelAccountDatabase
    val reaction: ReactionDatabase
    val favorite: FavoriteDatabase
    val subscription: SubscriptionDatabase

    /** Persisted worker task configuration access. */
    val workerTask: WorkerTaskDatabase

    /** Initializes the database schema and required resources. */
    suspend fun init()

    /** Removes persisted database data. */
    suspend fun clean()

    /** Returns a persisted backend configuration value, or null if the key is not configured. */
    suspend fun getBackendConfig(key: String): CustomResult<String?, DatabaseError>

    /** Creates or replaces a persisted backend configuration value. */
    suspend fun upsertBackendConfig(key: String, value: String): CustomResult<Unit, DatabaseError>

    suspend fun migration()
}

interface UserDatabase {
    suspend fun getUserAid(id: PrimaryKey): CustomResult<String?, DatabaseError>
    suspend fun getRawUser(objectFetch: ObjectFetch, uid: PrimaryKey? = null): CustomResult<RawUser?, DatabaseError>
    suspend fun getRawUserAndPublicKeyByAddress(ad: String): CustomResult<Pair<RawUser, String>?, DatabaseError>
    suspend fun createUser(user: User): CustomResult<User, DatabaseError>
    suspend fun isUserNotExistsByPublicKey(pk: String): CustomResult<Boolean, DatabaseError>
    suspend fun updateUserStatus(
        id: PrimaryKey,
        status: com.storyteller_f.shared.type.UserStatus,
    ): CustomResult<Boolean, DatabaseError>

    /**
     * nickname 为null 或者空字符串时不更新，avatar 为null 时不更新,aid 为null 或者空字符串时不更新.
     */
    suspend fun updateUserInfo(id: PrimaryKey, newUser: UpdateUserBody): CustomResult<Boolean, DatabaseError>
    suspend fun getUserAuthDataById(id: PrimaryKey): CustomResult<UserAuthData?, DatabaseError>
    suspend fun getUserAuthDataByAid(aid: String): CustomResult<UserAuthData?, DatabaseError>
    suspend fun getUserAuthDataByAddress(address: String): CustomResult<UserAuthData?, DatabaseError>
    suspend fun getRawUsers(
        objectListFetch: ObjectListFetch,
        uid: PrimaryKey? = null,
    ): CustomResult<List<RawUser>, DatabaseError>
    suspend fun getUserAcgByIds(objectListFetch: ObjectListFetch): CustomResult<List<Pair<Long, Long>>, DatabaseError>
    suspend fun addReadLog(userTopicRead: UserTopicRead): CustomResult<Unit, DatabaseError>
    suspend fun insertUserLog(log: UserLog): CustomResult<Unit, DatabaseError>
    suspend fun addDevice(uid: PrimaryKey, endpointUrl: String): CustomResult<Unit, DatabaseError>
    suspend fun removeDevice(uid: PrimaryKey, endpointUrl: String): CustomResult<Int, DatabaseError>
    suspend fun getUserDevices(uid: List<PrimaryKey>): CustomResult<List<UserDevice>, DatabaseError>
    suspend fun addAcgForUser(
        record: TaskRecord,
        assetTransaction: AssetTransaction?,
    ): CustomResult<Unit, DatabaseError>

    suspend fun getLatestTaskRecord(type: TaskRecordType): CustomResult<TaskRecord?, DatabaseError>

    /** Returns task-type summary rows for panel display. */
    suspend fun getTaskRecordSummaries(): CustomResult<List<TaskRecordSummary>, DatabaseError>

    /** Returns failed task executions selected for retry. */
    suspend fun getTaskRecordsToRetry(type: TaskRecordType, limit: Int): CustomResult<List<TaskRecord>, DatabaseError>

    /** Updates whether a task execution should be retried. */
    suspend fun updateTaskRecordRetryRequested(
        id: PrimaryKey,
        isRequested: Boolean,
    ): CustomResult<Boolean, DatabaseError>

    suspend fun getRawChildAccountPaginationListByHost(
        hostId: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<RawChildAccount>, DatabaseError>

    suspend fun getRawChildAccount(uid: PrimaryKey): CustomResult<ChildAccount?, DatabaseError>
    suspend fun getChildAccountIds(hostId: PrimaryKey): CustomResult<List<PrimaryKey>, DatabaseError>

    suspend fun createChildAccount(
        hostId: PrimaryKey,
        encryptedPrivateKey: String,
        encryptedAesKey: String,
        user: User,
        encryptedEncryptionPrivateKey: String? = null,
    ): CustomResult<Unit, DatabaseError>

    suspend fun getAllUsers(primaryKeyFetch: PrimaryKeyFetch): CustomResult<PaginationResult<RawUser>, DatabaseError>
    suspend fun getUserCount(): CustomResult<Long, DatabaseError>

    suspend fun getUserLogs(
        uid: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<UserLog>, DatabaseError>

    suspend fun getChildAccountCount(hostId: PrimaryKey): CustomResult<Long, DatabaseError>

    suspend fun getUserTwoFactor(uid: PrimaryKey): CustomResult<UserTwoFactor?, DatabaseError>
    suspend fun upsertUserTwoFactor(twoFactor: UserTwoFactor): CustomResult<Unit, DatabaseError>
    suspend fun disableUserTwoFactor(uid: PrimaryKey): CustomResult<Unit, DatabaseError>
    suspend fun updateRecoveryCodeHashes(uid: PrimaryKey, hashes: List<String>): CustomResult<Unit, DatabaseError>
}

interface TopicDatabase {
    suspend fun getTopicRootTuple(parentId: PrimaryKey): CustomResult<ObjectTuple?, DatabaseError>

    suspend fun saveEncryptedTopic(topic: Topic, content: TopicContent.Encrypted): CustomResult<Unit, DatabaseError>
    suspend fun savePlainTopic(
        topic: Topic,
        content: TopicContent.Plain,
        fileRefs: List<FileRef>,
    ): CustomResult<Unit, DatabaseError>

    suspend fun updateTopicPinned(topicId: PrimaryKey, newValue: Boolean): CustomResult<Boolean, DatabaseError>
    suspend fun updateTopicStatus(topicId: PrimaryKey, status: ObjectStatus): CustomResult<Boolean, DatabaseError>
    suspend fun getTopicList(primaryKeyFetch: PrimaryKeyFetch): CustomResult<List<Topic>, DatabaseError>
    suspend fun getTopicCommentCount(topicIdList: List<PrimaryKey>): CustomResult<List<Pair<Long, Long>>, DatabaseError>

    suspend fun isUserCommented(uid: PrimaryKey, topicId: List<PrimaryKey>): CustomResult<List<Long>, DatabaseError>

    suspend fun getUserCommentedTopicsPaginationResult(
        uid: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<Topic>, DatabaseError>

    suspend fun getUserCommentCount(uid: PrimaryKey): CustomResult<Long, DatabaseError>

    suspend fun getTopicContentFromByteArray(
        topics: List<Topic>,
        uid: PrimaryKey?,
    ): CustomResult<Map<PrimaryKey, TopicContent>, DatabaseError>

    suspend fun createTitle(title: Title, topic: Topic): CustomResult<Unit, DatabaseError>
    suspend fun getTopicCount(): CustomResult<Long, DatabaseError>
    suspend fun getTopic(fetch: ObjectFetch): CustomResult<Topic?, DatabaseError>
    suspend fun getAllTopicPagination(
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<Topic>, DatabaseError>
    suspend fun getTopicListByIds(ids: List<PrimaryKey>): CustomResult<List<Topic>, DatabaseError>
    suspend fun getTopicByParentId(
        uid: PrimaryKey?,
        primaryKeyFetch: PrimaryKeyFetch,
        parentId: PrimaryKey,
        pinType: TopicPinSearch?,
    ): CustomResult<PaginationResult<Topic>, DatabaseError>

    suspend fun getJoinedCommunityPinnedTopics(
        uid: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<Topic>, DatabaseError>

    suspend fun getLatestTopic(parentId: PrimaryKey): CustomResult<List<Topic>, DatabaseError>
}

interface FavoriteDatabase {
    suspend fun getUserFavorites(
        uid: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<UserFavorite>, DatabaseError>

    suspend fun addFavorite(userFavorite: UserFavorite): CustomResult<UserFavorite, DatabaseError>
    suspend fun removeFavorite(id: PrimaryKey): CustomResult<Unit, DatabaseError>
    suspend fun getFavorite(id: PrimaryKey): CustomResult<UserFavorite?, DatabaseError>
    suspend fun getFavorite(uid: PrimaryKey, objectId: PrimaryKey): CustomResult<UserFavorite?, DatabaseError>
    suspend fun getHasFavorite(
        idList: ObjectListFetch.IdListFetch,
        uid: PrimaryKey,
    ): CustomResult<List<UserFavorite>, DatabaseError>

    suspend fun getUserFavoriteCount(): CustomResult<Long, DatabaseError>
}

interface SubscriptionDatabase {
    suspend fun getUserSubscriptions(
        uid: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<UserSubscription>, DatabaseError>

    suspend fun addSubscription(userSubscription: UserSubscription): CustomResult<UserSubscription, DatabaseError>
    suspend fun removeSubscription(id: PrimaryKey): CustomResult<Unit, DatabaseError>
    suspend fun getSubscription(id: PrimaryKey): CustomResult<UserSubscription?, DatabaseError>
    suspend fun getSubscription(uid: PrimaryKey, objectId: PrimaryKey): CustomResult<UserSubscription?, DatabaseError>
    suspend fun getSubscriptionsByObjectId(
        objectId: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<List<UserSubscription>, DatabaseError>

    suspend fun insertSubscriptionSentLog(log: SubscriptionSentLog): CustomResult<SubscriptionSentLog, DatabaseError>
    suspend fun getLatestSubscriptionSentLog(objectId: PrimaryKey): CustomResult<SubscriptionSentLog?, DatabaseError>
    suspend fun getHasSubscription(
        idList: ObjectListFetch.IdListFetch,
        uid: PrimaryKey,
    ): CustomResult<List<UserSubscription>, DatabaseError>

    suspend fun getUserSubscriptionCount(uid: PrimaryKey): CustomResult<Long, DatabaseError>
}

interface ReactionDatabase {
    suspend fun statsReactionRecord(
        objectId: PrimaryKey,
        emoji: String,
        objectType: ObjectType,
    ): CustomResult<Unit, DatabaseError>

    suspend fun getReactionInfoPaginationResult(
        objectId: List<PrimaryKey>,
        uid: PrimaryKey?,
        reactionFetch: ReactionFetch,
    ): CustomResult<PaginationResult<ReactionInfo>, DatabaseError>

    suspend fun hasReactedEmoji(
        objectIdList: List<PrimaryKey>,
        uid: PrimaryKey,
    ): CustomResult<List<Pair<Long, String>>, DatabaseError>

    suspend fun getReactionInfo(
        uid: PrimaryKey,
        objectId: PrimaryKey,
        emojiText: String,
    ): CustomResult<ReactionInfo?, DatabaseError>

    suspend fun hasReactedForEmoji(
        objectId: PrimaryKey,
        uid: PrimaryKey,
        emoji: String,
    ): CustomResult<Boolean, DatabaseError>

    suspend fun deleteReaction(
        uid: PrimaryKey,
        emoji: String,
        objectId: PrimaryKey,
    ): CustomResult<Boolean, DatabaseError>

    suspend fun getReactionRecordInfo(
        uid: PrimaryKey,
        emoji: String,
        objectId: PrimaryKey,
    ): CustomResult<ReactionRecordInfo?, DatabaseError>

    suspend fun deleteReaction(reactionId: PrimaryKey): CustomResult<Boolean, DatabaseError>
    suspend fun insertReaction(reactionRecord: ReactionRecord): CustomResult<Unit, DatabaseError>
    suspend fun getReactionCount(objectIdList: List<PrimaryKey>): CustomResult<List<Pair<Long, Long>>, DatabaseError>
    suspend fun getReactionCountForEmoji(
        objectId: List<PrimaryKey>,
        emoji: String,
    ): CustomResult<List<Triple<Long, Long, PrimaryKey?>>, DatabaseError>

    suspend fun getUserReactionRecordsPaginationResult(
        uid: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<ReactionRecord>, DatabaseError>

    suspend fun getUserReactionRecordCount(uid: PrimaryKey): CustomResult<Long, DatabaseError>
}

interface TitleDatabase {
    suspend fun getTitlePaginationResult(
        primaryKeyFetch: PrimaryKeyFetch,
        uid: PrimaryKey,
        searchType: TitleSearchType,
        type: TitleType? = null,
        scopeId: PrimaryKey? = null,
        titleStatus: TitleWorkStatus? = null,
    ): CustomResult<PaginationResult<RawTitle>, DatabaseError>

    suspend fun getAllRawTitles(
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<RawTitle>, DatabaseError>
    suspend fun getTitleCount(): CustomResult<Long, DatabaseError>
    suspend fun getTitle(id: PrimaryKey): CustomResult<RawTitle?, DatabaseError>
    suspend fun updateTitleStatus(id: PrimaryKey, status: ObjectStatus): CustomResult<Boolean, DatabaseError>
}

interface CommunityDatabase {
    suspend fun getRawCommunity(
        objectFetch: ObjectFetch,
        fillJoinInfo: Boolean? = null,
        uid: PrimaryKey? = null,
    ): CustomResult<RawCommunity?, DatabaseError>

    suspend fun getJoinedCommunityIds(uid: PrimaryKey): CustomResult<List<Long>, DatabaseError>
    suspend fun getCommunityPaginationResult(
        hasPosterSearch: PosterSearch?,
        primaryKeyFetch: PrimaryKeyFetch,
        joinSearch: JoinSearch,
    ): CustomResult<PaginationResult<RawCommunity>?, DatabaseError>

    suspend fun createCommunity(
        community: Community,
        memberId: PrimaryKey,
    ): CustomResult<Pair<Community, Member>, DatabaseError>

    suspend fun getRawCommunities(objectListFetch: ObjectListFetch): CustomResult<List<RawCommunity>, DatabaseError>

    /**
     * name 为null 或者空字符串时不更新，icon，poster 为null时不更新.
     */
    suspend fun updateCommunity(id: PrimaryKey, body: UpdateCommunityBody): CustomResult<Boolean, DatabaseError>
    suspend fun updateCommunityStatus(id: PrimaryKey, status: ObjectStatus): CustomResult<Boolean, DatabaseError>
    suspend fun getCommunityCount(): CustomResult<Long, DatabaseError>
}

interface RoomDatabase {
    suspend fun checkRoomIsPrivate(roomId: PrimaryKey): CustomResult<Boolean?, DatabaseError>
    suspend fun getRoomPaginationResult(
        uid: PrimaryKey?,
        community: PrimaryKey?,
        primaryKeyFetch: PrimaryKeyFetch,
        joinSearch: JoinSearch,
    ): CustomResult<PaginationResult<RawRoom>, DatabaseError>

    suspend fun getRoomCommunityId(parentId: PrimaryKey): CustomResult<PrimaryKey?, DatabaseError>
    suspend fun getRoomPubKeyPaginationResult(
        roomId: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<UserPubKeyInfo>, DatabaseError>

    suspend fun getRawRoom(
        objectFetch: ObjectFetch,
        fillJoinInfo: Boolean? = null,
        uid: PrimaryKey? = null,
    ): CustomResult<RawRoom?, DatabaseError>

    suspend fun createRoom(room: Room, members: List<Member>): CustomResult<Room, DatabaseError>
    suspend fun getRawRooms(
        objectListFetch: ObjectListFetch,
        uid: PrimaryKey?,
    ): CustomResult<List<RawRoom>, DatabaseError>

    suspend fun getRoomList(objectListFetch: ObjectListFetch): CustomResult<List<Room>, DatabaseError>
    suspend fun updateRoom(id: PrimaryKey, body: UpdateRoomBody): CustomResult<Boolean, DatabaseError>
    suspend fun updateRoomStatus(id: PrimaryKey, status: ObjectStatus): CustomResult<Boolean, DatabaseError>
    suspend fun getPrivateRoomCount(): CustomResult<Long, DatabaseError>
    suspend fun getPublicRoomCount(): CustomResult<Long, DatabaseError>
    suspend fun getPrivateRoomPaginationResult(
        primaryKeyFetch: PrimaryKeyFetch,
        word: String? = null,
    ): CustomResult<PaginationResult<RawRoom>, DatabaseError>
}

interface FileDatabase {
    suspend fun getFileRecord(owner: PrimaryKey, name: String): CustomResult<FileRecord?, DatabaseError>
    suspend fun getFileRecordByIds(ids: List<PrimaryKey>): CustomResult<List<FileRecord>, DatabaseError>
    suspend fun getFileRecordListByOwner(owner: PrimaryKey): CustomResult<List<FileRecord>, DatabaseError>
    suspend fun getFileRecordByNames(names: List<String>): CustomResult<List<FileRecord>, DatabaseError>
    suspend fun getUploadRecord(id: PrimaryKey): CustomResult<UploadRecord?, DatabaseError>
    suspend fun insertFileRefs(fileRefs: List<FileRef>): CustomResult<Unit, DatabaseError>

    suspend fun getFileRecordPaginationList(
        uid: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<FileRecord>, DatabaseError>

    suspend fun getAllFileRecordPaginationList(
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<FileRecord>, DatabaseError>

    suspend fun insertFileRecord(
        fileRecordList: List<FileRecord>,
        ownerId: PrimaryKey,
        ownerType: ObjectType,
    ): CustomResult<Unit, DatabaseError>

    suspend fun insertUploadRecord(record: UploadRecord): CustomResult<UploadRecord, DatabaseError>

    suspend fun lockFileQuota(quotaInfo: QuotaInfo, operationId: PrimaryKey): CustomResult<Unit, DatabaseError>

    suspend fun finishFileQuota(
        quotaInfo: QuotaInfo,
        operationId: PrimaryKey,
        fileRecords: List<FileRecord>,
    ): CustomResult<List<FileRecord>, DatabaseError>

    /**
     * 只有在插入文件记录成功后才更新上传记录状态，避免文件上传了但是配额没有更新的情况.
     */
    suspend fun updateUploadRecordStatus(
        quotaInfo: QuotaInfo,
        record: UploadRecord,
        fileRecordList: List<FileRecord>,
    ): CustomResult<List<FileRecord>, DatabaseError>

    suspend fun getFileCount(): CustomResult<Long, DatabaseError>
    suspend fun getFileVolume(): CustomResult<Long, DatabaseError>
    suspend fun updateFileStatus(id: PrimaryKey, status: ObjectStatus): CustomResult<Boolean, DatabaseError>

    suspend fun getUploadRecordPaginationList(
        uid: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<UploadRecord>, DatabaseError>

    suspend fun getFileRefsByFileId(
        fileId: PrimaryKey,
        primaryKeyFetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<FileRef>, DatabaseError>
}

interface ContainerDatabase {
    suspend fun isMemberJoined(objectId: PrimaryKey, uid: PrimaryKey?): CustomResult<Boolean, DatabaseError>
    suspend fun addMember(member: Member): CustomResult<Member, DatabaseError>

    suspend fun updateMemberStatus(member: Member): CustomResult<Member, DatabaseError>

    suspend fun deleteMember(containerId: PrimaryKey, id: PrimaryKey): CustomResult<Unit, DatabaseError>
    suspend fun getJoinedUserList(roomId: PrimaryKey): CustomResult<List<Member>, DatabaseError>
    suspend fun getUserJoinedTime(
        parentIds: List<PrimaryKey>,
        uid: PrimaryKey,
    ): CustomResult<List<Member>, DatabaseError>

    suspend fun getMemberCount(parentIds: List<PrimaryKey>): CustomResult<List<Pair<Long, Long>>, DatabaseError>
    suspend fun getContainerInfo(
        parentIds: List<PrimaryKey>,
        uid: PrimaryKey?,
    ): CustomResult<Map<PrimaryKey, ContainerInfo>, DatabaseError>

    suspend fun getTopicReadList(
        parentIds: List<PrimaryKey>,
        uid: PrimaryKey,
    ): CustomResult<List<UserTopicRead>, DatabaseError>
    suspend fun getUsersHasUnreadRoomMap(
        uidList: List<PrimaryKey>,
    ): CustomResult<Map<PrimaryKey, Boolean>, DatabaseError>
    suspend fun getUserUnreadRoomCount(uid: PrimaryKey): CustomResult<Int, DatabaseError>
    suspend fun getUserUnreadMessageCount(uid: PrimaryKey): CustomResult<Long, DatabaseError>
    suspend fun getRoomActivityRecords(
        uid: PrimaryKey,
        roomIds: List<PrimaryKey>,
    ): CustomResult<List<RoomActivityRecord>, DatabaseError>
    suspend fun getUnreadRoomActivityRecords(
        uid: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<RoomActivityRecord>, DatabaseError>

    suspend fun getMemberPaginationResult(
        objectId: PrimaryKey?,
        word: String?,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<RawUser>, DatabaseError>

    suspend fun getMemberWithUserPaginationResult(
        objectId: PrimaryKey,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<Pair<Member, RawUser>>, DatabaseError>

    suspend fun getMemberWithUserByUids(
        objectId: PrimaryKey,
        uidList: List<PrimaryKey>,
    ): CustomResult<List<Pair<Member, RawUser>>, DatabaseError>

    suspend fun getQuotaInfo(ownerId: PrimaryKey, quotaType: QuotaType): CustomResult<Quota?, DatabaseError>
    suspend fun insertQuota(quota: Quota): CustomResult<Unit, DatabaseError>

    suspend fun getLatestTopicInContainer(
        parentIds: List<PrimaryKey>,
        uid: PrimaryKey?,
    ): CustomResult<Map<PrimaryKey, PrimaryKey?>, DatabaseError>

    suspend fun getMember(containerId: PrimaryKey, id: PrimaryKey): CustomResult<Member?, DatabaseError>
    suspend fun getMemberByIds(
        uid: PrimaryKey,
        objectIds: List<PrimaryKey>,
    ): CustomResult<List<Pair<Long, NestedMemberInfo?>>, DatabaseError>
}

interface AdminDatabase {
    suspend fun batchAddUser(users: List<User>)
    suspend fun batchAddCommunities(communities: List<Community>, members: List<Member>)

    suspend fun batchAddRooms(roomList: List<Room>, membersList: List<Member>)

    suspend fun getAllMembers(distinct: List<String>): CustomResult<List<MemberAuthData>, DatabaseError>
    suspend fun batchAddEncryptTopicKeys(
        encryptedKeys: List<Triple<PrimaryKey, ByteArray, Long>>,
    ): CustomResult<Unit, DatabaseError>

    suspend fun batchAddTopics(
        tuples: List<InsertTopicTuple>,
        userMap: Map<String, User>,
        objectType: ObjectType,
    ): CustomResult<Unit, DatabaseError>

    suspend fun createTaskRecord(record: TaskRecord): CustomResult<TaskRecord, DatabaseError>

    suspend fun getTaskRecords(
        type: TaskRecordType?,
        isSuccess: Boolean?,
        failureType: String?,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<TaskRecord>, DatabaseError>

    suspend fun batchAddSubscription(list: List<UserSubscription>): CustomResult<Unit, DatabaseError>
    suspend fun insertPanelLog(log: PanelLog): CustomResult<Unit, DatabaseError>
    suspend fun getPanelLogs(
        targetId: PrimaryKey,
        objectType: com.storyteller_f.shared.type.ObjectType,
        fetch: PrimaryKeyFetch,
    ): CustomResult<PaginationResult<PanelLog>, DatabaseError>
}

interface PanelAccountDatabase {
    suspend fun getPanelAccount(id: PrimaryKey): CustomResult<PanelAccount?, DatabaseError>
    suspend fun addPanelAccount(panelAccount: PanelAccount): CustomResult<Unit, DatabaseError>
    suspend fun getUserAuthDataById(id: PrimaryKey): CustomResult<UserAuthData?, DatabaseError>
    suspend fun getUserAuthDataByAddress(address: String): CustomResult<UserAuthData?, DatabaseError>
    suspend fun getRawUserAndPublicKeyByAddress(ad: String): CustomResult<Pair<RawPanelAccount, String>?, DatabaseError>
    suspend fun isUserNotExistsByPublicKey(pk: String): CustomResult<Boolean, DatabaseError>
}

const val PUBLIC_KEY_LENGTH = 4096
const val ADDRESS_LENGTH = 100
const val USER_NICKNAME = 20
const val COMMUNITY_NAME_LENGTH = 20
const val AID_LENGTH = 20
const val ROOM_NAME_LENGTH = 20

// 最长60，如果超过60 会进行裁切然后在后面添加uuid，保存时预留一部分空间
const val MEDIA_NAME_LENGTH = 120
