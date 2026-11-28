/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import com.storyteller_f.alphaheart.client.core.getClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer

internal actual fun createPublicAvatarFetcher(): Fetcher.Factory<Uri> {
    val cache = PublicAvatarCache()
    return Fetcher.Factory { uri, options, _ ->
        if (uri.scheme != "http" && uri.scheme != "https") {
            null
        } else {
            Fetcher {
                val (avatar, source) = cache.load(uri.toString())
                SourceFetchResult(
                    source = ImageSource(Buffer().write(avatar.bytes), options.fileSystem),
                    mimeType = avatar.mimeType,
                    dataSource = source,
                )
            }
        }
    }
}

private const val MAX_AVATAR_CACHE_BYTES = 4 * 1024 * 1024
private const val MAX_AVATAR_CACHE_ENTRIES = 256

private class AvatarBytes(val bytes: ByteArray, val mimeType: String?)

private class PublicAvatarCache {
    private val client = getClient { expectSuccess = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<AvatarBytes>>()
    private val cached = linkedMapOf<String, AvatarBytes>()
    private var cachedSize = 0

    suspend fun load(url: String): Pair<AvatarBytes, DataSource> {
        val pending =
            lock.withLock {
                cached.remove(url)?.let { avatar ->
                    cached[url] = avatar
                    return avatar to DataSource.MEMORY
                }
                inFlight.getOrPut(url) {
                    // The transfer belongs to the shared loader, so a scrolling row cannot cancel other waiters.
                    scope.async { download(url) }
                }
            }
        return pending.await() to DataSource.NETWORK
    }

    private suspend fun download(url: String): AvatarBytes {
        try {
            val response = client.get(url)
            val mimeType = response.headers[HttpHeaders.ContentType]?.substringBefore(';')
            val avatar = AvatarBytes(response.bodyAsBytes(), mimeType)
            lock.withLock {
                val size = avatar.bytes.size
                if (size <= MAX_AVATAR_CACHE_BYTES) {
                    while (cachedSize + size > MAX_AVATAR_CACHE_BYTES || cached.size >= MAX_AVATAR_CACHE_ENTRIES) {
                        val first = cached.entries.iterator()
                        cachedSize -= first.next().value.bytes.size
                        first.remove()
                    }
                    cached[url] = avatar
                    cachedSize += size
                }
            }
            return avatar
        } finally {
            lock.withLock { inFlight.remove(url) }
        }
    }
}
