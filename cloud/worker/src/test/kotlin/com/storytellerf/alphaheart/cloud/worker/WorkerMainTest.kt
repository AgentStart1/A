/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker

import com.storyteller_f.alphaheart.cloud.worker.TopicSafetyReviewerProvider
import com.storyteller_f.alphaheart.cloud.worker.WORKER_TASK_POLL_MILLIS
import com.storyteller_f.alphaheart.cloud.worker.createTopicSafetyReviewer
import com.storyteller_f.alphaheart.cloud.worker.executeConfiguredTaskIteration
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import com.storytellerf.alphaheart.cloud.worker.moderation.TopicSafetyReviewer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class WorkerMainTest {
    @Test
    fun `reviewer retries after factory failure`() =
        runTest {
            val config = commonJson.encodeToString(LlmConfig(provider = LlmProvider.OPENAI))
            val reviewer = TopicSafetyReviewer { false }
            var attempts = 0
            TopicSafetyReviewerProvider {
                attempts += 1
                if (attempts == 1) {
                    createTopicSafetyReviewer(CustomResult.success(config))
                } else {
                    CustomResult.success(reviewer)
                }
            }.use { provider ->
                assertNull(provider.get())
                assertSame(reviewer, provider.get())
                assertSame(reviewer, provider.get())
                assertEquals(2, attempts)
            }
        }

    @Test
    fun `reviewer initialization preserves cancellation`() =
        runTest {
            val cancellation = CancellationException("initialization cancelled")
            TopicSafetyReviewerProvider { throw cancellation }.use { provider ->
                assertSame(cancellation, assertFailsWith<CancellationException> { provider.get() })
            }
        }

    @Test
    fun `reviewer provider retries initialization`() {
        runTest {
            var attempts = 0
            val provider =
                TopicSafetyReviewerProvider {
                    attempts += 1
                    CustomResult.success(if (attempts == 1) null else TopicSafetyReviewer { false })
                }

            assertEquals(null, provider.get())
            assertNotNull(provider.get())
            assertNotNull(provider.get())
            assertEquals(2, attempts)
        }
    }

    @Test
    fun `missing task configuration does not execute task`() {
        runTest {
            var isExecuted = false
            var waited = 0L

            executeConfiguredTaskIteration(
                name = "intro",
                configResult = CustomResult.success(null),
                task = { isExecuted = true },
                wait = { waited = it },
            )

            assertFalse(isExecuted)
            assertEquals(WORKER_TASK_POLL_MILLIS, waited)
        }
    }

    @Test
    fun `disabled task configuration does not execute task`() {
        runTest {
            val config =
                WorkerTask(
                    type = TaskRecordType.INTRO,
                    isEnabled = false,
                    fetchSize = 10,
                    waitDurationMillis = 50_000,
                )
            var isExecuted = false
            var waited = 0L

            executeConfiguredTaskIteration(
                name = "intro",
                configResult = CustomResult.success(config),
                task = { isExecuted = true },
                wait = { waited = it },
            )

            assertFalse(isExecuted)
            assertEquals(WORKER_TASK_POLL_MILLIS, waited)
        }
    }

    @Test
    fun `enabled task uses configured limits`() {
        runTest {
            val config =
                WorkerTask(
                    type = TaskRecordType.INTRO,
                    isEnabled = true,
                    fetchSize = 23,
                    waitDurationMillis = 4_000,
                )
            var executedConfig: WorkerTask? = null
            var waited = 0L

            executeConfiguredTaskIteration(
                name = "intro",
                configResult = CustomResult.success(config),
                task = { executedConfig = it },
                wait = { waited = it },
            )

            assertEquals(config, executedConfig)
            assertEquals(config.waitDurationMillis, waited)
        }
    }
}
