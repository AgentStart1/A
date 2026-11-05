/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface TaskRegister {
    fun lockTask(key: String, block: suspend () -> Unit)

    /**
     * Merge only with merge-enabled admissions. [prepare] runs after admission, before waiting for
     * the serial turn, so queued requests capture their context while remaining owned by pause.
     */
    suspend fun runTask(
        key: String,
        merge: Boolean = false,
        prepare: suspend () -> Unit = {},
        block: suspend () -> Unit,
    )
    fun stop(key: String, block: suspend () -> Unit)
}

/** A stop invalidates all earlier admissions and publishes its state before later work can start. */
class SimpleTaskRegister(val lifecycleScope: CoroutineScope) : TaskRegister {
    val runningTaskCount = MutableStateFlow(0)
    private val mutex = Mutex()
    private val groups = mutableMapOf<String, RegisteredTasks>()

    override fun lockTask(key: String, block: suspend () -> Unit) {
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            executeTask(key, wait = false, merge = false, prepare = {}, block)
        }
    }

    override suspend fun runTask(key: String, merge: Boolean, prepare: suspend () -> Unit, block: suspend () -> Unit) {
        executeTask(key, wait = true, merge, prepare, block)
    }

    override fun stop(key: String, block: suspend () -> Unit) {
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val owner = checkNotNull(currentCoroutineContext()[Job])
            val stopped = beginStop(key, owner)
            try {
                stopped.jobs.forEach { it.cancel(TaskStoppedException()) }
                stopped.jobs.joinAll()
                stopped.previous?.join()
                block()
            } finally {
                withContext(NonCancellable) {
                    mutex.withLock {
                        if ((stopped.group.phase as? TaskPhase.Stopping)?.owner === owner) {
                            stopped.group.phase = TaskPhase.Idle
                            activateNext(stopped.group)
                        }
                        discardUnused(key, stopped.group)
                    }
                }
            }
        }
    }

    private suspend fun beginStop(key: String, owner: Job): TaskStop =
        mutex.withLock {
            val group = groups.getOrPut(key) { RegisteredTasks() }
            val previous = (group.phase as? TaskPhase.Stopping)?.owner
            TaskStop(group, previous, group.jobs.keys.toList()).also {
                group.jobs.clear()
                group.phase = TaskPhase.Stopping(owner)
            }
        }

    private suspend fun executeTask(
        key: String,
        wait: Boolean,
        merge: Boolean,
        prepare: suspend () -> Unit,
        block: suspend () -> Unit,
    ) {
        val job = checkNotNull(currentCoroutineContext()[Job])
        currentCoroutineContext().ensureActive()
        val admission = admit(key, job, wait, merge) ?: return
        try {
            prepare()
            admission.request.ready.await()
            mutex.withLock {
                currentCoroutineContext().ensureActive()
                if (job !in admission.group.jobs) throw TaskStoppedException()
                check((admission.group.phase as? TaskPhase.Running)?.job === job)
                runningTaskCount.update { it + 1 }
            }
            try {
                block()
            } finally {
                runningTaskCount.update { it - 1 }
            }
        } finally {
            release(key, admission)
        }
    }

    private suspend fun admit(key: String, job: Job, wait: Boolean, merge: Boolean): TaskAdmission? =
        mutex.withLock {
            val group = groups.getOrPut(key) { RegisteredTasks() }
            check(job !in group.jobs && (group.phase as? TaskPhase.Stopping)?.owner !== job) {
                "A registered task cannot await itself"
            }
            val duplicate = !wait && group.jobs.isNotEmpty()
            val merging = merge && group.jobs.values.any { it.merge }
            if (duplicate || merging) {
                null
            } else {
                val request = RegisteredTask(job, merge)
                group.jobs[job] = request
                activateNext(group)
                TaskAdmission(group, request)
            }
        }

    private suspend fun release(key: String, admission: TaskAdmission) =
        withContext(NonCancellable) {
            mutex.withLock {
                val group = admission.group
                val job = admission.request.job
                group.jobs.remove(job)
                if ((group.phase as? TaskPhase.Running)?.job === job) group.phase = TaskPhase.Idle
                activateNext(group)
                discardUnused(key, group)
            }
        }

    private fun activateNext(group: RegisteredTasks) {
        if (group.phase != TaskPhase.Idle) return
        val request = group.jobs.values.firstOrNull() ?: return
        group.phase = TaskPhase.Running(request.job)
        request.ready.complete(Unit)
    }

    private fun discardUnused(key: String, group: RegisteredTasks) {
        if (group.jobs.isEmpty() && group.phase == TaskPhase.Idle && groups[key] === group) groups.remove(key)
    }
}

internal class TaskStoppedException : CancellationException("Task stopped")

private sealed interface TaskPhase {
    data object Idle : TaskPhase
    data class Running(val job: Job) : TaskPhase
    data class Stopping(val owner: Job) : TaskPhase
}

private class RegisteredTasks {
    val jobs = linkedMapOf<Job, RegisteredTask>()
    var phase: TaskPhase = TaskPhase.Idle
}

private data class RegisteredTask(
    val job: Job,
    val merge: Boolean,
    val ready: CompletableDeferred<Unit> = CompletableDeferred(),
)

private data class TaskAdmission(val group: RegisteredTasks, val request: RegisteredTask)
private data class TaskStop(val group: RegisteredTasks, val previous: Job?, val jobs: List<Job>)
