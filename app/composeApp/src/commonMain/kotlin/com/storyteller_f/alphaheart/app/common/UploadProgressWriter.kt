/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

private const val PROGRESS_WRITE_STEP = 256L * 1024

/** Coalesces small transport buffers without losing the exact end of each request. */
internal class UploadProgressWriter(private val total: Long, private val write: suspend (Long) -> Unit) {
    private var saved = 0L

    suspend fun update(sent: Long) {
        val progress = sent.coerceIn(0L, total)
        if (progress > saved && (progress == total || progress - saved >= PROGRESS_WRITE_STEP)) {
            write(progress)
            saved = progress
        }
    }
}
