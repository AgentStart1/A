/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Rectangle
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidVideoFrameTest {
    @Test
    fun acceptsDecodedRgbPatternWithinPlayerBounds() {
        val image = patternImage()
        assertTrue(hasVideoTestPattern(image.png(), Rectangle(0, 0, 96, 96)))
    }

    @Test
    fun acceptsPatternUnderNativeControlsOverlay() {
        val image = BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB)
        val colors = listOf(Color(100, 0, 0), Color(0, 100, 0), Color(0, 0, 100))
        for (x in 0 until 96) for (y in 0 until 96) image.setRGB(x, y, colors[x / 32].rgb)
        assertTrue(hasVideoTestPattern(image.png(), Rectangle(0, 0, 96, 96)))
    }

    @Test
    fun rejectsBlackPlayerWithColorfulLauncherOutside() {
        val image = patternImage()
        for (x in 24 until 72) for (y in 24 until 72) image.setRGB(x, y, Color.BLACK.rgb)
        assertFalse(hasVideoTestPattern(image.png(), Rectangle(24, 24, 48, 48)))
    }

    @Test
    fun rejectsSolidColorPlaceholder() {
        val image = BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until 96) for (y in 0 until 96) image.setRGB(x, y, Color.BLUE.rgb)
        assertFalse(hasVideoTestPattern(image.png(), Rectangle(0, 0, 96, 96)))
    }

    @Test
    fun rejectsBoundsOutsideScreenshot() {
        assertFalse(hasVideoTestPattern(patternImage().png(), Rectangle(90, 90, 20, 20)))
    }

    @Test
    fun rejectsEmptyPlayerBounds() {
        assertFalse(hasVideoTestPattern(patternImage().png(), Rectangle(0, 0, 0, 0)))
    }

    private fun patternImage(): BufferedImage =
        BufferedImage(96, 96, BufferedImage.TYPE_INT_RGB).apply {
            val colors = listOf(Color.RED, Color.GREEN, Color.BLUE)
            for (x in 0 until width) for (y in 0 until height) setRGB(x, y, colors[x / 32].rgb)
        }

    private fun BufferedImage.png(): ByteArray {
        val bytes = ByteArrayOutputStream()
        BufferedOutputStream(bytes).use { assertTrue(ImageIO.write(this, "png", it)) }
        return bytes.toByteArray()
    }
}
