/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.maxBitmapSize
import coil3.size.Size
import com.storyteller.alphaheart.client.composecore.markdown.MERMAID_SOURCE
import com.storyteller.alphaheart.client.composecore.markdown.renderMermaidSvg
import com.storyteller_f.alphaheart.client.compose_core.utils.addPlatformImageDecoders
import com.storyteller_f.alphaheart.client.compose_core.utils.coilImageToImageBitmap
import com.storyteller_f.result.getOrThrow
import org.jetbrains.skia.FontMgr
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class SvgTextImageLoaderTest {
    @Test
    fun browserSansSerifSvgTextProducesDarkGlyphPixels() = assertSvgTextRenders("sans-serif")

    @Test
    fun browserRobotoSvgTextProducesDarkGlyphPixels() = assertSvgTextRenders("Roboto")

    @Test
    fun mermaidFontStackSvgTextProducesDarkGlyphPixels() =
        assertSvgTextRenders("Inter, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif")

    @Test
    fun generatedMermaidLabelsProduceRasterPixels() =
        runComposeUiTest {
            val context = capturePlatformContext()
            val imageLoader = ImageLoader.Builder(context).addPlatformImageDecoders().build()
            try {
                val svg = renderMermaidSvg(MERMAID_SOURCE)
                val svgWithoutText =
                    Regex("<text\\b[^>]*>.*?</text>", setOf(RegexOption.DOT_MATCHES_ALL)).replace(svg, "")
                assertTrue(svgWithoutText != svg, "Generated Mermaid SVG must include text elements")

                val rendered = imageLoader.decodeSvg(context, svg)
                val withoutText = imageLoader.decodeSvg(context, svgWithoutText)
                assertTrue(
                    countDifferentPixels(rendered, withoutText) >= MIN_DARK_GLYPH_PIXELS,
                    "The actual Mermaid SVG should rasterize its text differently from a text-stripped copy",
                )
            } finally {
                imageLoader.shutdown()
            }
        }

    @Test
    fun customMaxBitmapSizeCapsSvgRasterDimensions() =
        runComposeUiTest {
            val context = capturePlatformContext()
            val imageLoader = ImageLoader.Builder(context).addPlatformImageDecoders().build()
            try {
                val image =
                    imageLoader.decodeSvg(
                        context = context,
                        svg = svgWithText("Roboto", width = 2_000, height = 1_000),
                        maxBitmapSize = Size(120, 80),
                    )
                assertTrue(image.width <= 120, "Raster width ${image.width} exceeded the 120px request cap")
                assertTrue(image.height <= 80, "Raster height ${image.height} exceeded the 80px request cap")
            } finally {
                imageLoader.shutdown()
            }
        }

    private fun assertSvgTextRenders(fontFamily: String) =
        runComposeUiTest {
            val context = capturePlatformContext()
            val imageLoader = ImageLoader.Builder(context).addPlatformImageDecoders().build()
            try {
                val svg = svgWithText(fontFamily)
                val result =
                    imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(svg.encodeToByteArray())
                            .size(Size.ORIGINAL)
                            .build(),
                    )
                val diagnostic = (result as? ErrorResult)?.throwable?.stackTraceToString()
                val composeImage =
                    assertIs<SuccessResult>(result, diagnostic).image.coilImageToImageBitmap().getOrThrow()
                val pixels = composeImage.toPixelMap()
                val darkGlyphPixels =
                    (8 until 180).sumOf { x ->
                        (8 until 55).count { y ->
                            val color = pixels[x, y]
                            color.alpha > 0.5f && color.red < 0.2f && color.green < 0.2f && color.blue < 0.2f
                        }
                    }
                val availableFamilies =
                    (0 until FontMgr.default.familiesCount).joinToString { index ->
                        FontMgr.default.getFamilyName(index)
                    }
                assertTrue(
                    darkGlyphPixels >= MIN_DARK_GLYPH_PIXELS,
                    "Expected rendered SVG text pixels for font-family '$fontFamily'; " +
                        "found $darkGlyphPixels dark pixels. " +
                        "FontMgr.default has ${FontMgr.default.familiesCount} families: [$availableFamilies]",
                )
            } finally {
                imageLoader.shutdown()
            }
        }

    private fun svgWithText(fontFamily: String, width: Int = 200, height: Int = 60): String =
        """
        <svg xmlns="http://www.w3.org/2000/svg" width="$width" height="$height">
          <rect width="$width" height="$height" fill="#ffffff"/>
          <text x="10" y="40" font-family="$fontFamily" font-size="24" fill="#000000">Alpha</text>
        </svg>
        """.trimIndent()

    private companion object {
        const val MIN_DARK_GLYPH_PIXELS = 20
    }
}

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
private suspend fun ComposeUiTest.capturePlatformContext(): PlatformContext {
    var platformContext: PlatformContext? = null
    setContent {
        val context = LocalPlatformContext.current
        SideEffect { platformContext = context }
    }
    awaitIdle()
    return requireNotNull(platformContext)
}

private suspend fun ImageLoader.decodeSvg(
    context: PlatformContext,
    svg: String,
    maxBitmapSize: Size? = null,
): ImageBitmap {
    val request = ImageRequest.Builder(context).data(svg.encodeToByteArray()).size(Size.ORIGINAL)
    maxBitmapSize?.let(request::maxBitmapSize)
    return assertIs<SuccessResult>(execute(request.build())).image.coilImageToImageBitmap().getOrThrow()
}

private fun countDifferentPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertTrue(first.width == second.width && first.height == second.height, "SVG comparison images must match size")
    val firstPixels = first.toPixelMap()
    val secondPixels = second.toPixelMap()
    return (0 until first.width).sumOf { x ->
        (0 until first.height).count { y ->
            val a = firstPixels[x, y]
            val b = secondPixels[x, y]
            kotlin.math.abs(a.red - b.red) > 0.05f ||
                kotlin.math.abs(a.green - b.green) > 0.05f ||
                kotlin.math.abs(a.blue - b.blue) > 0.05f ||
                kotlin.math.abs(a.alpha - b.alpha) > 0.05f
        }
    }
}
