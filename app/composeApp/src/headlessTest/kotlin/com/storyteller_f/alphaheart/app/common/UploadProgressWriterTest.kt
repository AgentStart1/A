/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadProgressWriterTest {
    @Test
    fun smallBuffersProduceBoundedDatabaseWrites() =
        runTest {
            val written = mutableListOf<Long>()
            val total = 10L * 1024 * 1024
            val progress = UploadProgressWriter(total) { written.add(it) }
            for (sent in 1024L..total step 1024) progress.update(sent)
            assertTrue(written.size <= 41, "Transport buffers must not each become a database transaction")
            assertEquals(total, written.last())
            assertTrue(written.zipWithNext().all { (before, after) -> after > before })
        }

    @Test
    fun smallFilePersistsExactEndOnce() =
        runTest {
            val written = mutableListOf<Long>()
            val progress = UploadProgressWriter(8192) { written.add(it) }
            progress.update(1024)
            progress.update(8192)
            progress.update(9000)
            assertEquals(listOf(8192L), written)
        }
}
