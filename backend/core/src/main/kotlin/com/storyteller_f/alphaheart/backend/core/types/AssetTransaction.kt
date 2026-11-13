/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.AssetType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class AssetTransaction(
    val id: PrimaryKey,
    val uid: PrimaryKey,
    val createdTime: LocalDateTime,
    val type: AssetType,
    val before: Long,
    val after: Long,
) {
    companion object
}
