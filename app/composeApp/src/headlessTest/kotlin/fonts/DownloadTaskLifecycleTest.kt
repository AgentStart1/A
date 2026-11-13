/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.DiskFileEngine
import com.storyteller_f.alphaheart.app.common.DownloadFileLease
import com.storyteller_f.alphaheart.app.common.FileEngine
import com.storyteller_f.alphaheart.app.common.createDownloadRecord
import com.storyteller_f.alphaheart.app.common.createExclusiveArchiveSink
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadInfoStorage
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadTaskLifecycleTest {
    @Test
    fun resumePreparationFailurePreservesOtherAccountTasks() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val sibling = account.launch { awaitCancellation() }
            val disk = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            val files =
                object : FileEngine by disk {
                    override suspend fun size(path: String): Long? = error("Synthetic storage lookup failure")
                }
            createClient { error("Preparation failure must not send a request") }.use { client ->
                val fixture = DownloadTestFixture(account, files, client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    fixture.records.save(
                        createDownloadRecord(file, files.location("${fixture.namespace}/source"))
                            .copy(status = DownloadStatus.PAUSED),
                    )
                    fixture.downloader.resume(file.id)
                    runCurrent()
                    assertTrue(account.isActive)
                    assertTrue(sibling.isActive)
                    assertEquals(emptyList(), failures)
                    assertEquals(DownloadStatus.DOWNLOAD_FAILED, fixture.records.getDocumentByFileId(file.id)?.status)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    @Test
    fun duplicateResumeDoesNotRetryAfterPause() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val files = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            var requests = 0
            var allowCompletion = false
            val bytes = CommunityFontTestData.primary
            createClient {
                requests++
                if (!allowCompletion) awaitCancellation()
                respond(bytes)
            }.use { client ->
                val fixture = DownloadTestFixture(account, files, client)
                try {
                    val file = fontFile(bytes)
                    fixture.records.save(
                        createDownloadRecord(file, files.location("${fixture.namespace}/source"))
                            .copy(status = DownloadStatus.PAUSED),
                    )
                    fixture.downloader.resume(file.id)
                    fixture.downloader.resume(file.id)
                    runCurrent()
                    assertEquals(1, requests)
                    fixture.downloader.pause(file.id)
                    runCurrent()
                    assertEquals(1, requests)
                    assertEquals(DownloadStatus.PAUSED, fixture.records.getDocumentByFileId(file.id)?.status)
                    allowCompletion = true
                    fixture.downloader.resume(file.id)
                    runCurrent()
                    assertEquals(2, requests)
                    assertEquals(DownloadStatus.PROCESSED, fixture.records.getDocumentByFileId(file.id)?.status)
                    assertTrue(account.isActive)
                    assertEquals(emptyList(), failures)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    @Test
    fun resumeDuringOrdinaryRegistrationIsIgnored() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val disk = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            var first = true
            val files =
                object : FileEngine by disk {
                    override suspend fun acquire(key: String): DownloadFileLease {
                        if (first) {
                            first = false
                            entered.complete(Unit)
                            release.await()
                        }
                        return disk.acquire(key)
                    }
                }
            val bytes = CommunityFontTestData.primary
            var requests = 0
            createClient {
                requests++
                respond(bytes)
            }.use { client ->
                val fixture = DownloadTestFixture(account, files, client)
                try {
                    val file = fontFile(bytes)
                    fixture.records.save(
                        createDownloadRecord(file, files.location("${fixture.namespace}/source"))
                            .copy(status = DownloadStatus.PAUSED),
                    )
                    val ordinary = async { fixture.downloader.download(file) }
                    entered.await()
                    fixture.downloader.resume(file.id)
                    release.complete(Unit)
                    assertEquals(DownloadStatus.PAUSED, assertNotNull(ordinary.await()).status)
                    runCurrent()
                    assertEquals(0, requests)
                    assertEquals(DownloadStatus.PAUSED, fixture.records.getDocumentByFileId(file.id)?.status)
                    fixture.downloader.resume(file.id)
                    runCurrent()
                    assertEquals(1, requests)
                    assertEquals(DownloadStatus.PROCESSED, fixture.records.getDocumentByFileId(file.id)?.status)
                    assertTrue(account.isActive)
                    assertEquals(emptyList(), failures)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    @Test
    fun resumeLookupFailurePreservesOtherAccountTasks() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val sibling = account.launch { awaitCancellation() }
            val files = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            var lookups = 0
            val records =
                object : DownloadInfoStorage by DownloadTestRecords() {
                    override suspend fun getDocumentByFileId(fileId: Long): DownloadInfo? {
                        lookups++
                        error("Synthetic database lookup failure")
                    }
                }
            createClient { error("Lookup failure must not send a request") }.use { client ->
                val fixture = DownloadTestFixture(account, files, client, records = records)
                try {
                    fixture.downloader.resume(1)
                    runCurrent()
                    assertEquals(1, lookups)
                    assertTrue(account.isActive)
                    assertTrue(sibling.isActive)
                    assertEquals(emptyList(), failures)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    @Test
    fun failedStatusWritePreservesOriginalFailureAndAccount() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val sibling = account.launch { awaitCancellation() }
            val disk = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            val files =
                object : FileEngine by disk {
                    override suspend fun size(path: String): Long? = error("Synthetic storage lookup failure")
                }
            val backing = DownloadTestRecords()
            val records =
                object : DownloadInfoStorage by backing {
                    override suspend fun save(item: DownloadInfo) {
                        if (item.status == DownloadStatus.DOWNLOAD_FAILED) error("Synthetic status write failure")
                        backing.save(item)
                    }
                }
            createClient { error("Preparation failure must not send a request") }.use { client ->
                val fixture = DownloadTestFixture(account, files, client, records = records)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    records.save(createDownloadRecord(file, files.location("${fixture.namespace}/source")))
                    val failure = assertFailsWith<IllegalStateException> { fixture.downloader.download(file) }
                    assertEquals("Synthetic storage lookup failure", failure.message)
                    assertTrue(
                        generateSequence<Throwable>(failure) { it.cause }
                            .flatMap { it.suppressedExceptions }.any { it.message == "Synthetic status write failure" },
                    )
                    assertTrue(account.isActive)
                    assertTrue(sibling.isActive)
                    assertEquals(emptyList(), failures)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    @Test
    fun pausingRegistrationDoesNotCancelItsActiveCaller() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val entered = CompletableDeferred<Unit>()
            val disk = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            val files =
                object : FileEngine by disk {
                    override suspend fun acquire(key: String): DownloadFileLease {
                        entered.complete(Unit)
                        awaitCancellation()
                    }
                }
            createClient { error("Paused registration must not send a request") }.use { client ->
                val fixture = DownloadTestFixture(account, files, client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    val caller =
                        async {
                            assertFailsWith<IllegalStateException> { fixture.downloader.download(file) }
                        }
                    entered.await()
                    fixture.downloader.pause(file.id)
                    assertEquals("Download request was paused", caller.await().message)
                    assertTrue(account.isActive)
                    assertEquals(emptyList(), failures)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    @Test
    fun pauseStatusWriteFailurePreservesOperationCancellation() =
        runTest {
            val failures = mutableListOf<Throwable>()
            val account = createAccountScope(failures)
            val entered = CompletableDeferred<Unit>()
            val disk = DiskFileEngine(StandardTestDispatcher(testScheduler), ::createExclusiveArchiveSink)
            val files =
                object : FileEngine by disk {
                    override suspend fun size(path: String): Long? {
                        entered.complete(Unit)
                        awaitCancellation()
                    }
                }
            val records =
                DownloadTestRecords { saved ->
                    if (saved.status == DownloadStatus.PAUSED) error("Synthetic pause write failure")
                }
            createClient { error("Paused registration must not send a request") }.use { client ->
                val fixture = DownloadTestFixture(account, files, client, records = records)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    records.save(createDownloadRecord(file, files.location("${fixture.namespace}/source")))
                    val caller =
                        async {
                            assertFailsWith<IllegalStateException> { fixture.downloader.download(file) }
                        }
                    entered.await()
                    fixture.downloader.pause(file.id)
                    val failure = caller.await()
                    assertEquals("Download request was paused", failure.message)
                    assertTrue(
                        generateSequence<Throwable>(failure) { it.cause }
                            .flatMap { it.suppressedExceptions }.any { it.message == "Synthetic pause write failure" },
                    )
                    assertTrue(account.isActive)
                    assertEquals(emptyList(), failures)
                } finally {
                    checkNotNull(account.coroutineContext[Job]).cancelAndJoin()
                    fixture.close()
                }
            }
        }

    private fun TestScope.createClient(handler: MockRequestHandler): HttpClient =
        HttpClient(MockEngine) {
            engine {
                dispatcher = StandardTestDispatcher(testScheduler)
                addHandler(handler)
            }
        }

    private fun TestScope.createAccountScope(failures: MutableList<Throwable>): CoroutineScope =
        CoroutineScope(
            Job() + StandardTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, failure -> failures += failure },
        )
}
