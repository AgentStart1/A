/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.paging.PagingSource
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.RoomInfoStorage
import com.storyteller_f.storage.getName
import kotlinx.coroutines.flow.Flow

class RoomRoomInfoStorage(appDatabase: AppDatabase) : RoomInfoStorage {
    val impl = CommonStorageImpl(appDatabase)

    private suspend fun saveToDefaultCollections(item: RoomInfo): CommonEntity {
        val data = commonJson.encodeToString(item)
        val entity = CommonEntity(item.id, RoomCollection.Rooms.getName(), data)
        listOf(
            entity,
            entity.copy(id = item.aid),
        ).forEach {
            impl.save(it)
        }
        return entity
    }

    override suspend fun saveToDefault(item: RoomInfo) {
        saveToDefaultCollections(item)
    }

    override suspend fun saveLast(collection: RoomCollection, item: RoomInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is RoomCollection.Rooms) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveLast(entity)
        }
    }

    override suspend fun saveFirst(collection: RoomCollection, item: RoomInfo) {
        val commonEntity = saveToDefaultCollections(item)
        if (collection !is RoomCollection.Rooms) {
            val entity = commonEntity.copy(collection = collection.getName())
            impl.saveFirst(entity)
        }
    }

    override suspend fun replace(collection: RoomCollection, items: List<RoomInfo>) {
        val details = items.map { CommonEntity(it.id, RoomCollection.Rooms.getName(), commonJson.encodeToString(it)) }
        val aliases = details.zip(items) { entity, room -> entity.copy(id = room.aid) }
        val entities =
            details.mapIndexed { index, entity ->
                entity.copy(collection = collection.getName(), seq = index.toLong())
            }
        impl.appDatabase.getCommonDao().replaceCollection(collection.getName(), entities, details + aliases)
    }

    override fun observeData(collection: RoomCollection): PagingSource<Int, RoomInfo> =
        impl.observeData(
            collection.getName(),
        )

    override fun observeDatum(key: String): Flow<RoomInfo?> = impl.observeDatum(RoomCollection.Rooms.getName(), key)

    override suspend fun getDocument(collection: RoomCollection, key: String): RoomInfo? =
        impl.getDocument(
            collection.getName(),
            key,
        )

    override suspend fun delete(collection: RoomCollection, key: String) {
        impl.delete(collection.getName(), key)
    }

    override suspend fun clean(collection: RoomCollection) {
        impl.clean(collection.getName())
    }

    override suspend fun updateDocument(collection: RoomCollection, item: RoomInfo) {
        val data = commonJson.encodeToString(item)
        impl.update(collection.getName(), item.id.toString(), data)
    }
}
