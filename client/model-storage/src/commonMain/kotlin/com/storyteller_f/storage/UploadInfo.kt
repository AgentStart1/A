/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.storage

import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.Serializable

enum class UploadStatus {
    NOT_UPLOADING,
    UPLOADING,
    PAUSED,
    FAILED,
    SUCCESS,
}

@Serializable
data class UploadInfo(
    val id: PrimaryKey,
    val objectId: PrimaryKey,
    val pathHash: String,
    val path: String,
    val sha256: String,
    val crc32c: String,
    val progress: Long,
    val chunkProgress: Long,
    val total: Long,
    val status: UploadStatus,
    val message: String,
    val name: String,
    val contentType: String,
    val chunkSize: Long,
    // 对应的上传记录 id
    val recordId: PrimaryKey? = null,
    val objectType: ObjectType = ObjectType.USER,
) {
    val owner: ObjectTuple get() = objectId ob objectType

    companion object {
        /** Empty upload state used before an upload begins. */
        val EMPTY: UploadInfo = UploadInfo(0, 0, "", "", "", "", 0, 0, 0, UploadStatus.NOT_UPLOADING, "", "", "", 0)
    }
}
