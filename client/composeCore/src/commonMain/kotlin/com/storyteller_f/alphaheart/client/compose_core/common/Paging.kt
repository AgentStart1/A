/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.common

import androidx.lifecycle.ViewModel
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.fold
import com.storyteller_f.result.map
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.ListResponse
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.storage.CollectionListStorage
import com.storyteller_f.storage.GlobalListStorage
import com.storyteller_f.storage.RemoteKeyStorageWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

abstract class PagingViewModel<V : Any> : ViewModel() {
    abstract val flow: Flow<PagingData<V>>
}

data class APagingData<K, T>(val data: List<T>, val pagination: K?)

@Serializable
data class SectionLoadParams(val index: Int, val param: String?)

class SectionPagingSource<DATUM : Any>(private val services: List<RegularPagingSource<DATUM>>) :
    PagingSource<SectionLoadParams, DATUM>() {
    init {
        services.forEach {
            it.registerInvalidatedCallback {
                invalidate()
            }
        }
    }

    override suspend fun load(params: LoadParams<SectionLoadParams>): LoadResult<SectionLoadParams, DATUM> {
        val index = params.key?.index ?: 0
        val key = params.key?.param
        return load(index, key, params.loadSize, params.placeholdersEnabled)
    }

    private suspend fun load(
        index: Int,
        key: String?,
        loadSize: Int,
        placeholdersEnabled: Boolean,
    ): LoadResult<SectionLoadParams, DATUM> {
        val service =
            services.getOrNull(index) ?: return LoadResult.Page(
                data = emptyList(),
                prevKey = null, // Only paging forward.
                nextKey = null,
            )
        val pageResult =
            service.load(
                if (key == null) {
                    LoadParams.Refresh(null, loadSize, placeholdersEnabled)
                } else {
                    LoadParams.Append(key, loadSize, placeholdersEnabled)
                },
            )
        return when (pageResult) {
            is LoadResult.Error<String, DATUM> -> LoadResult.Error(pageResult.throwable)
            is LoadResult.Invalid<String, DATUM> -> LoadResult.Invalid()
            is LoadResult.Page<String, DATUM> -> load(loadSize, pageResult, index, placeholdersEnabled)
        }
    }

    private suspend fun load(
        loadSize: Int,
        prePageResult: LoadResult.Page<String, DATUM>,
        index: Int,
        placeholdersEnabled: Boolean,
    ): LoadResult<SectionLoadParams, DATUM> =
        if (loadSize - prePageResult.data.size > 0) {
            getNextParams(prePageResult.nextKey, index)?.let { nextParams ->
                when (
                    val nextPageResult =
                        load(nextParams.index, nextParams.param, loadSize, placeholdersEnabled)
                ) {
                    is LoadResult.Error<SectionLoadParams, DATUM> ->
                        LoadResult.Error(nextPageResult.throwable)

                    is LoadResult.Invalid<SectionLoadParams, DATUM> ->
                        LoadResult.Invalid()

                    is LoadResult.Page<SectionLoadParams, DATUM> -> {
                        // 合并两页
                        LoadResult.Page(prePageResult.data + nextPageResult.data, null, nextPageResult.nextKey)
                    }
                }
            } ?: LoadResult.Page(
                prePageResult.data,
                null,
                getNextParams(prePageResult.nextKey, index),
            )
        } else {
            LoadResult.Page(prePageResult.data, null, getNextParams(prePageResult.nextKey, index))
        }

    private fun getNextParams(nextToken: String?, index: Int): SectionLoadParams? =
        if (nextToken != null) {
            SectionLoadParams(index, nextToken)
        } else {
            val newIndex = index + 1
            if (newIndex >= services.size) {
                null
            } else {
                SectionLoadParams(newIndex, null)
            }
        }

    override fun getRefreshKey(state: PagingState<SectionLoadParams, DATUM>) = null
}

class RegularPagingSource<DATUM : Any>(
    val service: suspend (String?, Int) -> CustomResult<ListResponse<DATUM>, FailureDetails>,
) : PagingSource<String, DATUM>() {
    override suspend fun load(params: LoadParams<String>): LoadResult<String, DATUM> =
        service(
            params.key,
            params.loadSize,
        ).map { response ->
            val pagination = response.pagination
            APagingData(
                response.data,
                Pagination(pagination?.nextPageToken, pagination?.prePageToken, pagination?.total ?: 0),
            )
        }.fold(onSuccess = { (data, pagination) ->
            LoadResult.Page(data = data, prevKey = null, nextKey = pagination?.nextPageToken)
        }, onFailure = {
            LoadResult.Error(it.cause)
        })

    override fun getRefreshKey(state: PagingState<String, DATUM>): String? = null
}

@OptIn(ExperimentalPagingApi::class)
fun <C : Any, T : Any> buildPager(
    collection: C,
    wrapper: RemoteKeyStorageWrapper,
    storage: CollectionListStorage<C, T>,
    onUpdated: suspend () -> Unit = {},
    service: suspend (String?, Int) -> CustomResult<ListResponse<T>, FailureDetails>,
): Pager<String, T> =
    buildCollectionPager(
        collection,
        wrapper,
        storage,
        RegularPagingSource(service),
        onUpdated,
    )

@OptIn(ExperimentalPagingApi::class)
fun <C : Any, T : Any> buildPager(
    collection: C,
    wrapper: RemoteKeyStorageWrapper,
    storage: CollectionListStorage<C, T>,
    source: PagingSource<String, T>,
): Pager<String, T> = buildCollectionPager(collection, wrapper, storage, source)

@OptIn(ExperimentalPagingApi::class)
private fun <C : Any, T : Any> buildCollectionPager(
    collection: C,
    wrapper: RemoteKeyStorageWrapper,
    storage: CollectionListStorage<C, T>,
    source: PagingSource<String, T>,
    onUpdated: suspend () -> Unit = {},
): Pager<String, T> =
    Pager(
        PagingConfig(pageSize = 20),
        remoteMediator =
        CustomRemoteMediator(wrapper, source) { data, loadType ->
            updateCollectionPage(collection, storage, data, loadType)
            onUpdated()
        },
    ) {
        CompatPagingSource(storage.observeData(collection), IntKeyConverter)
    }

@OptIn(ExperimentalPagingApi::class)
fun <C : Any, T : Any> buildPager(
    collection: C,
    wrapper: RemoteKeyStorageWrapper,
    storage: CollectionListStorage<C, T>,
    networkSource: PagingSource<String, T>,
    localSourceFactory: () -> PagingSource<String, T>,
): Pager<String, T> =
    Pager(
        PagingConfig(pageSize = 20),
        remoteMediator =
        CustomRemoteMediator(wrapper, networkSource) { data, loadType ->
            updateCollectionPage(collection, storage, data, loadType)
        },
        pagingSourceFactory = localSourceFactory,
    )

@OptIn(ExperimentalPagingApi::class)
fun <T : Any> buildPager(
    wrapper: RemoteKeyStorageWrapper,
    storage: GlobalListStorage<T>,
    service: suspend (String?, Int) -> CustomResult<ListResponse<T>, FailureDetails>,
): Pager<String, T> =
    buildPager(wrapper, storage, RegularPagingSource(service)) {
        CompatPagingSource(storage.observeData(), IntKeyConverter)
    }

@OptIn(ExperimentalPagingApi::class)
fun <T : Any> buildPager(
    wrapper: RemoteKeyStorageWrapper,
    storage: GlobalListStorage<T>,
    networkSource: PagingSource<String, T>,
    localSourceFactory: () -> PagingSource<String, T>,
): Pager<String, T> =
    Pager(
        PagingConfig(pageSize = 20),
        remoteMediator =
        CustomRemoteMediator(wrapper, networkSource) { data, loadType ->
            if (loadType == LoadType.REFRESH) {
                storage.clean()
            }
            data.forEach {
                storage.save(it)
            }
        },
        pagingSourceFactory = localSourceFactory,
    )

private suspend fun <C : Any, T : Any> updateCollectionPage(
    collection: C,
    storage: CollectionListStorage<C, T>,
    data: List<T>,
    loadType: LoadType,
) {
    if (loadType == LoadType.REFRESH) {
        storage.replace(collection, data)
    } else {
        data.forEach {
            if (loadType == LoadType.PREPEND) storage.saveFirst(collection, it) else storage.saveLast(collection, it)
        }
    }
}
