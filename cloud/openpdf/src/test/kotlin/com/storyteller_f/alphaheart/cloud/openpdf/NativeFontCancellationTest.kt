/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeFontCancellationTest {
    @Test
    fun `job cancellation waits for native cleanup`() =
        runTest {
            // The native call and its deadline use real dispatchers, independent of runTest's virtual clock.
            withContext(Dispatchers.Default) {
                val native = GatedHarfBuzzCalls()
                val repository = FontInstanceRepository(HarfBuzzFontInstancer(native))
                val source = fontBytes("cff2-multiaxis.otf")
                val computed = CompletableDeferred<CustomResult<FontInstance, FontError>>()
                val worker =
                    async(Dispatchers.IO) {
                        repository.getInstance(source, axes = mapOf("wght" to 700f, "opsz" to 8f))
                            .also { computed.complete(it) }.getOrThrow()
                    }
                try {
                    withTimeout(30_000) { native.entered.await() }
                    assertTrue(native.inSubset.value)
                    val cancellation = CancellationException("Cancel during native CFF2 subsetting")
                    worker.cancel(cancellation)
                    assertTrue(worker.isCancelled)
                    assertFalse(worker.isCompleted)
                    assertTrue(native.releasing.value.isEmpty())
                    native.release.complete(Unit)
                    val failure = assertFailsWith<CancellationException> { worker.await() }
                    assertTrue(generateSequence<Throwable>(failure) { it.cause }.any { it === cancellation })
                    assertTrue(computed.isCompleted)
                    val instance = computed.await().getOrThrow()
                    assertTrue(SfntFont(instance.copyBytes(), 0).hasTable("CFF "))
                    val allocated = native.allocated.value
                    assertEquals(6, allocated.size)
                    assertEquals(allocated.indices.reversed().toList(), native.releasing.value)
                    assertEquals(allocated.indices.toSet(), native.released.value.toSet())
                    assertEquals(6, native.released.value.size)
                } finally {
                    native.release.complete(Unit)
                    withContext(NonCancellable) { worker.cancelAndJoin() }
                }
            }
        }
}
