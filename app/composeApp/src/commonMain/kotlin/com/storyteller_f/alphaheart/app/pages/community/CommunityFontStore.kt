/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.utils.md5
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val FONT_STOP_MILLIS = 5_000L

internal sealed interface CommunityFontState {
    data object Loading : CommunityFontState
    data class Ready(val bundle: CommunityFontBundle) : CommunityFontState
    data class Failed(val failure: Exception) : CommunityFontState
}

/** An application/server registry of shared construction flows; active entries are never evicted. */
internal class CommunityFontStore(
    private val scope: CoroutineScope,
    private val observe: (FileInfo) -> Flow<CommunityFontState>,
    private val resume: (FileInfo) -> Unit,
) {
    private val mutex = Mutex()
    private val entries = mutableMapOf<String, FontFlowEntry>()

    /** Each collector leases the registered stateIn, including throughout its stop grace period. */
    fun observeFont(file: FileInfo): Flow<CommunityFontState> =
        flow {
            scope.coroutineContext.ensureActive()
            val key = fontFileKey(file)
            val entry =
                mutex.withLock {
                    entries.getOrPut(key) {
                        val owner = CoroutineScope(scope.coroutineContext + Job(scope.coroutineContext[Job]))
                        FontFlowEntry(
                            owner,
                            observe(file).stateIn(
                                owner,
                                SharingStarted.WhileSubscribed(FONT_STOP_MILLIS, replayExpirationMillis = 0),
                                CommunityFontState.Loading,
                            ),
                        )
                    }.also {
                        it.retain()
                    }
                }
            try {
                emitAll(entry.state)
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (entry.release()) scheduleRemoval(key, entry)
                    }
                }
            }
        }

    fun retry(file: FileInfo) = resume(file)

    private fun scheduleRemoval(key: String, entry: FontFlowEntry) {
        val epoch = entry.epoch
        scope.launch {
            delay(FONT_STOP_MILLIS)
            mutex.withLock {
                if (entry.isIdle(epoch) && entries[key] === entry) {
                    entries.remove(key)
                    entry.scope.cancel()
                }
            }
        }
    }
}

private class FontFlowEntry(val scope: CoroutineScope, val state: StateFlow<CommunityFontState>) {
    private var readers = 0
    var epoch = 0L
        private set

    fun retain() {
        readers++
        epoch++
    }

    fun release(): Boolean {
        readers--
        return readers == 0
    }

    fun isIdle(expectedEpoch: Long): Boolean = readers == 0 && epoch == expectedEpoch
}

internal fun fontFileKey(file: FileInfo): String =
    md5("${file.id}:${file.fullName}:${file.lastModified}:${file.size}:${file.name}")
