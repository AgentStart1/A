/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.UserAuthData
import com.storyteller_f.alphaheart.backend.core.UserDatabase
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.AssetTransaction
import com.storyteller_f.alphaheart.backend.core.types.ChildAccount
import com.storyteller_f.alphaheart.backend.core.types.RawChildAccount
import com.storyteller_f.alphaheart.backend.core.types.RawUser
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.UserDevice
import com.storyteller_f.alphaheart.backend.core.types.UserLog
import com.storyteller_f.alphaheart.backend.core.types.UserTopicRead
import com.storyteller_f.alphaheart.backend.core.types.UserTwoFactor
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.isEmpty
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.Aids
import com.storyteller_f.alphaheart.backend.exposed.tables.AssetTransactions
import com.storyteller_f.alphaheart.backend.exposed.tables.ChildAccounts
import com.storyteller_f.alphaheart.backend.exposed.tables.TaskRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.UserDevices
import com.storyteller_f.alphaheart.backend.exposed.tables.UserLogs
import com.storyteller_f.alphaheart.backend.exposed.tables.UserTopicReads
import com.storyteller_f.alphaheart.backend.exposed.tables.UserTwoFactors
import com.storyteller_f.alphaheart.backend.exposed.tables.Users
import com.storyteller_f.alphaheart.backend.exposed.tables.addTaskRecord
import com.storyteller_f.alphaheart.backend.exposed.tables.encodeRecoveryCodeHashes
import com.storyteller_f.alphaheart.backend.exposed.tables.find
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapPresent
import com.storyteller_f.shared.model.TaskRecordSummary
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.longLiteral
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.upsert

class ExposedUserDatabase(
    private val databaseSession: ExposedDatabaseSession,
    private val combinedDatabase: CombinedDatabase,
) : UserDatabase {
    override suspend fun getUserAid(id: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                Aids.selectAll().where {
                    Aids.objectId eq id
                }
            }
            first {
                it[Aids.value]
            }
        }

    override suspend fun getRawUser(objectFetch: ObjectFetch, uid: PrimaryKey?) =
        getUserByPredicate(User::wrapRow) {
            where {
                when (objectFetch) {
                    is ObjectFetch.AidFetch -> Aids.value eq objectFetch.aid
                    is ObjectFetch.IdFetch -> Users.id eq objectFetch.id
                }
            }
        }.andThenPresent(DatabaseError::fromError) { user ->
            processUserToRawUser(uid, listOf(user))
        }.mapPresent {
            it.first()
        }

    override suspend fun getRawUserAndPublicKeyByAddress(ad: String) =
        getUserByPredicate({
            val value = User.wrapRow(it)
            Pair(RawUser(value), value.publicKey)
        }) {
            where {
                Users.address eq ad
            }
        }

    override suspend fun createUser(user: User): CustomResult<User, DatabaseError> =
        databaseSession.dbQuery {
            createUserRaw(user)
            val aid = user.aid
            if (!aid.isNullOrBlank()) {
                check(
                    Aids.insert {
                        it[objectId] = user.id
                        it[value] = aid
                        it[objectType] = ObjectType.USER
                    }.insertedCount > 0,
                ) {
                    "insert aid failed"
                }
            }
            user
        }

    private suspend fun createUserRaw(user: User) {
        check(
            Users.insert {
                it[id] = user.id
                it[encryptionPublicKey] = user.encryptionPublicKey
                it[publicKey] = user.publicKey
                it[publicKeyMd5] = md5(user.publicKey)
                it[address] = user.address
                it[nickname] = user.nickname
                it[createdTime] = user.createdTime
                it[notificationId] = user.notificationId
                it[passType] = user.passType
                it[algoType] = user.algoType
            }.insertedCount > 0,
        ) {
            "insert user failed"
        }
    }

    override suspend fun isUserNotExistsByPublicKey(pk: String) =
        databaseSession.dbSearch {
            search {
                val md5 = md5(pk)
                User.find {
                    Users.publicKeyMd5 eq md5 and (Users.publicKey eq pk)
                }
            }
            isEmpty()
        }

    override suspend fun updateUserStatus(id: PrimaryKey, status: com.storyteller_f.shared.type.UserStatus) =
        databaseSession.dbQuery {
            Users.update({
                Users.id eq id
            }) {
                it[this.status] = status
            } > 0
        }

    override suspend fun updateUserInfo(id: PrimaryKey, newUser: UpdateUserBody) =
        databaseSession.dbQuery {
            updateUser(newUser, id) && updateAid(newUser, id)
        }

    private suspend fun updateAid(newUser: UpdateUserBody, id: PrimaryKey): Boolean {
        val aid = newUser.aid
        if (aid.isNullOrBlank()) {
            return true
        }
        return Aids.upsert(Aids.objectId) {
            it[objectId] = id
            it[value] = aid
            it[objectType] = ObjectType.USER
        }.insertedCount > 0
    }

    private suspend fun updateUser(newUser: UpdateUserBody, id: PrimaryKey): Boolean {
        val avatar = newUser.avatar
        val name = newUser.nickname
        if (name.isNullOrBlank() && avatar == null) {
            return true
        }
        return Users.update({
            Users.id eq id
        }) {
            if (!name.isNullOrBlank()) {
                it[nickname] = name
            }
            if (avatar != null) {
                it[icon] = avatar
            }
        } > 0
    }

    private suspend fun getUserAuthDataBy(predicate: () -> Op<Boolean>) =
        databaseSession.dbSearch {
            search {
                Users.select(listOf(Users.publicKey, Users.id, Users.algoType)).where(predicate)
            }
            first {
                UserAuthData(it[Users.publicKey], it[Users.id], it[Users.algoType])
            }
        }

    override suspend fun getUserAuthDataById(id: PrimaryKey) =
        getUserAuthDataBy {
            Users.id eq id
        }

    override suspend fun getUserAuthDataByAid(aid: String) =
        getUserByPredicate({
            UserAuthData(it[Users.publicKey], it[Users.id], it[Users.algoType])
        }) {
            where {
                Aids.value eq aid
            }
        }

    override suspend fun getUserAuthDataByAddress(address: String) =
        getUserAuthDataBy {
            Users.address eq address
        }

    override suspend fun getRawUsers(
        objectListFetch: ObjectListFetch,
        uid: PrimaryKey?,
    ): CustomResult<List<RawUser>, DatabaseError> {
        if (objectListFetch is ObjectListFetch.AidListFetch && objectListFetch.aidList.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        if (objectListFetch is ObjectListFetch.IdListFetch && objectListFetch.idList.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        return getUserListByPredicate({
            where {
                when (objectListFetch) {
                    is ObjectListFetch.AidListFetch -> Aids.value inList objectListFetch.aidList
                    is ObjectListFetch.IdListFetch -> Users.id inList objectListFetch.idList
                }
            }
        }, User::wrapRow).andThen(DatabaseError::fromError) {
            processUserToRawUser(uid, it)
        }
    }

    override suspend fun getUserAcgByIds(objectListFetch: ObjectListFetch) =
        getUserListByPredicate(
            {
                where {
                    when (objectListFetch) {
                        is ObjectListFetch.AidListFetch -> Aids.value inList objectListFetch.aidList
                        is ObjectListFetch.IdListFetch -> Users.id inList objectListFetch.idList
                    }
                }
            },
        ) {
            it[Users.id] to it[Users.acgAmount]
        }

    override suspend fun addReadLog(userTopicRead: UserTopicRead) =
        databaseSession.dbQuery {
            check(
                UserTopicReads.upsert(onUpdate = {
                    it[UserTopicReads.updatedAt] = userTopicRead.updatedAt
                    it[UserTopicReads.topicId] =
                        CustomFunction(
                            "GREATEST",
                            UserTopicReads.topicId.columnType,
                            UserTopicReads.topicId,
                            longLiteral(userTopicRead.topicId),
                        )
                }) {
                    it[uid] = userTopicRead.uid
                    it[updatedAt] = userTopicRead.updatedAt
                    it[objectId] = userTopicRead.objectId
                    it[objectType] = userTopicRead.objectType
                    it[topicId] = userTopicRead.topicId
                }.insertedCount > 0,
            ) {
                "log failed"
            }
        }

    override suspend fun insertUserLog(log: UserLog) =
        databaseSession.dbQuery {
            check(
                UserLogs.insert {
                    it[UserLogs.id] = log.id
                    it[uid] = log.uid
                    it[type] = log.type
                    it[objectId] = log.objectId
                    it[objectType] = log.objectType
                    it[UserLogs.createdTime] = log.createdTime
                }.insertedCount > 0,
            ) {
                "Insert user log failed"
            }
        }

    override suspend fun addDevice(uid: PrimaryKey, endpointUrl: String) =
        databaseSession.dbQuery {
            check(
                UserDevices.insert {
                    it[UserDevices.uid] = uid
                    it[UserDevices.endpointUrl] = endpointUrl
                }.insertedCount > 0,
            ) {
                "Insert device failed"
            }
        }

    override suspend fun removeDevice(uid: PrimaryKey, endpointUrl: String) =
        databaseSession.dbQuery {
            UserDevices.deleteWhere {
                UserDevices.uid eq uid and (UserDevices.endpointUrl eq endpointUrl)
            }
        }

    override suspend fun getUserDevices(uid: List<PrimaryKey>) =
        databaseSession.dbSearch {
            search {
                UserDevices.selectAll().where {
                    UserDevices.uid inList uid
                }
            }
            map(UserDevice::wrapRow)
        }

    override suspend fun addAcgForUser(record: TaskRecord, assetTransaction: AssetTransaction?) =
        databaseSession.dbQuery {
            assetTransaction?.let { at ->
                check(
                    AssetTransactions.insert {
                        it[AssetTransactions.id] = at.id
                        it[AssetTransactions.uid] = at.uid
                        it[AssetTransactions.createdTime] = at.createdTime
                        it[AssetTransactions.type] = at.type
                        it[AssetTransactions.before] = at.before
                        it[AssetTransactions.after] = at.after
                    }.insertedCount > 0,
                ) {
                    "Insert asset transaction failed"
                }
                check(
                    Users.update({
                        Users.id eq at.uid and (Users.acgAmount eq at.before)
                    }) {
                        it[Users.acgAmount] = at.after
                    } > 0,
                ) {
                    "update user acg failed"
                }
            }
            addTaskRecord(record)
        }

    override suspend fun getLatestTaskRecord(type: TaskRecordType): CustomResult<TaskRecord?, DatabaseError> {
        val result =
            databaseSession.dbSearch {
                search {
                    TaskRecords.selectAll().where {
                        TaskRecords.type eq type
                    }.orderBy(TaskRecords.objectId, SortOrder.DESC)
                }
                first(TaskRecord::wrapRow)
            }
        return result
    }

    override suspend fun getTaskRecordSummaries(): CustomResult<List<TaskRecordSummary>, DatabaseError> =
        TaskRecordType.entries.fold(CustomResult.success(emptyList())) { result, type ->
            result.andThen(DatabaseError::fromError) { summaries ->
                getTaskRecordSummary(
                    type,
                ).andThen(DatabaseError::fromError) { summary -> CustomResult.success(summaries + summary) }
            }
        }

    override suspend fun getTaskRecordsToRetry(
        type: TaskRecordType,
        limit: Int,
    ): CustomResult<List<TaskRecord>, DatabaseError> {
        val result =
            databaseSession.dbSearch {
                search {
                    TaskRecords.selectAll().where {
                        TaskRecords.type eq type and
                            (TaskRecords.success eq false) and
                            (TaskRecords.retryRequested eq true)
                    }.orderBy(TaskRecords.id, SortOrder.ASC).limit(limit)
                }
                map(TaskRecord::wrapRow)
            }
        return result
    }

    override suspend fun updateTaskRecordRetryRequested(
        id: PrimaryKey,
        isRequested: Boolean,
    ): CustomResult<Boolean, DatabaseError> =
        databaseSession.dbQuery {
            TaskRecords.update({
                if (isRequested) {
                    TaskRecords.id eq id and (TaskRecords.success eq false)
                } else {
                    TaskRecords.id eq id
                }
            }) {
                it[TaskRecords.retryRequested] = isRequested
            } > 0
        }

    private suspend fun getTaskRecordSummary(type: TaskRecordType): CustomResult<TaskRecordSummary, DatabaseError> =
        getTaskRecordCount(type, true).andThen(DatabaseError::fromError) { successCount ->
            getTaskRecordCount(type, false).andThen(DatabaseError::fromError) { failureCount ->
                getRetryRequestedTaskRecordCount(type).andThen(DatabaseError::fromError) { retryRequestedCount ->
                    CustomResult.success(
                        TaskRecordSummary(
                            type = type,
                            successCount = successCount,
                            failureCount = failureCount,
                            retryRequestedCount = retryRequestedCount,
                        ),
                    )
                }
            }
        }

    private suspend fun getTaskRecordCount(
        type: TaskRecordType,
        isSuccess: Boolean,
    ): CustomResult<Long, DatabaseError> {
        val result =
            databaseSession.dbSearch {
                search {
                    TaskRecords.select(TaskRecords.id).where {
                        TaskRecords.type eq type and (TaskRecords.success eq isSuccess)
                    }
                }
                count()
            }
        return result
    }

    private suspend fun getRetryRequestedTaskRecordCount(type: TaskRecordType): CustomResult<Long, DatabaseError> {
        val result =
            databaseSession.dbSearch {
                search {
                    TaskRecords.select(TaskRecords.id).where {
                        TaskRecords.type eq type and
                            (TaskRecords.success eq false) and
                            (TaskRecords.retryRequested eq true)
                    }
                }
                count()
            }
        return result
    }

    override suspend fun getRawChildAccountPaginationListByHost(hostId: PrimaryKey, fetch: PrimaryKeyFetch) =
        paginationFromResults(
            databaseSession.dbSearch {
                search {
                    Users.join(ChildAccounts, JoinType.INNER, Users.id, ChildAccounts.uid)
                        .join(Aids, JoinType.LEFT, Users.id, Aids.objectId)
                        .select(Users.fields + ChildAccounts.fields + Aids.value).where {
                            ChildAccounts.hostId eq hostId
                        }.bindPaginationQuery(Users, fetch)
                }
                map {
                    val childAccount = ChildAccount.wrapRow(it)
                    val user = User.wrapRow(it)
                    RawChildAccount(childAccount, RawUser(user))
                }
            },
            getChildAccountCount(hostId),
        )

    override suspend fun getChildAccountCount(hostId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                ChildAccounts.join(Users, JoinType.INNER, ChildAccounts.uid, Users.id)
                    .selectAll()
                    .where {
                        ChildAccounts.hostId eq hostId
                    }
            }
            count()
        }

    override suspend fun getChildAccountIds(hostId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                ChildAccounts.select(ChildAccounts.uid).where {
                    ChildAccounts.hostId eq hostId
                }
            }
            map {
                it[ChildAccounts.uid]
            }
        }

    override suspend fun getRawChildAccount(uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                ChildAccounts.selectAll().where {
                    ChildAccounts.uid eq uid
                }
            }
            first {
                ChildAccount.wrapRow(it)
            }
        }

    override suspend fun createChildAccount(
        hostId: PrimaryKey,
        encryptedPrivateKey: String,
        encryptedAesKey: String,
        user: User,
        encryptedEncryptionPrivateKey: String?,
    ) = databaseSession.dbQuery {
        createUserRaw(user)
        check(
            ChildAccounts.insert {
                it[this.hostId] = hostId
                it[this.encryptedPrivateKey] = encryptedPrivateKey
                it[this.encryptedAesKey] = encryptedAesKey
                it[this.primaryKeyMd5] = md5(encryptedPrivateKey)
                it[this.encryptedEncryptionPrivateKey] = encryptedEncryptionPrivateKey
                it[uid] = user.id
            }.insertedCount > 0,
        ) {
            "Insert alternate account failed"
        }
    }

    override suspend fun getAllUsers(primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            getUserListByPredicate(
                { bindPaginationQuery(Users, primaryKeyFetch) },
                User::wrapRow,
            ).andThen(DatabaseError::fromError) {
                processUserToRawUser(null, it)
            },
            getUserCount(),
        )

    override suspend fun getUserCount() = getUserCountByPredicate()

    private suspend fun <T> getUserListByPredicate(
        queryBuilder: Query.() -> Query = { this },
        block: (ResultRow) -> T,
    ) = databaseSession.dbSearch {
        search {
            Users.join(Aids, JoinType.LEFT, Users.id, Aids.objectId)
                .selectAll()
                .queryBuilder()
        }
        map(block)
    }

    private suspend fun <T> getUserByPredicate(block: (ResultRow) -> T, queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Users.join(Aids, JoinType.LEFT, Users.id, Aids.objectId)
                    .selectAll()
                    .queryBuilder()
            }
            first(block)
        }

    private suspend fun getUserCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Users.select(Users.id).queryBuilder()
            }
            count()
        }

    override suspend fun getUserLogs(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
        paginationFromResults(
            databaseSession.dbSearch {
                search {
                    UserLogs.selectAll().where {
                        UserLogs.uid eq uid
                    }.orderBy(UserLogs.id, SortOrder.DESC).bindPaginationQuery(UserLogs, fetch)
                }
                map(UserLog::wrapRow)
            },
            databaseSession.dbSearch {
                search {
                    UserLogs.select(UserLogs.id).where { UserLogs.uid eq uid }
                }
                count()
            },
        )

    override suspend fun getUserTwoFactor(uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserTwoFactors.selectAll().where {
                    UserTwoFactors.uid eq uid
                }
            }
            first(UserTwoFactor::wrapRow)
        }

    override suspend fun upsertUserTwoFactor(twoFactor: UserTwoFactor) =
        databaseSession.dbQuery {
            check(
                UserTwoFactors.upsert(UserTwoFactors.uid, onUpdate = {
                    it[UserTwoFactors.enabled] = twoFactor.enabled
                    it[UserTwoFactors.type] = twoFactor.type
                    it[UserTwoFactors.totpSecret] = twoFactor.totpSecret
                    it[UserTwoFactors.recoveryCodeHashes] = encodeRecoveryCodeHashes(twoFactor.recoveryCodeHashes)
                    it[UserTwoFactors.updatedAt] = twoFactor.updatedAt
                }) {
                    it[uid] = twoFactor.uid
                    it[enabled] = twoFactor.enabled
                    it[type] = twoFactor.type
                    it[totpSecret] = twoFactor.totpSecret
                    it[recoveryCodeHashes] = encodeRecoveryCodeHashes(twoFactor.recoveryCodeHashes)
                    it[updatedAt] = twoFactor.updatedAt
                    it[createdTime] = twoFactor.updatedAt
                }.insertedCount > 0,
            ) {
                "upsert user two factor failed"
            }
        }

    override suspend fun disableUserTwoFactor(uid: PrimaryKey) =
        databaseSession.dbQuery {
            UserTwoFactors.update({
                UserTwoFactors.uid eq uid
            }) {
                it[enabled] = false
            }
            Unit
        }

    override suspend fun updateRecoveryCodeHashes(uid: PrimaryKey, hashes: List<String>) =
        databaseSession.dbQuery {
            UserTwoFactors.update({
                UserTwoFactors.uid eq uid
            }) {
                it[recoveryCodeHashes] = encodeRecoveryCodeHashes(hashes)
            }
            Unit
        }

    private suspend fun processUserToRawUser(
        uid: PrimaryKey?,
        users: List<User>,
    ): CustomResult<List<RawUser>, DatabaseError> =
        captureFailure(DatabaseError::from) {
            val userIds = users.map { it.id }
            if (userIds.isEmpty()) return@captureFailure emptyList()

            val favoriteMap =
                if (uid != null) {
                    combinedDatabase.favorite.getHasFavorite(ObjectListFetch.IdListFetch(userIds), uid)
                        .getOrThrow().associateBy { it.objectId }
                } else {
                    emptyMap()
                }

            val subscriptionMap =
                if (uid != null) {
                    combinedDatabase.subscription.getHasSubscription(ObjectListFetch.IdListFetch(userIds), uid)
                        .getOrThrow().associateBy { it.objectId }
                } else {
                    emptyMap()
                }

            users.map { user ->
                RawUser(
                    user,
                    favoriteId = favoriteMap[user.id]?.id,
                    subscriptionId = subscriptionMap[user.id]?.id,
                )
            }
        }
}
