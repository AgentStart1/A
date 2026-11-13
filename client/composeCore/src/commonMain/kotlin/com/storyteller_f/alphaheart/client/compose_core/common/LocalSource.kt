/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.common

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.request
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.shared.commonJson
import com.storyteller_f.storage.RemoteKeyStorageWrapper
import com.storyteller_f.storage.RemoteKeys
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

class CachedLoadingHandler<T : Any>(
    flow: Flow<T?>,
    private val scope: CoroutineScope,
    private val onSave: suspend (T) -> Unit,
    private val loader: suspend () -> CustomResult<T, FailureDetails>,
) : LoadingHandler<T> {
    override val state: MutableStateFlow<LoadingState?> = MutableStateFlow(null)

    @OptIn(FlowPreview::class)
    override val data = flow.stateIn(scope, SharingStarted.Lazily, null)

    init {
        refresh()
    }

    override fun refresh() {
        scope.launch {
            request({
                onSave(it)
            }) {
                loader()
            }
        }
    }
}

@Suppress("unused")
@OptIn(ExperimentalPagingApi::class)
class CustomRemoteMediator<Datum : Any>(
    private val wrapper: RemoteKeyStorageWrapper,
    private val networkService: PagingSource<String, Datum>,
    private val update: suspend (List<Datum>, LoadType) -> Unit,
) : RemoteMediator<String, Datum>() {
    override suspend fun load(loadType: LoadType, state: PagingState<String, Datum>): MediatorResult {
        Napier.v(tag = "pagination") {
            "mediator load $loadType"
        }
        return try {
            val params =
                when (loadType) {
                    LoadType.REFRESH ->
                        return applyResult(loadRefreshWindow(state), loadType)

                    LoadType.PREPEND -> {
                        val remoteKey = wrapper.getPreRemoteKey()?.key
                        PagingSource.LoadParams.Append(
                            remoteKey
                                ?: return MediatorResult.Success(endOfPaginationReached = true),
                            state.config.pageSize,
                            state.config.enablePlaceholders,
                        )
                    }

                    LoadType.APPEND -> {
                        val remoteKey = wrapper.getNextRemoteKey()?.key
                        PagingSource.LoadParams.Append(
                            remoteKey
                                ?: return MediatorResult.Success(endOfPaginationReached = true),
                            state.config.pageSize,
                            state.config.enablePlaceholders,
                        )
                    }
                }
            applyResult(networkService.load(params), loadType)
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Napier.e(e, tag = "pagination") {
                "mediator load error"
            }
            MediatorResult.Error(e)
        }
    }

    /** Reload the cached range before replacing it; API tokens are passed through unchanged. */
    private suspend fun loadRefreshWindow(state: PagingState<String, Datum>): PagingSource.LoadResult<String, Datum> {
        val cachedSize =
            state.pages.sumOf { it.data.size } +
                (state.pages.firstOrNull()?.itemsBefore ?: 0).coerceAtLeast(0) +
                (state.pages.lastOrNull()?.itemsAfter ?: 0).coerceAtLeast(0)
        val targetSize =
            maxOf(
                state.config.initialLoadSize,
                cachedSize,
                (state.anchorPosition ?: 0) + 1,
            )
        val data = mutableListOf<Datum>()
        val tokens = mutableSetOf<String>()
        var params: PagingSource.LoadParams<String> =
            PagingSource.LoadParams.Refresh(
                null,
                state.config.pageSize,
                state.config.enablePlaceholders,
            )
        while (true) {
            when (val page = networkService.load(params)) {
                is PagingSource.LoadResult.Error -> return page

                is PagingSource.LoadResult.Invalid -> return page

                is PagingSource.LoadResult.Page -> {
                    data.addAll(page.data)
                    val next = page.nextKey
                    if (data.size >= targetSize || next == null) {
                        return PagingSource.LoadResult.Page(data.toList(), null, next)
                    }
                    check(tokens.add(next)) { "Repeated pagination token" }
                    params =
                        PagingSource.LoadParams.Append(
                            next,
                            state.config.pageSize,
                            state.config.enablePlaceholders,
                        )
                }
            }
        }
    }

    private suspend fun applyResult(loadResult: PagingSource.LoadResult<String, Datum>, loadType: LoadType) =
        when (loadResult) {
            is PagingSource.LoadResult.Error<String, Datum> -> {
                MediatorResult.Error(loadResult.throwable)
            }

            is PagingSource.LoadResult.Invalid<String, Datum> -> {
                MediatorResult.Error(Exception("invalid"))
            }

            is PagingSource.LoadResult.Page<String, Datum> -> {
                val data = loadResult.data
                val nextKey = loadResult.nextKey
                val preKey = loadResult.prevKey
                update(data, loadType)
                if (loadType != LoadType.APPEND) {
                    wrapper.savePreRemoteKey(RemoteKeys(wrapper.collectionName, preKey))
                }
                if (loadType != LoadType.PREPEND) {
                    wrapper.saveNextRemoteKey(RemoteKeys(wrapper.collectionName, nextKey))
                }
                Napier.v(tag = "pagination") {
                    "mediator success type: $loadType"
                }
                MediatorResult.Success(endOfPaginationReached = nextKey == null)
            }
        }
}

class IntermediatePagingSource<Key : Any, T : Any>(val clazz: KClass<Key>, val pagingSource: PagingSource<Key, T>) :
    PagingSource<String, T>() {
    override fun getRefreshKey(state: PagingState<String, T>): String? = null

    @OptIn(InternalSerializationApi::class)
    override suspend fun load(params: LoadParams<String>): LoadResult<String, T> {
        val serializer = clazz.serializer()
        val loadParams: LoadParams<Key> =
            when (params) {
                is LoadParams.Append<*> -> {
                    val key =
                        params.key.let {
                            commonJson.decodeFromString(serializer, it)
                        }
                    LoadParams.Append(key, params.loadSize, params.placeholdersEnabled)
                }

                is LoadParams.Prepend<*> -> {
                    val key =
                        params.key.let {
                            commonJson.decodeFromString(serializer, it)
                        }
                    LoadParams.Prepend(key, params.loadSize, params.placeholdersEnabled)
                }

                is LoadParams.Refresh<*> -> LoadParams.Refresh(null, params.loadSize, params.placeholdersEnabled)
            }
        return when (val load = pagingSource.load(loadParams)) {
            is LoadResult.Error<Key, T> -> LoadResult.Error(load.throwable)

            is LoadResult.Invalid<Key, T> -> LoadResult.Invalid()

            is LoadResult.Page<Key, T> -> {
                val preKey = load.prevKey?.let { commonJson.encodeToString(serializer, it) }
                val nextKey = load.nextKey?.let { commonJson.encodeToString(serializer, it) }
                LoadResult.Page(load.data, preKey, nextKey)
            }
        }
    }
}
