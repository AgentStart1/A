/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import androidx.compose.runtime.staticCompositionLocalOf
import com.storyteller_f.shared.model.FileInfo
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.io.RawSink

/** A save request, deliberately without a generated toString that could expose a signed resource URL. */
class ResourceDownloadRequest(val url: String, val fileName: String, val expectedSize: Long? = null) {
    /** Unknown-size resources remain valid; known metadata must match exactly before reporting completion. */
    fun acceptsSize(byteCount: Long): Boolean = expectedSize == null || byteCount == expectedSize
}

internal val LocalResourceDownloadFile = staticCompositionLocalOf<FileInfo?> { null }

internal fun mediaDownloadRequest(url: String, file: FileInfo?): ResourceDownloadRequest =
    if (file != null && file.url == url) {
        ResourceDownloadRequest(url, file.name, file.size)
    } else {
        val name = url.substringBefore('?').substringBefore('#').substringAfterLast('/').ifBlank { "download" }
        ResourceDownloadRequest(url, name)
    }

enum class ResourceSaveOutcome { SAVED, HANDED_OFF, CANCELLED }

/** The owning application's existing global controller owns downloads after a local menu is dismissed. */
val LocalResourceDownloadAction =
    staticCompositionLocalOf<(ResourceDownloadRequest) -> Unit> {
        { error("Resource download action is not provided") }
    }

/** Saves to the platform destination, or hands a named Blob to the browser; never stores an application cache. */
expect suspend fun saveRemoteResource(
    client: HttpClient,
    resource: ResourceDownloadRequest,
    dialogSettings: FileKitDialogSettings,
): ResourceSaveOutcome

/** Streams once into a native destination; cancellation/failure closes the sink and never replays the request. */
suspend fun streamRemoteResource(client: HttpClient, resource: ResourceDownloadRequest, openSink: () -> RawSink) {
    try {
        client.prepareGet(resource.url).execute { response ->
            if (response.status != HttpStatusCode.OK) {
                throw ResourceDownloadFailure("Resource download HTTP ${response.status.value}")
            }
            val channel = response.body<ByteReadChannel>()
            openSink().use { sink ->
                var received = 0L
                while (!channel.isClosedForRead) {
                    val chunk = channel.readBuffer(64 * 1024L)
                    received += chunk.remaining
                    chunk.transferTo(sink)
                }
                if (!resource.acceptsSize(received)) {
                    throw ResourceDownloadFailure("Downloaded resource size does not match")
                }
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: ResourceDownloadFailure) {
        throw failure
    } catch (_: Exception) {
        // Network exceptions may contain the signed URL; the operation dialog receives only a safe failure.
        throw ResourceDownloadFailure("Resource download failed")
    }
}

private class ResourceDownloadFailure(message: String) : IllegalStateException(message)
