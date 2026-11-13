/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.storyteller_f.shared.commonJson
import com.storyteller_f.storage.WrappedPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CommonStorageImpl(val appDatabase: AppDatabase) {
    inline fun <reified T : Any> observeData(collection: String): PagingSource<Int, T> {
        val source = appDatabase.getCommonDao().getAsSource(collection)
        return WrappedPagingSource(source, ::getLocalRefreshKey) { list ->
            list.mapNotNull {
                commonJson.safeDecodeFromStringOrNull(it.data)
            }
        }
    }

    inline fun <reified T : Any> observeDatum(collection: String, id: String): Flow<T?> {
        val source = appDatabase.getCommonDao().getAsFlow(collection, id)
        return source.map {
            it?.data?.let { string -> commonJson.safeDecodeFromStringOrNull(string) }
        }
    }

    suspend fun clean(collection: String) {
        appDatabase.getCommonDao().clean(collection)
    }

    suspend inline fun <reified T> getDocument(collection: String, id: String): T? {
        val entity = appDatabase.getCommonDao().get(collection, id)
        return entity?.data?.let { commonJson.safeDecodeFromStringOrNull(it) }
    }

    suspend fun saveLast(entity: CommonEntity) {
        appDatabase.getCommonDao().saveLast(entity)
    }

    suspend fun saveFirst(entity: CommonEntity) {
        appDatabase.getCommonDao().saveFirst(entity)
    }

    suspend fun delete(collection: String, key: String) {
        appDatabase.getCommonDao().delete(collection, key)
    }

    suspend fun save(entity: CommonEntity) {
        appDatabase.getCommonDao().insert(entity)
    }

    suspend fun update(collection: String, id: String, data: String) {
        appDatabase.getCommonDao().updateData(collection, id, data)
    }
}

// Match Room's offset paging policy without deriving keys from business IDs or remote tokens.
@PublishedApi
internal fun <T : Any> getLocalRefreshKey(state: PagingState<Int, T>): Int? =
    state.anchorPosition?.let { (it - state.config.initialLoadSize / 2).coerceAtLeast(0) }
