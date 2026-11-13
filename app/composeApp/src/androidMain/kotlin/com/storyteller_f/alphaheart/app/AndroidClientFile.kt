/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.storyteller_f.alphaheart.app.utils.ClientFile
import io.ktor.http.ContentType
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.BufferedInputStream

fun getClipFile(context: Context, uri: Uri): ClipFile? {
    val contentResolver: ContentResolver = context.contentResolver

    val query =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
    if (query == null) {
        return null
    }
    val (name, size) =
        query.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                name to size
            } else {
                null
            }
        } ?: return null
    val type = contentResolver.getType(uri) ?: "*/*"
    return ClipFile(context, name, ContentType.parse(type), size, uri.toString())
}

class ClipFile(
    context: Context,
    override val name: String,
    override val contentType: ContentType,
    override val size: Long,
    override val path: String,
) : ClientFile {
    private val contentResolver: ContentResolver = context.contentResolver

    override fun source(): Source {
        val stream = contentResolver.openInputStream(path.toUri()) ?: error("get stream failed")
        return BufferedInputStream(stream).asSource().buffered()
    }
}
