/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.DownloadContext
import com.storyteller_f.alphaheart.app.common.DownloadSession
import com.storyteller_f.alphaheart.app.common.DownloaderImpl
import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import com.storyteller_f.alphaheart.app.common.captureDownloadSession
import com.storyteller_f.alphaheart.app.common.createDownloadRecord
import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class DownloadSessionLifetimeTest {
    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun pauseOwnsSessionPreparationBeforeExecution() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val cleaned = CompletableDeferred<Unit>()
            val records =
                DownloadTestRecords { record ->
                    if (record.status == DownloadStatus.PAUSED) assertTrue(cleaned.isCompleted)
                }
            val files = createFileEngine()
            val namespace = "prepare-${Uuid.random().toHexString()}"
            val downloader =
                DownloaderImpl(
                    DownloadContext(namespace, this, records) {
                        try {
                            entered.complete(Unit)
                            awaitCancellation()
                        } finally {
                            cleaned.complete(Unit)
                        }
                    },
                    SimpleTaskRegister(this),
                    files,
                )
            val file = fontFile(CommunityFontTestData.primary)
            records.save(createDownloadRecord(file, files.location("public-resources-v1/$namespace/source")))
            val caller = async { assertFailsWith<IllegalStateException> { downloader.download(file) } }
            entered.await()
            downloader.pause(file.id)
            assertEquals("Download request was paused", caller.await().message)
            records.observeDocumentByFileId(file.id).first { it?.status == DownloadStatus.PAUSED }
            assertTrue(cleaned.isCompleted)
        }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun aDifferentAccountReusesTheSamePublicFileTask() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var requests = 0
            val first =
                createSession {
                    requests++
                    started.complete(Unit)
                    release.await()
                    respond(CommunityFontTestData.primary)
                }
            val second = createSession { error("Same-version reuse cannot send another request") }
            var current = first
            val captured = mutableListOf<SimpleUserSessionManager>()
            val records = DownloadTestRecords()
            val files = createFileEngine()
            val namespace = "accounts-${Uuid.random().toHexString()}"
            val downloader =
                DownloaderImpl(
                    DownloadContext(namespace, this, records) {
                        captured += current
                        DownloadSession(current) {}
                    },
                    SimpleTaskRegister(this),
                    files,
                )
            try {
                val file = fontFile(CommunityFontTestData.primary)
                val initial = assertNotNull(downloader.download(file))
                started.await()
                current = second
                val reused = async { downloader.download(file) }
                runCurrent()
                assertEquals(listOf(first), captured)
                assertNull(reused.await())
                release.complete(Unit)
                downloader.observeTask(file.id).first { !it }
                assertEquals(initial.path, records.getDocumentByFileId(file.id)?.path)
                assertEquals(1L, records.getDocumentByFileId(file.id)?.processedGeneration)
                assertEquals(1, requests)
            } finally {
                first.client.close()
                second.client.close()
                files.delete(files.location("public-resources-v1/$namespace"))
            }
        }

    @Test
    fun accountClosureDoesNotCloseTheAdmittedTransferClient() =
        runTest {
            val origin = "https://fonts.invalid"
            val cookies = AcceptAllCookiesStorage()
            cookies.addCookie(Url(origin), Cookie("user_session", "synthetic-first", path = "/"))
            val owner =
                createSimpleUserSessionManager(
                    "wss://fonts.invalid/ws",
                    cookies,
                    ConstPassHolder(null),
                    createClient = { _, _ -> HttpClient(MockEngine { error("Use the detached transfer client") }) },
                ) { _, _, _ -> }
            owner.model.updateUser(UserInfo.EMPTY.copy(id = 11))
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val closed = CompletableDeferred<Unit>()
            var captures = 0
            val records = DownloadTestRecords()
            val files = createFileEngine()
            val namespace = "session-${Uuid.random().toHexString()}"
            val downloader =
                DownloaderImpl(
                    DownloadContext(namespace, this, records) {
                        captures++
                        captureDownloadSession(origin, owner) { url, snapshot, model ->
                            HttpClient(
                                MockEngine { request ->
                                    assertEquals("user_session=synthetic-first", request.headers[HttpHeaders.Cookie])
                                    assertEquals("11", request.headers["X-Account-Id"])
                                    started.complete(Unit)
                                    release.await()
                                    respond(CommunityFontTestData.primary)
                                },
                            ) {
                                defaultClientConfigure(snapshot, model, url)
                            }.also { client ->
                                client.coroutineContext[Job]?.invokeOnCompletion { closed.complete(Unit) }
                            }
                        }
                    },
                    SimpleTaskRegister(this),
                    files,
                )
            try {
                val file = fontFile(CommunityFontTestData.primary)
                downloader.download(file)
                started.await()
                owner.client.close()
                cookies.addCookie(Url(origin), Cookie("user_session", "synthetic-second", path = "/"))
                owner.model.updateUser(UserInfo.EMPTY.copy(id = 22))
                release.complete(Unit)
                assertNotNull(records.observeDocumentByFileId(file.id).first { it?.status == DownloadStatus.PROCESSED })
                closed.await()
                assertEquals(1, captures)
            } finally {
                owner.client.close()
                files.delete(files.location("public-resources-v1/$namespace"))
            }
        }

    private fun createSession(handler: MockRequestHandler): SimpleUserSessionManager =
        createSimpleUserSessionManager(
            "wss://fonts.invalid/ws",
            passHolder = ConstPassHolder(null),
            createClient = { _, _ -> HttpClient(MockEngine(handler)) },
        ) { _, _, _ -> }
}
