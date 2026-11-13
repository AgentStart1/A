/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.query

import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.andWhere

fun Query.bindPaginationQuery(table: BaseTable, primaryKeyFetch: PrimaryKeyFetch): Query {
    val order =
        when (val cursor = primaryKeyFetch.cursor) {
            is Cursor.DescCursor<PrimaryKey> -> {
                andWhere {
                    table.id less cursor.value
                }
                SortOrder.DESC
            }

            is Cursor.AscCursor<PrimaryKey> -> {
                andWhere {
                    table.id greater cursor.value
                }
                SortOrder.ASC
            }

            null -> null
        }
    return orderBy(table.id, order ?: SortOrder.DESC).limit(primaryKeyFetch.size)
}
