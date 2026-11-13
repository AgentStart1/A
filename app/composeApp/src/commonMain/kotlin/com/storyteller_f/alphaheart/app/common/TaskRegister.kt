/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface TaskRegister {
    fun observeTask(key: String): Flow<Boolean>
    fun lockTask(key: String, block: suspend () -> Unit)

    /** Return false without preparing or executing when this key is running or stopping. */
    suspend fun runTask(key: String, prepare: suspend () -> Unit = {}, block: suspend () -> Unit): Boolean
    fun stop(key: String, block: suspend () -> Unit)
}

/** Admission is atomic per key; independent keys execute concurrently outside the registry mutex. */
class SimpleTaskRegister(val lifecycleScope: CoroutineScope) : TaskRegister {
    val runningTaskCount = MutableStateFlow(0)
    private val mutex = Mutex()
    private val tasks = mutableMapOf<String, TaskPhase>()
    private val activeKeys = MutableStateFlow<Set<String>>(emptySet())

    override fun observeTask(key: String): Flow<Boolean> = activeKeys.map { key in it }.distinctUntilChanged()

    override fun lockTask(key: String, block: suspend () -> Unit) {
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) { runTask(key, block = block) }
    }

    override suspend fun runTask(key: String, prepare: suspend () -> Unit, block: suspend () -> Unit): Boolean {
        val job = checkNotNull(currentCoroutineContext()[Job])
        currentCoroutineContext().ensureActive()
        val admitted =
            mutex.withLock {
                if (key in tasks) {
                    false
                } else {
                    tasks[key] = TaskPhase.Running(job)
                    activeKeys.value = tasks.keys.toSet()
                    true
                }
            }
        if (!admitted) return false
        try {
            prepare()
            mutex.withLock {
                currentCoroutineContext().ensureActive()
                if (tasks[key] != TaskPhase.Running(job)) throw TaskStoppedException()
                runningTaskCount.update { it + 1 }
            }
            try {
                block()
            } finally {
                withContext(NonCancellable) { mutex.withLock { runningTaskCount.update { it - 1 } } }
            }
            return true
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (tasks[key] == TaskPhase.Running(job)) {
                        tasks.remove(key)
                        activeKeys.value = tasks.keys.toSet()
                    }
                }
            }
        }
    }

    override fun stop(key: String, block: suspend () -> Unit) {
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val owner = checkNotNull(currentCoroutineContext()[Job])
            val previous =
                mutex.withLock {
                    tasks.put(key, TaskPhase.Stopping(owner)).also { activeKeys.value = tasks.keys.toSet() }
                }
            try {
                if (previous is TaskPhase.Running) previous.owner.cancel(TaskStoppedException())
                previous?.owner?.join()
                block()
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if (tasks[key] == TaskPhase.Stopping(owner)) {
                            tasks.remove(key)
                            activeKeys.value = tasks.keys.toSet()
                        }
                    }
                }
            }
        }
    }
}

internal class TaskStoppedException : CancellationException("Task stopped")

private sealed interface TaskPhase {
    val owner: Job
    data class Running(override val owner: Job) : TaskPhase
    data class Stopping(override val owner: Job) : TaskPhase
}
