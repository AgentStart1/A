/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.AssetTransaction
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.assetType
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import org.jetbrains.exposed.v1.core.ResultRow

object AssetTransactions : BaseTable() {
    val type = assetType("type")
    val before = long("before")
    val after = long("after")
    val uid = customPrimaryKey("uid")
}

fun AssetTransaction.Companion.wrapRow(row: ResultRow): AssetTransaction =
    with(AssetTransactions) {
        AssetTransaction(row[id], row[uid], row[createdTime], row[type], row[before], row[after])
    }
