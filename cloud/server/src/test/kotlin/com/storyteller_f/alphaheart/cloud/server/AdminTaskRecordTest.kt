/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.client.core.getTaskRecordSummaries
import com.storyteller_f.alphaheart.client.core.getTaskRecords
import com.storyteller_f.alphaheart.client.core.getWorkerTasks
import com.storyteller_f.alphaheart.client.core.markTaskRecordForRetry
import com.storyteller_f.alphaheart.client.core.updateWorkerTask
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.utils.now
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminTaskRecordTest {
    @Test
    fun `admin creates persisted task config`() {
        test {
            val outer = attachPanelSession()
            loginPanelSession(outer) {
                assertTrue(getWorkerTasks().getOrThrow().data.isEmpty())

                val initial =
                    WorkerTask(
                        type = TaskRecordType.INTRO,
                        isEnabled = true,
                        fetchSize = 12,
                        waitDurationMillis = 2_000,
                    )
                assertEquals(initial, updateWorkerTask(initial).getOrThrow())
                assertEquals(listOf(initial), getWorkerTasks().getOrThrow().data)
            }
        }
    }

    @Test
    fun `admin updates persisted task config`() {
        test {
            val outer = attachPanelSession()
            loginPanelSession(outer) {
                val initial =
                    WorkerTask(
                        type = TaskRecordType.INTRO,
                        isEnabled = true,
                        fetchSize = 12,
                        waitDurationMillis = 2_000,
                    )
                updateWorkerTask(initial).getOrThrow()

                val updated = initial.copy(isEnabled = false, fetchSize = 4)
                assertEquals(updated, updateWorkerTask(updated).getOrThrow())
                assertEquals(listOf(updated), getWorkerTasks().getOrThrow().data)
            }
        }
    }

    @Test
    fun `admin paginates worker task records`() =
        test {
            val outer = attachPanelSession()
            prepareTaskRecords()
            loginPanelSession(outer) {
                val firstPage = getTaskRecords(null, PaginationQuery(size = 2)).getOrThrow()
                assertEquals(listOf(104L, 103L), firstPage.data.map { it.id })
                assertEquals(4, firstPage.pagination?.total)

                val secondPage =
                    getTaskRecords(
                        null,
                        PaginationQuery(nextPageToken = firstPage.pagination?.nextPageToken, size = 2),
                    ).getOrThrow()
                assertEquals(listOf(102L, 101L), secondPage.data.map { it.id })
            }
        }

    @Test
    fun `admin filters task records by type`() =
        test {
            val outer = attachPanelSession()
            prepareTaskRecords()
            loginPanelSession(outer) {
                val filtered = getTaskRecords(TaskRecordType.TITLE, PaginationQuery()).getOrThrow()
                assertEquals(listOf(TaskRecordType.TITLE), filtered.data.map { it.type })
            }
        }

    @Test
    fun `admin filters failed task records`() =
        test {
            val outer = attachPanelSession()
            prepareTaskRecords()
            loginPanelSession(outer) {
                val failed =
                    getTaskRecords(
                        type = TaskRecordType.TOPIC_MODERATION,
                        query = PaginationQuery(),
                        isSuccess = false,
                        failureType = TaskRecordType.MODEL_RESPONSE_FAILURE,
                    ).getOrThrow()
                assertEquals(listOf(104L), failed.data.map { it.id })
            }
        }

    @Test
    fun `admin summarizes task records`() =
        test {
            val outer = attachPanelSession()
            prepareTaskRecords()
            loginPanelSession(outer) {
                val summaries = getTaskRecordSummaries().getOrThrow().data.associateBy { it.type }
                assertEquals(1, summaries[TaskRecordType.TOPIC_MODERATION]?.failureCount)
                assertEquals(1, summaries[TaskRecordType.TITLE]?.successCount)
            }
        }

    @Test
    fun `admin requests failed task retry`() =
        test {
            val outer = attachPanelSession()
            prepareTaskRecords()
            loginPanelSession(outer) {
                markTaskRecordForRetry(104).getOrThrow()
                val retryRequested =
                    getTaskRecords(
                        TaskRecordType.TOPIC_MODERATION,
                        PaginationQuery(),
                        isSuccess = false,
                    ).getOrThrow()
                assertEquals(true, retryRequested.data.single().isRetryRequested)
            }
        }

    private suspend fun TestMate.prepareTaskRecords() {
        withWorkerBackend { backend ->
            listOf(
                TaskRecord(101, now(), TaskRecordType.INTRO, 1001),
                TaskRecord(102, now(), TaskRecordType.TITLE, 1002),
                TaskRecord(103, now(), TaskRecordType.SUBSCRIPTION, 1003),
                TaskRecord(
                    id = 104,
                    createdTime = now(),
                    type = TaskRecordType.TOPIC_MODERATION,
                    objectId = 1004,
                    failureType = TaskRecordType.MODEL_RESPONSE_FAILURE,
                    failureReason = "unexpected model output",
                ),
            ).forEach {
                backend.database.admin.createTaskRecord(it).getOrThrow()
            }
        }
    }
}
