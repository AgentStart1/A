/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.mikepenz.aboutlibraries.Libs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface LibrariesState {
    data object Loading : LibrariesState
    data class Ready(val libraries: Libs) : LibrariesState
    data class Failed(val cause: Throwable) : LibrariesState
}

internal class AboutLibrariesHost(
    parentScope: CoroutineScope,
    coordination: CoroutineDispatcher,
    private val worker: CoroutineDispatcher,
    private val load: suspend () -> Libs,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val mutableState = MutableStateFlow<LibrariesState>(LibrariesState.Loading)
    val state = mutableState.asStateFlow()
    private var loading: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            loading?.cancel()
            loading =
                scope.launch {
                    mutableState.value = LibrariesState.Loading
                    try {
                        val libraries = withContext(worker) { load() }
                        check(libraries.libraries.isNotEmpty()) { "Library definitions are empty" }
                        mutableState.value = LibrariesState.Ready(libraries)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        mutableState.value = LibrariesState.Failed(failure)
                    }
                }
        }
    }

    fun close() {
        scope.cancel()
    }
}
