package com.storyteller_f.alphaheart.client.sqlitenow

import androidx.paging.PagingSource
import androidx.paging.PagingState

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

class OffsetPagingSource<T : Any>(
    queryFlow: Flow<Any>? = null,
    private val loadData: suspend (limit: Long, offset: Long) -> List<T>
) : PagingSource<Int, T>() {

    init {
        if (queryFlow != null) {
            @OptIn(DelicateCoroutinesApi::class)
            val job: Job = GlobalScope.launch {
                queryFlow.drop(1).collect {
                    invalidate()
                }
            }
            registerInvalidatedCallback {
                job.cancel()
            }
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val pageNumber = params.key ?: 0
        val pageSize = params.loadSize
        val offset = pageNumber * pageSize

        return try {
            val data = loadData(pageSize.toLong(), offset.toLong())
            val prevKey = if (pageNumber > 0) pageNumber - 1 else null
            val nextKey = if (data.isNotEmpty()) pageNumber + 1 else null
            LoadResult.Page(
                data = data,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
