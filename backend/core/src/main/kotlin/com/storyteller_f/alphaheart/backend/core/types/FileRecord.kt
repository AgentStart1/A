/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class FileRecord(
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
    val name: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val owner: PrimaryKey,
    val ownerType: ObjectType,
    val contentType: String,
    val size: Long,
    val fullName: String,
    val sha256: String,
    val status: ObjectStatus = ObjectStatus.NORMAL,
) {
    val readOnly get() = status == ObjectStatus.READ_ONLY
    val dimension = Dimension(width, height)

    companion object
}

fun FileRecord.toFileInfo(
    url: String,
    lastModified: LocalDateTime,
    favoriteId: PrimaryKey? = null,
    subscriptionId: PrimaryKey? = null,
): FileInfo =
    FileInfo(
        id,
        url,
        fullName,
        contentType,
        size,
        name,
        owner,
        ownerType,
        lastModified,
        dimension,
        favoriteId,
        subscriptionId,
        status,
    )
