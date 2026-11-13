/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.Quota
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import com.storyteller_f.alphaheart.backend.exposed.quotaType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table

object Quotas : Table() {
    val ownerId = customPrimaryKey("owner_id")
    val ownerType = objectType("owner_type")
    val quotaType = quotaType("quota_type")
    val used = long("used")
    val total = long("total")
    val lockId = customPrimaryKey("lock_id").nullable()
    override val primaryKey = PrimaryKey(ownerId, quotaType)
}

fun Quota.Companion.wrapRow(resultRow: ResultRow): Quota =
    Quota(
        resultRow[Quotas.ownerId],
        resultRow[Quotas.ownerType],
        resultRow[Quotas.total],
        resultRow[Quotas.used],
        resultRow[Quotas.quotaType],
        resultRow[Quotas.lockId],
    )
