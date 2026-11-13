/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.paging.PagingSource
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FileRefInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.model.PanelOverview
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.ReactionRecordInfo
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
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.OverviewStorage
import com.storyteller_f.storage.PanelLogCollection
import com.storyteller_f.storage.PanelLogInfoStorage
import com.storyteller_f.storage.ReactionCollection
import com.storyteller_f.storage.ReactionInfoStorage
import com.storyteller_f.storage.RemoteKeyStorage
import com.storyteller_f.storage.RemoteKeyStorage.Companion.NEXT_COLLECTION
import com.storyteller_f.storage.RemoteKeyStorage.Companion.PRE_COLLECTION
import com.storyteller_f.storage.RemoteKeys
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
import com.storyteller_f.storage.WrappedPagingSource
import com.storyteller_f.storage.getName
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class RoomUserInfoStorage(appDatabase: AppDatabase) : UserInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    private suspend fun saveToDefaultCollections(item: UserInfo): CommonEntity {
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(item.id, UserCollection.Users.getName(), data)
        buildList {
            add(entity)
            item.aid?.let { add(entity.copy(id = it)) }
        }.forEach {
            impl.save(it)
        }
        return entity
    }

    override suspend fun saveToDefault(item: UserInfo) {
        saveToDefaultCollections(item)
    }

    override suspend fun saveLast(collection: UserCollection, item: UserInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is UserCollection.Users) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveLast(entity)
        }
    }

    override suspend fun saveFirst(collection: UserCollection, item: UserInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is UserCollection.Users) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveFirst(entity)
        }
    }

    override fun observeData(collection: UserCollection): PagingSource<Int, UserInfo> =
        impl.observeData(
            collection.getName(),
        )

    override fun observeDatum(key: String): Flow<UserInfo?> = impl.observeDatum(UserCollection.Users.getName(), key)

    override suspend fun getDocument(collection: UserCollection, key: String): UserInfo? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: UserCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun clean(collection: UserCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun updateDocument(collection: UserCollection, item: UserInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomCommunityInfoStorage(appDatabase: AppDatabase) : CommunityInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    private suspend fun saveToDefaultCollections(item: CommunityInfo): CommonEntity {
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(item.id, CommunityCollection.Communities.getName(), data)
        buildList {
            add(entity)
            add(entity.copy(id = item.aid))
        }.forEach {
            impl.save(it)
        }
        return entity
    }

    override suspend fun saveToDefault(item: CommunityInfo) {
        saveToDefaultCollections(item)
    }

    override suspend fun saveLast(collection: CommunityCollection, item: CommunityInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is CommunityCollection.Communities) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveLast(entity)
        }
    }

    override suspend fun saveFirst(collection: CommunityCollection, item: CommunityInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is CommunityCollection.Communities) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveFirst(entity)
        }
    }

    override fun observeData(collection: CommunityCollection): PagingSource<Int, CommunityInfo> =
        impl.observeData(
            collection.getName(),
        )

    override fun observeDatum(key: String): Flow<CommunityInfo?> {
        val scope = CommunityCollection.Communities.getName()
        return impl.observeDatum(scope, key)
    }

    override suspend fun getDocument(collection: CommunityCollection, key: String): CommunityInfo? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: CommunityCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun clean(collection: CommunityCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun updateDocument(collection: CommunityCollection, item: CommunityInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomTopicInfoStorage(val appDatabase: AppDatabase) : TopicInfoStorage {
    val impl = CommonStorageImpl(appDatabase)
    private suspend fun saveToDefaultCollections(item: TopicDetail): CommonEntity {
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(item.id, TopicCollection.Topics.getName(), data)
        buildList {
            add(entity)
            item.aid?.let { add(entity.copy(id = it)) }
        }.forEach {
            appDatabase.getCommonDao().insert(it)
        }
        return entity
    }

    override suspend fun saveToDefault(item: TopicDetail) {
        saveToDefaultCollections(item)
    }

    override suspend fun saveLast(collection: TopicCollection, item: TopicDetail) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is TopicCollection.Topics) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveLast(entity)
        }
    }

    override suspend fun saveFirst(collection: TopicCollection, item: TopicDetail) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is TopicCollection.Topics) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveFirst(entity)
        }
    }

    override fun observeData(collection: TopicCollection): PagingSource<Int, TopicDetail> =
        impl.observeData(
            collection.getName(),
        )

    override fun observeDatum(key: String): Flow<TopicDetail?> =
        impl.observeDatum(
            TopicCollection.Topics.getName(),
            key,
        )

    override suspend fun getDocument(collection: TopicCollection, key: String): TopicDetail? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: TopicCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun clean(collection: TopicCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun updateDocument(collection: TopicCollection, item: TopicDetail) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomTitleInfoStorage(appDatabase: AppDatabase) : TitleInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    private suspend inline fun saveToCollections(item: TitleDetail): CommonEntity {
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(item.id, TitleCollection.Titles.getName(), data)
        impl.save(entity)
        return entity
    }

    override suspend fun saveToDefault(item: TitleDetail) {
        saveToCollections(item)
    }

    override suspend fun saveLast(collection: TitleCollection, item: TitleDetail) {
        val commonEntity = saveToCollections(item)
        if (collection !is TitleCollection.Titles) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveLast(entity)
        }
    }

    override suspend fun saveFirst(collection: TitleCollection, item: TitleDetail) {
        val commonEntity = saveToCollections(item)
        if (collection !is TitleCollection.Titles) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveFirst(entity)
        }
    }

    override fun observeData(collection: TitleCollection): PagingSource<Int, TitleDetail> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: TitleCollection) {
        impl.clean(collection.getName())
    }

    override fun observeDatum(key: String): Flow<TitleDetail?> =
        impl.observeDatum(
            TitleCollection.Titles.getName(),
            key,
        )

    override suspend fun getDocument(collection: TitleCollection, key: String): TitleDetail? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: TitleCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: TitleCollection, item: TitleDetail) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomMemberInfoStorage(appDatabase: AppDatabase) : MemberInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveLast(collection: MemberCollection, item: MemberInfo) {
        val data = commonJson.encodeToString<MemberInfo>(item)
        val entity = CommonEntity(item.id, collection.getName(), data)
        impl.saveLast(entity)
    }

    override suspend fun saveFirst(collection: MemberCollection, item: MemberInfo) {
        val data = commonJson.encodeToString<MemberInfo>(item)
        val entity = CommonEntity(item.id, collection.getName(), data)
        impl.saveFirst(entity)
    }

    override fun observeData(collection: MemberCollection): PagingSource<Int, MemberInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: MemberCollection) {
        impl.clean(collection.getName())
    }
    override suspend fun getDocument(collection: MemberCollection, key: String): MemberInfo? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: MemberCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: MemberCollection, item: MemberInfo) {
        val data = commonJson.encodeToString<MemberInfo>(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RemoteKeyRoomStorage(val appDatabase: AppDatabase) : RemoteKeyStorage {
    override suspend fun getPreRemoteKey(collection: String): RemoteKeys? =
        appDatabase.getCommonDao().get(
            PRE_COLLECTION,
            collection,
        )?.let {
            commonJson.safeDecodeFromStringOrNull(it.data)
        }

    override suspend fun getNextRemoteKey(collection: String): RemoteKeys? =
        appDatabase.getCommonDao().get(
            NEXT_COLLECTION,
            collection,
        )?.let {
            commonJson.safeDecodeFromStringOrNull(it.data)
        }

    override suspend fun savePreRemoteKey(remoteKeys: RemoteKeys) {
        appDatabase.getCommonDao().insert(
            CommonEntity(remoteKeys.collectionName, PRE_COLLECTION, commonJson.encodeToString(remoteKeys)),
        )
    }

    override suspend fun saveNextRemoteKey(remoteKeys: RemoteKeys) {
        appDatabase.getCommonDao()
            .insert(CommonEntity(remoteKeys.collectionName, NEXT_COLLECTION, commonJson.encodeToString(remoteKeys)))
    }

    override suspend fun deletePreRemoteKey(collection: String) {
        appDatabase.getCommonDao().delete(PRE_COLLECTION, collection)
    }

    override suspend fun deleteNextRemoteKey(collection: String) {
        appDatabase.getCommonDao().delete(NEXT_COLLECTION, collection)
    }
}

class RoomReactionInfoStorage(appDatabase: AppDatabase) : ReactionInfoStorage {
    val impl = CommonStorageImpl(appDatabase)
    suspend fun saveToDefaultCollections(item: ReactionInfo): CommonEntity {
        val id = "${item.objectId}-${item.emoji}"
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(id, ReactionCollection.Reactions.getName(), data)
        impl.save(entity)
        return entity
    }

    override suspend fun saveLast(collection: ReactionCollection, item: ReactionInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection == ReactionCollection.Reactions) {
            impl.saveLast(commonEntity.copy(collection = collection.getName()))
        }
    }

    override suspend fun saveFirst(collection: ReactionCollection, item: ReactionInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection == ReactionCollection.Reactions) {
            impl.saveFirst(commonEntity.copy(collection = collection.getName()))
        }
    }

    override fun observeData(collection: ReactionCollection): PagingSource<Int, ReactionInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: ReactionCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: ReactionCollection, key: String): ReactionInfo? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: ReactionCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: ReactionCollection, item: ReactionInfo) {
        val id = "${item.objectId}-${item.emoji}"
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), id, data)
    }
}

class RoomChildAccountStorage(appDatabase: AppDatabase) : ChildAccountStorage {
    val impl = CommonStorageImpl(appDatabase)
    override suspend fun save(item: ChildAccountInfo) {
        val data = commonJson.encodeToString(item)
        impl.save(CommonEntity(item.id, ChildAccountStorage.COLLECTION_NAME, data))
    }

    override fun observeData(): PagingSource<Int, ChildAccountInfo> =
        impl.observeData(
            ChildAccountStorage.COLLECTION_NAME,
        )

    override suspend fun clean() {
        impl.clean(ChildAccountStorage.COLLECTION_NAME)
    }

    override fun observeDatum(key: String): Flow<ChildAccountInfo?> =
        impl.observeDatum(
            ChildAccountStorage.COLLECTION_NAME,
            key,
        )
}

class RoomFileInfoStorage(appDatabase: AppDatabase) : FileInfoStorage {
    val impl = CommonStorageImpl(appDatabase)
    override suspend fun saveToDefault(item: FileInfo) {
        saveToDefaultCollection(item)
    }

    private suspend fun saveToDefaultCollection(item: FileInfo): CommonEntity {
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(item.id, FileCollection.Files.getName(), data)
        impl.save(entity)
        return entity
    }

    override suspend fun saveFirst(collection: FileCollection, item: FileInfo) {
        val commonEntity = saveToDefaultCollection(item)
        impl.saveFirst(commonEntity.copy(collection = collection.getName()))
    }

    override suspend fun saveLast(collection: FileCollection, item: FileInfo) {
        val commonEntity = saveToDefaultCollection(item)
        impl.saveLast(commonEntity.copy(collection = collection.getName()))
    }

    override fun observeData(collection: FileCollection): PagingSource<Int, FileInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: FileCollection) {
        impl.clean(collection.getName())
    }

    override fun observeDatum(key: String): Flow<FileInfo?> = impl.observeDatum(FileCollection.Files.getName(), key)

    override suspend fun getDocument(collection: FileCollection, key: String): FileInfo? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: FileCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: FileCollection, item: FileInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomDownloadInfoStorage(
    val appDatabase: AppDatabase,
    private val collection: String = DownloadInfoStorage.COLLECTION_NAME,
) : DownloadInfoStorage {
    override fun forServer(server: String): DownloadInfoStorage =
        RoomDownloadInfoStorage(appDatabase, "public-resources-v1:$server")

    override suspend fun save(item: DownloadInfo) {
        val data = commonJson.encodeToString(item)
        appDatabase.getDownloadDao().insert(
            DownloadEntity(
                id = item.id.toString(),
                collection = collection,
                fileId = item.fileInfo.id,
                data = data,
            ),
        )
    }

    override suspend fun getDocumentByFileId(fileId: PrimaryKey): DownloadInfo? =
        appDatabase.getDownloadDao().getByFileId(
            collection,
            fileId,
        )
            ?.let {
                commonJson.safeDecodeFromStringOrNull(it.data)
            }

    override fun observeDocumentByFileId(fileId: PrimaryKey): Flow<DownloadInfo?> =
        appDatabase.getDownloadDao()
            .getByFileIdAsFlow(collection, fileId).map {
                it?.data?.let { string -> commonJson.safeDecodeFromStringOrNull(string) }
            }

    override fun observeData(): PagingSource<Int, DownloadInfo> =
        WrappedPagingSource(
            appDatabase.getDownloadDao().getAsSource(collection),
            ::getLocalRefreshKey,
        ) { entities ->
            entities.mapNotNull {
                commonJson.safeDecodeFromStringOrNull(it.data)
            }
        }

    override suspend fun clean() {
        appDatabase.getDownloadDao().clean(collection)
    }
}

class RoomUploadInfoStorage(val appDatabase: AppDatabase) : UploadInfoStorage {
    override suspend fun saveFirst(collection: UploadCollection, item: UploadInfo) {
        val data = commonJson.encodeToString(item)
        appDatabase.getUploadDao().insert(
            UploadEntity(item.id.toString(), collection.getName(), data, item.pathHash),
        )
    }

    override suspend fun saveLast(collection: UploadCollection, item: UploadInfo) {
        val data = commonJson.encodeToString(item)
        appDatabase.getUploadDao().insert(
            UploadEntity(item.id.toString(), collection.getName(), data, item.pathHash),
        )
    }

    override suspend fun getDocument(collection: UploadCollection, key: String): UploadInfo? =
        appDatabase.getUploadDao().get(
            collection.getName(),
            key,
        )?.let {
            commonJson.safeDecodeFromStringOrNull(it.data)
        }

    override fun observeData(collection: UploadCollection): PagingSource<Int, UploadInfo> =
        WrappedPagingSource(
            appDatabase.getUploadDao().getAsSource(collection.getName()),
            ::getLocalRefreshKey,
        ) { entities ->
            entities.mapNotNull {
                commonJson.safeDecodeFromStringOrNull(it.data)
            }
        }

    override suspend fun delete(collection: UploadCollection, key: String) {
        appDatabase.getUploadDao().delete(collection.getName(), key)
    }

    override suspend fun clean(collection: UploadCollection) {
        appDatabase.getUploadDao().clean(collection.getName())
    }

    override suspend fun updateDocument(collection: UploadCollection, item: UploadInfo) {
        val data = commonJson.encodeToString(item)
        appDatabase.getUploadDao().updateData(collection.getName(), item.id.toString(), data)
    }

    override fun observeDatumByHash(collection: UploadCollection, pathHash: String): Flow<UploadInfo?> =
        appDatabase.getUploadDao().getAsFlow(
            collection.getName(),
            pathHash,
        ).map {
            it?.data?.let { string -> commonJson.safeDecodeFromStringOrNull(string) }
        }
}

class RoomOverviewStorage(appDatabase: AppDatabase) : OverviewStorage {
    val impl = CommonStorageImpl(appDatabase)
    override suspend fun save(item: PanelOverview) {
        val data = commonJson.encodeToString(item)
        impl.save(CommonEntity("overview", OverviewStorage.COLLECTION_NAME, data))
    }

    override fun observeDatum(): Flow<PanelOverview?> = impl.observeDatum(OverviewStorage.COLLECTION_NAME, "overview")
}

class RoomUserOverviewStorage(appDatabase: AppDatabase) : UserOverviewStorage {
    val impl = CommonStorageImpl(appDatabase)
    override suspend fun save(item: UserOverview) {
        val data = commonJson.encodeToString(item)
        impl.save(CommonEntity("overview", UserOverviewStorage.COLLECTION_NAME, data))
    }

    override fun observeDatum(): Flow<UserOverview?> =
        impl.observeDatum(
            UserOverviewStorage.COLLECTION_NAME,
            "overview",
        )
}

class RoomUserFavoriteStorage(appDatabase: AppDatabase) : UserFavoriteStorage {
    val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveToDefault(item: UserFavoriteItem) {
        val data = commonJson.encodeToString(item)
        impl.save(CommonEntity(item.id, UserFavoriteStorage.COLLECTION_NAME, data))
    }

    override suspend fun saveLast(collection: UserFavoriteCollection, item: UserFavoriteItem) {
        val data = commonJson.encodeToString(item)
        impl.saveLast(CommonEntity(item.id, collection.getName(), data))
    }

    override suspend fun saveFirst(collection: UserFavoriteCollection, item: UserFavoriteItem) {
        val data = commonJson.encodeToString(item)
        impl.saveFirst(CommonEntity(item.id, collection.getName(), data))
    }

    override fun observeData(collection: UserFavoriteCollection): PagingSource<Int, UserFavoriteItem> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: UserFavoriteCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: UserFavoriteCollection, key: String): UserFavoriteItem? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override fun observeDatum(key: String): Flow<UserFavoriteItem?> =
        impl.observeDatum(
            UserFavoriteStorage.COLLECTION_NAME,
            key,
        )

    override suspend fun updateDocument(collection: UserFavoriteCollection, item: UserFavoriteItem) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }

    override suspend fun delete(collection: UserFavoriteCollection, key: String) {
        impl.delete(collection.getName(), key)
    }
}

class RoomUserSubscriptionStorage(appDatabase: AppDatabase) : UserSubscriptionStorage {
    val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveToDefault(item: UserSubscriptionItem) {
        val data = commonJson.encodeToString(item)
        impl.save(CommonEntity(item.id, UserSubscriptionStorage.COLLECTION_NAME, data))
    }

    override suspend fun saveLast(collection: UserSubscriptionCollection, item: UserSubscriptionItem) {
        val data = commonJson.encodeToString(item)
        impl.saveLast(CommonEntity(item.id, collection.getName(), data))
    }

    override suspend fun saveFirst(collection: UserSubscriptionCollection, item: UserSubscriptionItem) {
        val data = commonJson.encodeToString(item)
        impl.saveFirst(CommonEntity(item.id, collection.getName(), data))
    }

    override fun observeData(collection: UserSubscriptionCollection): PagingSource<Int, UserSubscriptionItem> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: UserSubscriptionCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: UserSubscriptionCollection, key: String): UserSubscriptionItem? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override fun observeDatum(key: String): Flow<UserSubscriptionItem?> =
        impl.observeDatum(
            UserSubscriptionStorage.COLLECTION_NAME,
            key,
        )

    override suspend fun updateDocument(collection: UserSubscriptionCollection, item: UserSubscriptionItem) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }

    override suspend fun delete(collection: UserSubscriptionCollection, key: String) {
        impl.delete(collection.getName(), key)
    }
}

class RoomUserReactionRecordStorage(appDatabase: AppDatabase) : UserReactionRecordStorage {
    val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveFirst(collection: UserReactionRecordCollection, item: ReactionRecordInfo) {
        val data = commonJson.encodeToString(item)
        impl.saveFirst(CommonEntity(item.id, collection.getName(), data))
    }

    override suspend fun saveLast(collection: UserReactionRecordCollection, item: ReactionRecordInfo) {
        val data = commonJson.encodeToString(item)
        impl.saveLast(CommonEntity(item.id, collection.getName(), data))
    }

    override fun observeData(collection: UserReactionRecordCollection): PagingSource<Int, ReactionRecordInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: UserReactionRecordCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: UserReactionRecordCollection, key: String): ReactionRecordInfo? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: UserReactionRecordCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: UserReactionRecordCollection, item: ReactionRecordInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomUserLogInfoStorage(appDatabase: AppDatabase) : UserLogInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveFirst(collection: UserLogCollection, item: UserLogItem) {
        impl.saveFirst(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override suspend fun saveLast(collection: UserLogCollection, item: UserLogItem) {
        impl.saveLast(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override fun observeData(collection: UserLogCollection): PagingSource<Int, UserLogItem> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: UserLogCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: UserLogCollection, key: String): UserLogItem? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: UserLogCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: UserLogCollection, item: UserLogItem) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomUploadRecordInfoStorage(appDatabase: AppDatabase) : UploadRecordInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveFirst(collection: UploadRecordCollection, item: UploadRecordInfo) {
        impl.saveFirst(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override suspend fun saveLast(collection: UploadRecordCollection, item: UploadRecordInfo) {
        impl.saveLast(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override fun observeData(collection: UploadRecordCollection): PagingSource<Int, UploadRecordInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: UploadRecordCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: UploadRecordCollection, key: String): UploadRecordInfo? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: UploadRecordCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: UploadRecordCollection, item: UploadRecordInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomFileRefInfoStorage(appDatabase: AppDatabase) : FileRefInfoStorage {
    private val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveFirst(collection: FileRefCollection, item: FileRefInfo) {
        impl.saveFirst(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override suspend fun saveLast(collection: FileRefCollection, item: FileRefInfo) {
        impl.saveLast(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override fun observeData(collection: FileRefCollection): PagingSource<Int, FileRefInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: FileRefCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: FileRefCollection, key: String): FileRefInfo? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: FileRefCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: FileRefCollection, item: FileRefInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomPanelLogInfoStorage(appDatabase: AppDatabase) : PanelLogInfoStorage {
    private val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveFirst(collection: PanelLogCollection, item: PanelLogInfo) {
        impl.saveFirst(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override suspend fun saveLast(collection: PanelLogCollection, item: PanelLogInfo) {
        impl.saveLast(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override fun observeData(collection: PanelLogCollection): PagingSource<Int, PanelLogInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: PanelLogCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: PanelLogCollection, key: String): PanelLogInfo? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: PanelLogCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: PanelLogCollection, item: PanelLogInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomTaskRecordInfoStorage(appDatabase: AppDatabase) : TaskRecordInfoStorage {
    private val impl = CommonStorageImpl(appDatabase)

    override suspend fun saveFirst(collection: TaskRecordCollection, item: TaskRecordInfo) {
        impl.saveFirst(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override suspend fun saveLast(collection: TaskRecordCollection, item: TaskRecordInfo) {
        impl.saveLast(CommonEntity(item.id, collection.getName(), commonJson.encodeToString(item)))
    }

    override fun observeData(collection: TaskRecordCollection): PagingSource<Int, TaskRecordInfo> =
        impl.observeData(
            collection.getName(),
        )

    override suspend fun clean(collection: TaskRecordCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun getDocument(collection: TaskRecordCollection, key: String): TaskRecordInfo? =
        impl.getDocument(collection.getName(), key)

    override suspend fun delete(collection: TaskRecordCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun updateDocument(collection: TaskRecordCollection, item: TaskRecordInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}

class RoomModelStorage(appDatabase: AppDatabase) : ModelStorage {
    override val user: UserInfoStorage = RoomUserInfoStorage(appDatabase)
    override val community: CommunityInfoStorage = RoomCommunityInfoStorage(appDatabase)
    override val topic: TopicInfoStorage = RoomTopicInfoStorage(appDatabase)
    override val title: TitleInfoStorage = RoomTitleInfoStorage(appDatabase)
    override val room: RoomInfoStorage = RoomRoomInfoStorage(appDatabase)
    override val member: MemberInfoStorage = RoomMemberInfoStorage(appDatabase)
    override val remoteKey: RemoteKeyStorage = RemoteKeyRoomStorage(appDatabase)
    override val reaction: ReactionInfoStorage = RoomReactionInfoStorage(appDatabase)
    override val childAccount: ChildAccountStorage = RoomChildAccountStorage(appDatabase)
    override val fileInfo: FileInfoStorage = RoomFileInfoStorage(appDatabase)
    override val download: DownloadInfoStorage = RoomDownloadInfoStorage(appDatabase)
    override val upload: UploadInfoStorage = RoomUploadInfoStorage(appDatabase)
    override val overview: OverviewStorage = RoomOverviewStorage(appDatabase)
    override val userOverview: UserOverviewStorage = RoomUserOverviewStorage(appDatabase)
    override val favorite: UserFavoriteStorage = RoomUserFavoriteStorage(appDatabase)
    override val subscription: UserSubscriptionStorage = RoomUserSubscriptionStorage(appDatabase)
    override val userReactionRecord: UserReactionRecordStorage = RoomUserReactionRecordStorage(appDatabase)
    override val userLog: UserLogInfoStorage = RoomUserLogInfoStorage(appDatabase)
    override val panelLog: PanelLogInfoStorage = RoomPanelLogInfoStorage(appDatabase)
    override val uploadRecord: UploadRecordInfoStorage = RoomUploadRecordInfoStorage(appDatabase)
    override val fileRef: FileRefInfoStorage = RoomFileRefInfoStorage(appDatabase)
    override val taskRecord: TaskRecordInfoStorage = RoomTaskRecordInfoStorage(appDatabase)
}

fun getRoomModelStorage(name: String) = RoomModelStorage(getRoomDatabase(name))

inline fun <reified T : Any> Json.safeDecodeFromStringOrNull(string: String): T? =
    try {
        decodeFromString(string)
    } catch (e: SerializationException) {
        Napier.e(e) {
            "Failed to decode cached model"
        }
        null
    }
