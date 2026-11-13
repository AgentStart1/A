package com.storyteller_f.alphaheart.client.sqlitenow

import androidx.paging.PagingSource
import com.storyteller_f.alphaheart.client.sqlitenow.db.AppDatabase
import com.storyteller_f.alphaheart.client.sqlitenow.db.CommonEntityQuery
import com.storyteller_f.shared.commonJson
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking

class CommonStorageImpl(val appDatabase: AppDatabase) {
    @PublishedApi
    internal fun ensureOpen() {
        try {
            appDatabase.connection()
        } catch (e: IllegalStateException) {
            runBlocking {
                appDatabase.open()
            }
        }
    }

    inline fun <reified T : Any> observeData(
        collection: String,
    ): PagingSource<Int, T> {
        return OffsetPagingSource(
            queryFlow = appDatabase.commonEntity.getPaged(
                CommonEntityQuery.GetPaged.Params(
                    collection = collection,
                    limit = 1,
                    offset = 0
                )
            ).asFlow().onStart {
                ensureOpen()
            }
        ) { limit, offset ->
            ensureOpen()
            val rows = appDatabase.commonEntity.getPaged(
                CommonEntityQuery.GetPaged.Params(
                    collection = collection,
                    limit = limit,
                    offset = offset
                )
            ).asList()
            rows.mapNotNull {
                try {
                    commonJson.decodeFromString<T>(it.data)
                } catch (e: Exception) {
                    Napier.e(e) { "Error decoding JSON" }
                    null
                }
            }
        }
    }

    inline fun <reified T : Any> observeDatum(
        collection: String,
        id: String
    ): Flow<T?> {
        val flow = appDatabase.commonEntity.getById(
            CommonEntityQuery.GetById.Params(collection = collection, id = id)
        ).asFlow().onStart {
            ensureOpen()
        }
        return flow.map { list ->
            list.firstOrNull()?.data?.let { string ->
                try {
                    commonJson.decodeFromString<T>(string)
                } catch (e: Exception) {
                    Napier.e(e) { "Error decoding JSON" }
                    null
                }
            }
        }
    }

    suspend fun clean(collection: String) {
        ensureOpen()
        appDatabase.commonEntity.clean(CommonEntityQuery.Clean.Params(collection = collection))
    }

    suspend inline fun <reified T> getDocument(
        collection: String,
        id: String
    ): T? {
        ensureOpen()
        val row = appDatabase.commonEntity.getById(
            CommonEntityQuery.GetById.Params(collection = collection, id = id)
        ).asOneOrNull()
        return row?.data?.let {
            try {
                commonJson.decodeFromString<T>(it)
            } catch (e: Exception) {
                Napier.e(e) { "Error decoding JSON" }
                null
            }
        }
    }

    suspend fun saveLast(collection: String, id: String, data: String) {
        save(collection, id, data)
    }

    suspend fun saveFirst(collection: String, id: String, data: String) {
        save(collection, id, data)
    }

    suspend fun delete(collection: String, key: String) {
        ensureOpen()
        appDatabase.commonEntity.delete(CommonEntityQuery.Delete.Params(collection = collection, id = key))
    }

    suspend fun save(collection: String, id: String, data: String) {
        ensureOpen()
        appDatabase.commonEntity.insert(CommonEntityQuery.Insert.Params(collection = collection, id = id, data = data))
    }

    suspend fun update(collection: String, id: String, data: String) {
        save(collection, id, data) // Insert acts as upsert with ON CONFLICT DO UPDATE
    }
}
