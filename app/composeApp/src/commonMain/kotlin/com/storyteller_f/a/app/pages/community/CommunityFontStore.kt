/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.utils.md5
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MAX_CACHED_FONT_BYTES = 128L * 1024 * 1024
private const val MAX_CACHED_FONT_FILES = 12

/** One account/session owns this ephemeral cache. A cancelled reader does not cancel other readers. */
internal class CommunityFontStore(
    private val scope: CoroutineScope,
    private val maxCachedBytes: Long = MAX_CACHED_FONT_BYTES,
    private val fetch: suspend (FileInfo, Boolean) -> CommunityFontBundle,
) {
    private val mutex = Mutex()
    private val cached = linkedMapOf<String, CommunityFontBundle>()
    private val pending = mutableMapOf<String, PendingFontLoad>()
    private val reloadEvents = MutableSharedFlow<CommunityFontReloaded>(extraBufferCapacity = MAX_CACHED_FONT_FILES)

    /** Successful explicit retries only; ordinary cache fills and evictions do not request UI refreshes. */
    val reloads = reloadEvents.asSharedFlow()

    /** Each subscription owns its collector; shared account-owned reads survive unsubscription. */
    fun observeFont(file: FileInfo, retry: Boolean = false): Flow<CommunityFontBundle?> =
        flow {
            val key = fontFileKey(file)
            coroutineScope {
                // Subscribe before loading, retaining the latest retry while the initial read suspends.
                val refreshed = Channel<CommunityFontBundle>(Channel.CONFLATED)
                launch(start = CoroutineStart.UNDISPATCHED) {
                    reloads.filter { it.key == key }.collect { refreshed.send(it.bundle) }
                }
                try {
                    emit(load(file, retry))
                    for (bundle in refreshed) emit(bundle)
                } finally {
                    refreshed.cancel()
                }
            }
        }.distinctUntilChanged()

    suspend fun load(file: FileInfo, retry: Boolean = false): CommunityFontBundle? {
        val key = fontFileKey(file)
        val result =
            mutex.withLock {
                if (!retry) {
                    cached.remove(key)?.let { value ->
                        cached[key] = value
                        return value
                    }
                }
                val previous = pending[key]
                if (previous != null && (!retry || previous.retry)) {
                    previous.result
                } else {
                    if (retry) cached.remove(key)
                    scope.async(start = CoroutineStart.LAZY) {
                        readAndCache(file, retry, previous?.result)
                    }.also { pending[key] = PendingFontLoad(retry, it) }
                }
            }
        result.start()
        return result.await()
    }

    private suspend fun readAndCache(
        file: FileInfo,
        retry: Boolean,
        previous: Deferred<CommunityFontBundle?>?,
    ): CommunityFontBundle? {
        val key = fontFileKey(file)
        val owner = checkNotNull(currentCoroutineContext()[Job])
        try {
            previous?.await()
            val bundle = readFont(file, retry)
            currentCoroutineContext().ensureActive()
            val published =
                mutex.withLock {
                    if (pending[key]?.result !== owner) {
                        false
                    } else {
                        if (bundle != null) cached[key] = bundle
                        trimCache()
                        true
                    }
                }
            if (published && retry && bundle != null) reloadEvents.emit(CommunityFontReloaded(key, bundle))
            return bundle
        } finally {
            withContext(NonCancellable) {
                mutex.withLock { if (pending[key]?.result === owner) pending.remove(key) }
            }
        }
    }

    private suspend fun readFont(file: FileInfo, retry: Boolean): CommunityFontBundle? =
        try {
            fetch(file, retry)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (fatal: Error) {
            throw fatal
        } catch (failure: Throwable) {
            Napier.w { "Unable to load a community font: ${failure.message.orEmpty()}" }
            null
        }

    private fun trimCache() {
        while (cached.size > MAX_CACHED_FONT_FILES || cached.values.sumOf { it.byteCount } > maxCachedBytes) {
            cached.remove(cached.keys.first())
        }
    }
}

internal data class CommunityFontReloaded(val key: String, val bundle: CommunityFontBundle)

private data class PendingFontLoad(val retry: Boolean, val result: Deferred<CommunityFontBundle?>)

internal fun fontFileKey(file: FileInfo): String =
    md5(
        "${file.id}:${file.fullName}:${file.lastModified}:${file.size}:${file.name}",
    )
