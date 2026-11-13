/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.Table

object Aids : Table() {
    val value = varchar("value", 100).uniqueIndex()
    val objectId = customPrimaryKey("object_id").uniqueIndex()
    val objectType = objectType("object_type")

    init {
        index("id-value", true, objectId, value)
    }
}
