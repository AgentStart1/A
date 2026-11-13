/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.shared.utils

import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.CryptoError
import com.storyteller_f.shared.model.captureCrypto
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

internal class CryptoErrorTest {
    @Test
    fun `crypto capture propagates cancellation`() {
        val cancellation = CancellationException("cancelled")
        assertSame(
            cancellation,
            assertFailsWith<CancellationException> {
                captureCrypto { throw cancellation }
            },
        )
    }

    @Test
    fun `crypto invalid input retains cause`() {
        val invalid = IllegalArgumentException("invalid key")
        val result = captureCrypto { throw invalid }
        val error = assertIs<CustomResult.Failure<CryptoError>>(result).error
        assertSame(invalid, assertIs<CryptoError.InvalidInput>(error).cause)
    }
}
