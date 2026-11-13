/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.FileRefInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class FileRef(
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val author: PrimaryKey,
    val mediaName: String,
    val fileId: PrimaryKey,
) {
    companion object
}

fun FileRef.toFileRefInfo() =
    FileRefInfo(
        id = id,
        objectId = objectId,
        objectType = objectType,
        author = author,
        mediaName = mediaName,
        fileId = fileId,
    )
