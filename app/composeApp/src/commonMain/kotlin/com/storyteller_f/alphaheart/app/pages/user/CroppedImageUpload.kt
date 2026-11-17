/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.shared.utils.sha256
import io.ktor.http.ContentType
import kotlinx.io.Buffer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MAX_CROPPED_FILE_NAME_LENGTH = 200

/** Each confirmed export is a new file; a failed upload never silently changes its identity or retries. */
@OptIn(ExperimentalUuidApi::class)
internal suspend fun prepareCroppedImageUpload(sourceName: String, bytes: ByteArray): UploadData {
    val name = croppedImageUploadName(sourceName, Uuid.random())
    val hash = Buffer().also { it.write(bytes) }.use { sha256(it) }
    return UploadData(bytes.size.toLong(), name, ContentType.Image.PNG, hash) {
        Buffer().also { it.write(bytes) }
    }
}

@OptIn(ExperimentalUuidApi::class)
internal fun croppedImageUploadName(sourceName: String, exportId: Uuid): String {
    val suffix = "-cropped-$exportId.png"
    val stem = sourceName.substringBeforeLast('.').ifBlank { "image" }
    return stem.take(MAX_CROPPED_FILE_NAME_LENGTH - suffix.length) + suffix
}
