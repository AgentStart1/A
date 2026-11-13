/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Rectangle
import java.awt.Color
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import java.awt.Rectangle as ImageRectangle

/** Checks the uploaded RGB test pattern inside a verified native player rectangle. */
internal fun hasVideoTestPattern(png: ByteArray, bounds: Rectangle): Boolean {
    val image = BufferedInputStream(ByteArrayInputStream(png)).use { requireNotNull(ImageIO.read(it)) }
    val playerBounds = ImageRectangle(bounds.x, bounds.y, bounds.width, bounds.height)
    if (!ImageRectangle(image.width, image.height).contains(playerBounds)) return false
    val channels =
        (1 until FRAME_GRID_SEGMENTS).flatMap { row ->
            (1 until FRAME_GRID_SEGMENTS).mapNotNull { column ->
                val x = bounds.x + bounds.width * column / FRAME_GRID_SEGMENTS
                val y = bounds.y + bounds.height * row / FRAME_GRID_SEGMENTS
                Color(image.getRGB(x, y)).dominantVideoChannel()
            }
        }
    return channels.size >= MIN_COLORED_SAMPLES && channels.toSet().size == RGB_CHANNEL_COUNT
}

private fun Color.dominantVideoChannel(): Int? {
    val highest = maxOf(red, green, blue)
    if (highest < MIN_CHANNEL_LIGHT || highest - minOf(red, green, blue) < MIN_CHANNEL_CONTRAST) return null
    return when (highest) {
        red -> 0
        green -> 1
        else -> 2
    }
}

private const val FRAME_GRID_SEGMENTS = 8
private const val MIN_COLORED_SAMPLES = 12
private const val RGB_CHANNEL_COUNT = 3
private const val MIN_CHANNEL_LIGHT = 64
private const val MIN_CHANNEL_CONTRAST = 40
