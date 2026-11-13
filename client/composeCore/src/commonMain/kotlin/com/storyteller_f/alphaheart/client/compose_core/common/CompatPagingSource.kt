/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.common

import androidx.paging.PagingSource
import androidx.paging.PagingState

interface CompatKeyConverter<F, T> {
    fun from(f: F): T
    fun to(t: T): F
}

/** Converts local database offsets only; remote API tokens never pass through this converter. */
object IntKeyConverter : CompatKeyConverter<Int, String> {
    override fun from(f: Int): String = f.toString()

    override fun to(t: String): Int = t.toInt()
}

class CompatPagingSource<OK : Any, TK : Any, T : Any>(
    val rawSource: PagingSource<OK, T>,
    val converter: CompatKeyConverter<OK, TK>,
) : PagingSource<TK, T>() {
    init {
        rawSource.registerInvalidatedCallback {
            invalidate()
        }
    }

    override fun getRefreshKey(state: PagingState<TK, T>): TK? =
        rawSource.getRefreshKey(
            PagingState(
                state.pages.map { page ->
                    LoadResult.Page(
                        page.data,
                        page.prevKey?.let(converter::to),
                        page.nextKey?.let(converter::to),
                        page.itemsBefore,
                        page.itemsAfter,
                    )
                },
                state.anchorPosition,
                state.config,
                if (state.config.enablePlaceholders) {
                    (state.pages.firstOrNull()?.itemsBefore ?: 0).coerceAtLeast(
                        0,
                    )
                } else {
                    0
                },
            ),
        )?.let(converter::from)

    override suspend fun load(params: LoadParams<TK>): LoadResult<TK, T> {
        val loadResult =
            rawSource.load(
                when (params) {
                    is LoadParams.Append<TK> ->
                        LoadParams.Append(
                            converter.to(params.key),
                            params.loadSize,
                            params.placeholdersEnabled,
                        )

                    is LoadParams.Prepend<TK> ->
                        LoadParams.Prepend(
                            converter.to(params.key),
                            params.loadSize,
                            params.placeholdersEnabled,
                        )

                    is LoadParams.Refresh<TK> ->
                        LoadParams.Refresh(
                            params.key?.let { converter.to(it) },
                            params.loadSize,
                            params.placeholdersEnabled,
                        )
                },
            )
        return when (loadResult) {
            is LoadResult.Error<OK, T> -> LoadResult.Error(loadResult.throwable)

            is LoadResult.Invalid<OK, T> -> LoadResult.Invalid()

            is LoadResult.Page<OK, T> ->
                LoadResult.Page(
                    loadResult.data,
                    loadResult.prevKey?.let { converter.from(it) },
                    loadResult.nextKey?.let { converter.from(it) },
                    loadResult.itemsBefore,
                    loadResult.itemsAfter,
                )
        }
    }
}
