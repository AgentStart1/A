/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.markdown

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class MermaidSvgImageLoaderTest {
    @Test
    fun defaultImageLoaderDecodesMermaidSvgBytesWithoutAnExplicitDecoderFactory() =
        runComposeUiTest {
            var platformContext: PlatformContext? = null
            setContent {
                val context = LocalPlatformContext.current
                SideEffect { platformContext = context }
            }
            awaitIdle()

            val context = requireNotNull(platformContext)
            val imageLoader = ImageLoader.Builder(context).build()
            try {
                val svg = renderMermaidSvg(MERMAID_SOURCE)
                val result =
                    imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(svg.encodeToByteArray())
                            .size(Size.ORIGINAL)
                            .build(),
                    )
                val image = assertIs<SuccessResult>(result).image

                assertTrue(image.width > 0, "Decoded SVG width must be positive")
                assertTrue(image.height > 0, "Decoded SVG height must be positive")
            } finally {
                imageLoader.shutdown()
            }
        }
}
