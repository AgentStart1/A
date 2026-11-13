/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.storyteller_f.alphaheart.client.compose_core.utils.addPlatformImageDecoders
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class AvifDecoderTest {
    @Test
    internal fun decodesAvifWithBrowserImageDecoder() = runTest { verifyAvifDecode() }

    private suspend fun verifyAvifDecode() {
        val context = PlatformContext.INSTANCE
        val imageLoader = ImageLoader.Builder(context).addPlatformImageDecoders().build()
        try {
            val request =
                ImageRequest.Builder(context)
                    .data(Base64.decode(TWO_PIXEL_AVIF))
                    .build()

            val result = imageLoader.execute(request)
            if (result is ErrorResult) throw result.throwable
            val successResult = assertIs<SuccessResult>(result)

            assertEquals(2, successResult.image.width)
            assertEquals(2, successResult.image.height)
        } finally {
            imageLoader.shutdown()
        }
    }

    private companion object {
        const val TWO_PIXEL_AVIF =
            "AAAAIGZ0eXBhdmlmAAAAAGF2aWZtaWYxbWlhZk1BMUEAAADybWV0YQAAAAAAAAAoaGRscgAAAAAAAAAAcGljdAAAAAAAAAAAAAAA" +
                "AGxpYmF2aWYAAAAADnBpdG0AAAAAAAEAAAAeaWxvYwAAAABEAAABAAEAAAABAAABGgAAABkAAAAoaWluZgAAAAAAAQAAABppbmZl" +
                "AgAAAAABAABhdjAxQ29sb3IAAAAAamlwcnAAAABLaXBjbwAAABRpc3BlAAAAAAAAAAIAAAACAAAAEHBpeGkAAAAAAwgICAAAAAxh" +
                "djFDgSAAAAAAABNjb2xybmNseAABAAIABoAAAAAXaXBtYQAAAAAAAAABAAEEAQKDBAAAACFtZGF0EgAKBzgANhAQIGkyDB+QP///" +
                "xAAArLK+CQ=="
    }
}
