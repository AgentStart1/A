/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.svg.SvgDecoder
import com.storyteller_f.result.getOrThrow
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class BrowserImageCropEncodingTest {
    @Test
    fun decodedSvgCanBeCroppedAndEncodedToRealPngBytesWithoutAFileSystem() =
        runComposeUiTest {
            var capturedContext: PlatformContext? = null
            setContent {
                val context = LocalPlatformContext.current
                SideEffect { capturedContext = context }
            }
            awaitIdle()
            val context = assertNotNull(capturedContext)
            val loader = ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
            try {
                val svg =
                    """
                    <svg xmlns="http://www.w3.org/2000/svg" width="40" height="20">
                      <rect width="40" height="20" fill="#ff0000"/>
                    </svg>
                    """.trimIndent()
                val response =
                    loader.execute(
                        ImageRequest.Builder(context).data(svg.encodeToByteArray()).size(Size.ORIGINAL).build(),
                    )
                val source = assertIs<SuccessResult>(response).image.coilImageToImageBitmap().getOrThrow()
                assertEquals(40, source.width)
                assertEquals(20, source.height)
                val cropped = ImageBitmap(20, 20)
                Canvas(cropped).drawImage(source, Offset(-10f, 0f), Paint())
                val bytes = encodeImageBitmap(cropped, ImageFormat.PNG).getOrThrow()
                assertTrue(bytes.size > 8)
                assertContentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10), bytes.copyOf(8))
                val decoded = Image.makeFromEncoded(bytes)
                try {
                    assertEquals(20, decoded.width)
                    assertEquals(20, decoded.height)
                    val pixel = decoded.toComposeImageBitmap().toPixelMap()[10, 10]
                    assertEquals(1f, pixel.red)
                    assertEquals(0f, pixel.green)
                    assertEquals(0f, pixel.blue)
                    assertEquals(1f, pixel.alpha)
                } finally {
                    decoded.close()
                }
            } finally {
                loader.shutdown()
            }
        }
}
