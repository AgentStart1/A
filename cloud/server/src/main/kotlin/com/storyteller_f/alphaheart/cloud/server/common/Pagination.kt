/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.common

import com.storyteller_f.alphaheart.api.PageableQuery
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.Fetch
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.ReactionFetch
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.obj.ListResponse
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.shared.obj.ReactionCursorKey
import com.storyteller_f.shared.type.PrimaryKey
import io.ktor.util.converters.DefaultConversionService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.reflect.KClass

interface PagingGenerator<in T, F : Any> {
    fun parse(prePageToken: String?, nextPageToken: String?, size: Int): F

    fun generate(list: List<T>, fetch: F): Pair<String?, String?>
}

open class PrimaryKeyPagingGenerator<T>(val block: (T) -> PrimaryKey) : PagingGenerator<T, PrimaryKeyFetch> {
    override fun parse(prePageToken: String?, nextPageToken: String?, size: Int): PrimaryKeyFetch =
        PrimaryKeyFetch(
            when {
                !nextPageToken.isNullOrBlank() ->
                    getPageToken(
                        PrimaryKey::class,
                        nextPageToken,
                    )?.let { Cursor.DescCursor(it) }

                !prePageToken.isNullOrBlank() ->
                    getPageToken(
                        PrimaryKey::class,
                        prePageToken,
                    )?.let {
                        Cursor.AscCursor(it)
                    }

                else -> null
            },
            size,
        )

    override fun generate(list: List<T>, fetch: PrimaryKeyFetch): Pair<String?, String?> {
        val size = fetch.size
        val next =
            if (size <= list.size) {
                block(list.last()).toString()
            } else {
                null
            }
        val pre =
            if (list.isNotEmpty()) {
                block(list.first()).toString()
            } else {
                null
            }
        return pre to next
    }
}

object IdentifiablePagingGenerator :
    PrimaryKeyPagingGenerator<PrimaryKeyIdentifiable>(PrimaryKeyIdentifiable::id)

fun <T> pagingGenerator(block: (T) -> PrimaryKey): PrimaryKeyPagingGenerator<T> = PrimaryKeyPagingGenerator(block)

open class OffsetPagingGenerator<T> : PagingGenerator<T, OffsetFetch> {
    override fun parse(prePageToken: String?, nextPageToken: String?, size: Int): OffsetFetch {
        val cursor =
            when {
                !nextPageToken.isNullOrBlank() -> nextPageToken.toIntOrNull()?.let { Cursor.DescCursor(it) }
                !prePageToken.isNullOrBlank() -> prePageToken.toIntOrNull()?.let { Cursor.AscCursor(it) }
                else -> null
            }
        return OffsetFetch(cursor, size)
    }

    override fun generate(list: List<T>, fetch: OffsetFetch): Pair<String?, String?> {
        val currentOffset = fetch.cursor?.value ?: 0
        val size = fetch.size
        val next =
            if (size <= list.size) {
                (currentOffset + size).toString()
            } else {
                null
            }
        val pre =
            if (currentOffset > 0) {
                maxOf(0, currentOffset - size).toString()
            } else {
                null
            }
        return pre to next
    }
}

object GeneralOffsetPagingGenerator : OffsetPagingGenerator<Any>()

suspend fun <T, F : Fetch, Response : ListResponse<T>> PageableQuery.pagination(
    generator: PagingGenerator<T, F>,
    responseBuilder: (ImmutableList<T>, Pagination<String>) -> Response,
    block: suspend (F) -> CustomResult<PaginationResult<T>?, FailureDetails>,
): CustomResult<Response?, ServerError> =
    captureFailure(ServerError::from) {
        val size = size
        require(size > 0) {
            "Invalid query size"
        }
        val nextPageToken = nextPageToken
        val prePageToken = prePageToken

        require(nextPageToken.isNullOrBlank() || prePageToken.isNullOrBlank()) {
            "Invalid query"
        }
        generator.parse(prePageToken, nextPageToken, size)
    }.andThen(ServerError::fromError) { f ->
        block(f).andThenPresent(ServerError::fromError) { page ->
            captureFailure(ServerError::from) {
                val (list, count) = page
                val (pre, next) = generator.generate(list, f)
                responseBuilder(list.toImmutableList(), Pagination(next, pre, count))
            }
        }
    }

@Suppress("UNCHECKED_CAST")
fun <R : Any> getPageToken(pageTokenType: KClass<R>, pageToken: String): R? =
    if (pageTokenType == ULong::class) {
        pageToken.toULongOrNull() as? R
    } else {
        DefaultConversionService.fromValue(pageToken, pageTokenType) as? R
    }

class ReactionPaginationGenerator(val backend: Backend) : PagingGenerator<ReactionInfo, ReactionFetch> {
    override fun parse(prePageToken: String?, nextPageToken: String?, size: Int): ReactionFetch =
        ReactionFetch(
            when {
                !nextPageToken.isNullOrBlank() ->
                    Cursor.DescCursor(
                        commonJson.decodeFromString<ReactionCursorKey>(nextPageToken),
                    )

                !prePageToken.isNullOrBlank() ->
                    Cursor.AscCursor(
                        commonJson.decodeFromString<ReactionCursorKey>(prePageToken),
                    )

                else -> null
            },
            size,
        )

    override fun generate(list: List<ReactionInfo>, fetch: ReactionFetch): Pair<String?, String?> {
        val size = fetch.size
        val next =
            if (size <= list.size) {
                val last = list.last()
                commonJson.encodeToString(ReactionCursorKey(last.count, last.lastReactionId))
            } else {
                null
            }
        val pre =
            if (list.isNotEmpty()) {
                val first = list.first()
                commonJson.encodeToString(ReactionCursorKey(first.count, first.lastReactionId))
            } else {
                null
            }
        return pre to next
    }
}
