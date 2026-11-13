/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.app.utils.streamUploadedFile
import com.storyteller_f.shared.model.FileInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadedFileDownloadTest {
    @Test
    fun downloadWritesExactBytesAndClosesDestination() =
        runTest {
            val expected = ByteArray(150_000) { (it % 251).toByte() }
            val output = Buffer()
            var closed = false
            HttpClient(MockEngine { respond(expected) }).use { client ->
                streamUploadedFile(client, file(expected.size)) {
                    object : RawSink by output {
                        override fun close() {
                            closed = true
                        }
                    }
                }
            }
            assertContentEquals(expected, output.readByteArray())
            assertTrue(closed)
        }

    @Test
    fun httpFailureDoesNotOpenDestination() =
        runTest {
            var opened = false
            HttpClient(MockEngine { respond("missing", HttpStatusCode.NotFound) }).use { client ->
                assertFailsWith<IllegalStateException> {
                    streamUploadedFile(client, file(10)) {
                        opened = true
                        Buffer()
                    }
                }
            }
            assertFalse(opened)
        }

    @Test
    fun truncatedResponseFailsAndClosesDestination() =
        runTest {
            var closed = false
            val output = Buffer()
            HttpClient(MockEngine { respond(byteArrayOf(1, 2)) }).use { client ->
                assertFailsWith<IllegalStateException> {
                    streamUploadedFile(client, file(10)) {
                        object : RawSink by output {
                            override fun close() {
                                closed = true
                            }
                        }
                    }
                }
            }
            assertTrue(closed)
        }

    @Test
    fun cancellationPropagatesWithoutOpeningDestination() =
        runTest {
            var opened = false
            HttpClient(MockEngine { throw CancellationException("cancel download") }).use { client ->
                assertFailsWith<CancellationException> {
                    streamUploadedFile(client, file(10)) {
                        opened = true
                        Buffer()
                    }
                }
            }
            assertFalse(opened)
        }

    private fun file(size: Int) = FileInfo.EMPTY.copy(url = "https://fixture.invalid/file", size = size.toLong())
}
