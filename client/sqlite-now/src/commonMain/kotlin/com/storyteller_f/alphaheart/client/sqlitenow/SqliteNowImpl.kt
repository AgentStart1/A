package com.storyteller_f.alphaheart.client.sqlitenow

import androidx.paging.PagingSource
import com.storyteller_f.alphaheart.client.sqlitenow.db.AppDatabase
import com.storyteller_f.alphaheart.client.sqlitenow.db.DownloadEntityQuery
import com.storyteller_f.alphaheart.client.sqlitenow.db.UploadEntityQuery
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FileRefInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.model.PanelOverview
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UploadRecordInfo
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogItem
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.ChildAccountStorage
import com.storyteller_f.storage.CommunityCollection
import com.storyteller_f.storage.CommunityInfoStorage
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadInfoStorage
import com.storyteller_f.storage.FileCollection
import com.storyteller_f.storage.FileInfoStorage
import com.storyteller_f.storage.FileRefCollection
import com.storyteller_f.storage.FileRefInfoStorage
import com.storyteller_f.storage.MemberCollection
import com.storyteller_f.storage.MemberInfoStorage
import com.storyteller_f.storage.OverviewStorage
import com.storyteller_f.storage.PanelLogCollection
import com.storyteller_f.storage.PanelLogInfoStorage
import com.storyteller_f.storage.ReactionCollection
import com.storyteller_f.storage.ReactionInfoStorage
import com.storyteller_f.storage.RemoteKeyStorage
import com.storyteller_f.storage.RemoteKeys
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.RoomInfoStorage
import com.storyteller_f.storage.TaskRecordCollection
import com.storyteller_f.storage.TaskRecordInfoStorage
import com.storyteller_f.storage.TitleCollection
import com.storyteller_f.storage.TitleInfoStorage
import com.storyteller_f.storage.TopicCollection
import com.storyteller_f.storage.TopicInfoStorage
import com.storyteller_f.storage.UploadCollection
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadInfoStorage
import com.storyteller_f.storage.UploadRecordCollection
import com.storyteller_f.storage.UploadRecordInfoStorage
import com.storyteller_f.storage.UserCollection
import com.storyteller_f.storage.UserFavoriteCollection
import com.storyteller_f.storage.UserFavoriteStorage
import com.storyteller_f.storage.UserInfoStorage
import com.storyteller_f.storage.UserLogCollection
import com.storyteller_f.storage.UserLogInfoStorage
import com.storyteller_f.storage.UserOverviewStorage
import com.storyteller_f.storage.UserReactionRecordCollection
import com.storyteller_f.storage.UserReactionRecordStorage
import com.storyteller_f.storage.UserSubscriptionCollection
import com.storyteller_f.storage.UserSubscriptionStorage
import com.storyteller_f.storage.getName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

class SqliteNowUserInfoStorage(db: AppDatabase) : UserInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: UserInfo) = saveLast(UserCollection.Users, item)
    override fun observeDatum(key: String): Flow<UserInfo?> = storage.observeDatum(UserCollection.Users.getName(), key)
    override suspend fun saveLast(collection: UserCollection, item: UserInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: UserCollection, item: UserInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: UserCollection): PagingSource<Int, UserInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: UserCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: UserCollection, key: String): UserInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: UserCollection, item: UserInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: UserCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowCommunityInfoStorage(db: AppDatabase) : CommunityInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: CommunityInfo) = saveLast(CommunityCollection.Communities, item)
    override fun observeDatum(key: String): Flow<CommunityInfo?> = storage.observeDatum(CommunityCollection.Communities.getName(), key)
    override suspend fun saveLast(collection: CommunityCollection, item: CommunityInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: CommunityCollection, item: CommunityInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: CommunityCollection): PagingSource<Int, CommunityInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: CommunityCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: CommunityCollection, key: String): CommunityInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: CommunityCollection, item: CommunityInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: CommunityCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowTopicInfoStorage(db: AppDatabase) : TopicInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: TopicDetail) = saveLast(TopicCollection.Topics, item)
    override fun observeDatum(key: String): Flow<TopicDetail?> = storage.observeDatum(TopicCollection.Topics.getName(), key)
    override suspend fun saveLast(collection: TopicCollection, item: TopicDetail) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: TopicCollection, item: TopicDetail) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: TopicCollection): PagingSource<Int, TopicDetail> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: TopicCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: TopicCollection, key: String): TopicDetail? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: TopicCollection, item: TopicDetail) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: TopicCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowTitleInfoStorage(db: AppDatabase) : TitleInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: TitleDetail) = saveLast(TitleCollection.Titles, item)
    override fun observeDatum(key: String): Flow<TitleDetail?> = storage.observeDatum(TitleCollection.Titles.getName(), key)
    override suspend fun saveLast(collection: TitleCollection, item: TitleDetail) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: TitleCollection, item: TitleDetail) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: TitleCollection): PagingSource<Int, TitleDetail> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: TitleCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: TitleCollection, key: String): TitleDetail? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: TitleCollection, item: TitleDetail) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: TitleCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowRoomInfoStorage(db: AppDatabase) : RoomInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: RoomInfo) = saveLast(RoomCollection.Rooms, item)
    override fun observeDatum(key: String): Flow<RoomInfo?> = storage.observeDatum(RoomCollection.Rooms.getName(), key)
    override suspend fun saveLast(collection: RoomCollection, item: RoomInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: RoomCollection, item: RoomInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: RoomCollection): PagingSource<Int, RoomInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: RoomCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: RoomCollection, key: String): RoomInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: RoomCollection, item: RoomInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: RoomCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowMemberInfoStorage(db: AppDatabase) : MemberInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: MemberCollection, item: MemberInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: MemberCollection, item: MemberInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: MemberCollection): PagingSource<Int, MemberInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: MemberCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: MemberCollection, key: String): MemberInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: MemberCollection, item: MemberInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: MemberCollection, key: String) = storage.delete(collection.getName(), key)
}

class RemoteKeySqliteNowStorage(val appDatabase: AppDatabase) : RemoteKeyStorage {
    private val storage = CommonStorageImpl(appDatabase)

    override suspend fun getPreRemoteKey(collection: String): RemoteKeys? {
        return storage.getDocument<RemoteKeys>(RemoteKeyStorage.PRE_COLLECTION, collection)
    }

    override suspend fun getNextRemoteKey(collection: String): RemoteKeys? {
        return storage.getDocument<RemoteKeys>(RemoteKeyStorage.NEXT_COLLECTION, collection)
    }

    override suspend fun savePreRemoteKey(remoteKeys: RemoteKeys) {
        storage.save(RemoteKeyStorage.PRE_COLLECTION, remoteKeys.collectionName, com.storyteller_f.shared.commonJson.encodeToString(remoteKeys))
    }

    override suspend fun saveNextRemoteKey(remoteKeys: RemoteKeys) {
        storage.save(RemoteKeyStorage.NEXT_COLLECTION, remoteKeys.collectionName, com.storyteller_f.shared.commonJson.encodeToString(remoteKeys))
    }

    override suspend fun deletePreRemoteKey(collection: String) {
        storage.delete(RemoteKeyStorage.PRE_COLLECTION, collection)
    }

    override suspend fun deleteNextRemoteKey(collection: String) {
        storage.delete(RemoteKeyStorage.NEXT_COLLECTION, collection)
    }
}

class SqliteNowReactionInfoStorage(db: AppDatabase) : ReactionInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: ReactionCollection, item: ReactionInfo) = storage.saveLast(
        collection.getName(),
        item.objectId.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: ReactionCollection, item: ReactionInfo) = storage.saveFirst(
        collection.getName(),
        item.objectId.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: ReactionCollection): PagingSource<Int, ReactionInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: ReactionCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: ReactionCollection, key: String): ReactionInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: ReactionCollection, item: ReactionInfo) = storage.update(
        collection.getName(),
        item.objectId.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: ReactionCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowChildAccountStorage(db: AppDatabase) : ChildAccountStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun save(item: ChildAccountInfo) = storage.save(
        ChildAccountStorage.COLLECTION_NAME,
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun clean() = storage.clean(ChildAccountStorage.COLLECTION_NAME)
    override suspend fun getDocument(key: String): ChildAccountInfo? = storage.getDocument(
        ChildAccountStorage.COLLECTION_NAME,
        key
    )
    override fun observeData(): PagingSource<Int, ChildAccountInfo> = storage.observeData(
        ChildAccountStorage.COLLECTION_NAME
    )
    override fun observeDatum(key: String): Flow<ChildAccountInfo?> = storage.observeDatum(
        ChildAccountStorage.COLLECTION_NAME,
        key
    )
    suspend fun delete(key: String) = storage.delete(ChildAccountStorage.COLLECTION_NAME, key)
}

class SqliteNowFileInfoStorage(db: AppDatabase) : FileInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: FileInfo) = saveLast(FileCollection.Files, item)
    override fun observeDatum(key: String): Flow<FileInfo?> = storage.observeDatum(FileCollection.Files.getName(), key)
    override suspend fun saveLast(collection: FileCollection, item: FileInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: FileCollection, item: FileInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: FileCollection): PagingSource<Int, FileInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: FileCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: FileCollection, key: String): FileInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: FileCollection, item: FileInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: FileCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowDownloadInfoStorage(val db: AppDatabase) : DownloadInfoStorage {
    private fun ensureOpen() {
        try {
            db.connection()
        } catch (e: IllegalStateException) {
            runBlocking {
                db.open()
            }
        }
    }

    override suspend fun getDocumentByFileId(fileId: PrimaryKey): DownloadInfo? {
        ensureOpen()
        val row = db.downloadEntity.getByFileId(
            DownloadEntityQuery.GetByFileId.Params(
                collection = DownloadInfoStorage.COLLECTION_NAME,
                fileId = fileId.toLong()
            )
        ).asOneOrNull()
        return row?.data?.let {
            try {
                com.storyteller_f.shared.commonJson.decodeFromString<DownloadInfo>(it)
            } catch (e: Exception) {
                io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                null
            }
        }
    }

    override suspend fun save(item: DownloadInfo) {
        ensureOpen()
        db.downloadEntity.insert(
            DownloadEntityQuery.Insert.Params(
                collection = DownloadInfoStorage.COLLECTION_NAME,
                id = item.id.toString(),
                fileId = item.fileInfo.id.toLong(),
                data = com.storyteller_f.shared.commonJson.encodeToString(item)
            )
        )
    }

    override fun observeData(): PagingSource<Int, DownloadInfo> {
        return OffsetPagingSource(
            queryFlow = db.downloadEntity.getPaged(
                DownloadEntityQuery.GetPaged.Params(
                    collection = DownloadInfoStorage.COLLECTION_NAME,
                    limit = 1,
                    offset = 0
                )
            ).asFlow().onStart {
                ensureOpen()
            }
        ) { limit, offset ->
            ensureOpen()
            val rows = db.downloadEntity.getPaged(
                DownloadEntityQuery.GetPaged.Params(
                    collection = DownloadInfoStorage.COLLECTION_NAME,
                    limit = limit,
                    offset = offset
                )
            ).asList()
            rows.mapNotNull {
                try {
                    com.storyteller_f.shared.commonJson.decodeFromString<DownloadInfo>(it.data)
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                    null
                }
            }
        }
    }

    override suspend fun clean() {
        ensureOpen()
        db.downloadEntity.clean(DownloadEntityQuery.Clean.Params(DownloadInfoStorage.COLLECTION_NAME))
    }

    override fun observeDatum(key: String): Flow<DownloadInfo?> {
        val flow = db.downloadEntity.getById(
            DownloadEntityQuery.GetById.Params(
                collection = DownloadInfoStorage.COLLECTION_NAME,
                id = key
            )
        ).asFlow().onStart {
            ensureOpen()
        }
        return flow.map { list ->
            list.firstOrNull()?.data?.let { string ->
                try {
                    com.storyteller_f.shared.commonJson.decodeFromString<DownloadInfo>(string)
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                    null
                }
            }
        }
    }

    override suspend fun getDocument(key: String): DownloadInfo? {
        ensureOpen()
        val row = db.downloadEntity.getById(
            DownloadEntityQuery.GetById.Params(
                collection = DownloadInfoStorage.COLLECTION_NAME,
                id = key
            )
        ).asOneOrNull()
        return row?.data?.let {
            try {
                com.storyteller_f.shared.commonJson.decodeFromString<DownloadInfo>(it)
            } catch (e: Exception) {
                io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                null
            }
        }
    }
}

class SqliteNowUploadInfoStorage(val db: AppDatabase) : UploadInfoStorage {
    private fun ensureOpen() {
        try {
            db.connection()
        } catch (e: IllegalStateException) {
            runBlocking {
                db.open()
            }
        }
    }

    override fun observeDatumByHash(
        collection: UploadCollection,
        pathHash: String
    ): Flow<UploadInfo?> {
        val flow = db.uploadEntity.getByPathHash(
            UploadEntityQuery.GetByPathHash.Params(
                collection = collection.getName(),
                pathHash = pathHash
            )
        ).asFlow().onStart {
            ensureOpen()
        }
        return flow.map { list ->
            list.firstOrNull()?.data?.let { string ->
                try {
                    com.storyteller_f.shared.commonJson.decodeFromString<UploadInfo>(string)
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                    null
                }
            }
        }
    }

    private suspend fun saveInternal(collection: UploadCollection, item: UploadInfo) {
        ensureOpen()
        db.uploadEntity.insert(
            UploadEntityQuery.Insert.Params(
                collection = collection.getName(),
                id = item.objectId.toString(),
                data = com.storyteller_f.shared.commonJson.encodeToString(item),
                pathHash = item.pathHash ?: ""
            )
        )
    }

    override suspend fun saveLast(collection: UploadCollection, item: UploadInfo) = saveInternal(collection, item)

    override suspend fun saveFirst(collection: UploadCollection, item: UploadInfo) = saveInternal(collection, item)

    override fun observeData(collection: UploadCollection): PagingSource<Int, UploadInfo> {
        return OffsetPagingSource(
            queryFlow = db.uploadEntity.getPaged(
                UploadEntityQuery.GetPaged.Params(
                    collection = collection.getName(),
                    limit = 1,
                    offset = 0
                )
            ).asFlow().onStart {
                ensureOpen()
            }
        ) { limit, offset ->
            ensureOpen()
            val rows = db.uploadEntity.getPaged(
                UploadEntityQuery.GetPaged.Params(
                    collection = collection.getName(),
                    limit = limit,
                    offset = offset
                )
            ).asList()
            rows.mapNotNull {
                try {
                    com.storyteller_f.shared.commonJson.decodeFromString<UploadInfo>(it.data)
                } catch (e: Exception) {
                    io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                    null
                }
            }
        }
    }

    override suspend fun clean(collection: UploadCollection) {
        ensureOpen()
        db.uploadEntity.clean(UploadEntityQuery.Clean.Params(collection.getName()))
    }

    override suspend fun getDocument(collection: UploadCollection, key: String): UploadInfo? {
        ensureOpen()
        val row = db.uploadEntity.getById(
            UploadEntityQuery.GetById.Params(
                collection = collection.getName(),
                id = key
            )
        ).asOneOrNull()
        return row?.data?.let {
            try {
                com.storyteller_f.shared.commonJson.decodeFromString<UploadInfo>(it)
            } catch (e: Exception) {
                io.github.aakira.napier.Napier.e(e) { "Error decoding JSON" }
                null
            }
        }
    }

    override suspend fun updateDocument(collection: UploadCollection, item: UploadInfo) {
        saveInternal(collection, item)
    }

    override suspend fun delete(collection: UploadCollection, key: String) {
        ensureOpen()
        db.uploadEntity.delete(
            UploadEntityQuery.Delete.Params(
                collection = collection.getName(),
                id = key
            )
        )
    }
}

class SqliteNowOverviewStorage(db: AppDatabase) : OverviewStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun save(item: PanelOverview) = storage.save(
        OverviewStorage.COLLECTION_NAME,
        "0",
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeDatum(): Flow<PanelOverview?> = storage.observeDatum(OverviewStorage.COLLECTION_NAME, "0")
}

class SqliteNowUserOverviewStorage(db: AppDatabase) : UserOverviewStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun save(item: UserOverview) = storage.save(
        UserOverviewStorage.COLLECTION_NAME,
        "0",
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeDatum(): Flow<UserOverview?> = storage.observeDatum(UserOverviewStorage.COLLECTION_NAME, "0")
}

class SqliteNowUserFavoriteStorage(db: AppDatabase) : UserFavoriteStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: UserFavoriteItem) = saveLast(
        UserFavoriteCollection.UserFavorites(item.id.toLong()),
        item
    )
    override fun observeDatum(key: String): Flow<UserFavoriteItem?> = storage.observeDatum(
        UserFavoriteStorage.COLLECTION_NAME,
        key
    )
    override suspend fun saveLast(collection: UserFavoriteCollection, item: UserFavoriteItem) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: UserFavoriteCollection, item: UserFavoriteItem) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(
        collection: UserFavoriteCollection
    ): PagingSource<Int, UserFavoriteItem> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: UserFavoriteCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(
        collection: UserFavoriteCollection,
        key: String
    ): UserFavoriteItem? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: UserFavoriteCollection, item: UserFavoriteItem) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: UserFavoriteCollection, key: String) = storage.delete(
        collection.getName(),
        key
    )
}

class SqliteNowUserSubscriptionStorage(db: AppDatabase) : UserSubscriptionStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveToDefault(item: UserSubscriptionItem) = saveLast(
        UserSubscriptionCollection.UserSubscriptions(item.id.toLong()),
        item
    )
    override fun observeDatum(key: String): Flow<UserSubscriptionItem?> = storage.observeDatum(
        UserSubscriptionStorage.COLLECTION_NAME,
        key
    )
    override suspend fun saveLast(
        collection: UserSubscriptionCollection,
        item: UserSubscriptionItem
    ) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(
        collection: UserSubscriptionCollection,
        item: UserSubscriptionItem
    ) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(
        collection: UserSubscriptionCollection
    ): PagingSource<Int, UserSubscriptionItem> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: UserSubscriptionCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(
        collection: UserSubscriptionCollection,
        key: String
    ): UserSubscriptionItem? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(
        collection: UserSubscriptionCollection,
        item: UserSubscriptionItem
    ) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: UserSubscriptionCollection, key: String) = storage.delete(
        collection.getName(),
        key
    )
}

class SqliteNowUserReactionRecordStorage(db: AppDatabase) : UserReactionRecordStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(
        collection: UserReactionRecordCollection,
        item: ReactionRecordInfo
    ) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(
        collection: UserReactionRecordCollection,
        item: ReactionRecordInfo
    ) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(
        collection: UserReactionRecordCollection
    ): PagingSource<Int, ReactionRecordInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: UserReactionRecordCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(
        collection: UserReactionRecordCollection,
        key: String
    ): ReactionRecordInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(
        collection: UserReactionRecordCollection,
        item: ReactionRecordInfo
    ) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: UserReactionRecordCollection, key: String) = storage.delete(
        collection.getName(),
        key
    )
}

class SqliteNowUserLogInfoStorage(db: AppDatabase) : UserLogInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: UserLogCollection, item: UserLogItem) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: UserLogCollection, item: UserLogItem) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: UserLogCollection): PagingSource<Int, UserLogItem> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: UserLogCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: UserLogCollection, key: String): UserLogItem? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: UserLogCollection, item: UserLogItem) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: UserLogCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowUploadRecordInfoStorage(db: AppDatabase) : UploadRecordInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: UploadRecordCollection, item: UploadRecordInfo) = storage.saveLast(
        collection.getName(),
        item.objectId.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: UploadRecordCollection, item: UploadRecordInfo) = storage.saveFirst(
        collection.getName(),
        item.objectId.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(
        collection: UploadRecordCollection
    ): PagingSource<Int, UploadRecordInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: UploadRecordCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(
        collection: UploadRecordCollection,
        key: String
    ): UploadRecordInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: UploadRecordCollection, item: UploadRecordInfo) = storage.update(
        collection.getName(),
        item.objectId.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: UploadRecordCollection, key: String) = storage.delete(
        collection.getName(),
        key
    )
}

class SqliteNowFileRefInfoStorage(db: AppDatabase) : FileRefInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: FileRefCollection, item: FileRefInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: FileRefCollection, item: FileRefInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: FileRefCollection): PagingSource<Int, FileRefInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: FileRefCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: FileRefCollection, key: String): FileRefInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: FileRefCollection, item: FileRefInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: FileRefCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowPanelLogInfoStorage(db: AppDatabase) : PanelLogInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: PanelLogCollection, item: PanelLogInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: PanelLogCollection, item: PanelLogInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: PanelLogCollection): PagingSource<Int, PanelLogInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: PanelLogCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: PanelLogCollection, key: String): PanelLogInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: PanelLogCollection, item: PanelLogInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: PanelLogCollection, key: String) = storage.delete(collection.getName(), key)
}

class SqliteNowTaskRecordInfoStorage(db: AppDatabase) : TaskRecordInfoStorage {
    private val storage = CommonStorageImpl(db)
    override suspend fun saveLast(collection: TaskRecordCollection, item: TaskRecordInfo) = storage.saveLast(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun saveFirst(collection: TaskRecordCollection, item: TaskRecordInfo) = storage.saveFirst(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override fun observeData(collection: TaskRecordCollection): PagingSource<Int, TaskRecordInfo> = storage.observeData(
        collection.getName()
    )
    override suspend fun clean(collection: TaskRecordCollection) = storage.clean(collection.getName())
    override suspend fun getDocument(collection: TaskRecordCollection, key: String): TaskRecordInfo? = storage.getDocument(
        collection.getName(),
        key
    )
    override suspend fun updateDocument(collection: TaskRecordCollection, item: TaskRecordInfo) = storage.update(
        collection.getName(),
        item.id.toString(),
        com.storyteller_f.shared.commonJson.encodeToString(item)
    )
    override suspend fun delete(collection: TaskRecordCollection, key: String) = storage.delete(collection.getName(), key)
}
