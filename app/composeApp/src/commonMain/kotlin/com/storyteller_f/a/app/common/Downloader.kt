/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.common

import com.storyteller_f.a.app.UIViewModel
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.storage.DownloadInfo
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

interface Downloader {
    /** Register/validate the request, then return its current record. Observe the database for completion. */
    suspend fun download(fileInfo: FileInfo, retry: Boolean = false): DownloadInfo
    fun resume(id: PrimaryKey)
    fun pause(id: PrimaryKey)
}

interface TaskRegister {
    fun lockTask(key: String, block: suspend () -> Unit)

    /** Merge requests only with other merge-enabled admissions; ordinary queued work still precedes them. */
    suspend fun runTask(key: String, merge: Boolean = false, block: suspend () -> Unit)
    fun stop(key: String, block: suspend () -> Unit)
}

/** A stop invalidates all earlier admissions and publishes its state before later work can start. */
class SimpleTaskRegister(val lifecycleScope: CoroutineScope) : TaskRegister {
    val runningTaskCount = MutableStateFlow(0)
    private val mutex = Mutex()
    private val groups = mutableMapOf<String, RegisteredTasks>()

    override fun lockTask(key: String, block: suspend () -> Unit) {
        lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            executeTask(key, wait = false, merge = false, block)
        }
    }

    override suspend fun runTask(key: String, merge: Boolean, block: suspend () -> Unit) {
        executeTask(key, wait = true, merge, block)
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

    private suspend fun executeTask(key: String, wait: Boolean, merge: Boolean, block: suspend () -> Unit) {
        val job = checkNotNull(currentCoroutineContext()[Job])
        currentCoroutineContext().ensureActive()
        val admission = admit(key, job, wait, merge) ?: return
        try {
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

class DownloaderImpl(val uiViewModel: UIViewModel, val register: TaskRegister) : Downloader {
    private val files = createDownloadFileStore()

    private fun currentEngine(): DownloadEngine {
        val instance = uiViewModel.instance.value
        return DownloadEngine(
            DownloadContext(
                md5(instance.address),
                instance.accountScope,
                instance.database.download,
                instance.sessionManager,
            ),
            register,
            files,
        )
    }

    override suspend fun download(fileInfo: FileInfo, retry: Boolean): DownloadInfo =
        currentEngine().start(
            fileInfo,
            retry,
        )
    override fun resume(id: PrimaryKey) = currentEngine().resume(id)
    override fun pause(id: PrimaryKey) = currentEngine().pause(id)
}

internal fun getDownloadRevision(file: FileInfo): String =
    md5("${file.fullName}:${file.lastModified}:${file.size}:${file.name}")

internal fun createDownloadRecord(file: FileInfo, path: String): DownloadInfo =
    DownloadInfo(file.id, file, com.storyteller_f.storage.DownloadStatus.NOT_DOWNLOADED, "", path, 0, file.size)
