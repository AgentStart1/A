/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.support.ui.FluentWait
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.time.Duration
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.assertEquals

internal fun verifyAndroidPredictiveBackCancellation(
    fromRight: Boolean,
    adb: (List<String>) -> String,
    screenshot: () -> ByteArray,
    output: File,
) {
    assertEquals(
        "2",
        adb(listOf("shell", "settings", "get", "secure", "navigation_mode")).trim(),
        "Predictive back regression requires gesture navigation",
    )
    val baselineBytes = screenshot()
    val baseline = ImageIO.read(ByteArrayInputStream(baselineBytes))
    val start = if (fromRight) baseline.width - 2 else 1
    val direction = if (fromRight) -1 else 1
    val y = baseline.height / 2
    val edge = if (fromRight) "right" else "left"
    fun motion(action: String, x: Int) {
        adb(listOf("shell", "input", "motionevent", action, x.toString(), y.toString()))
    }
    output.resolve("predictive-$edge-before.png").writeBytes(baselineBytes)
    try {
        motion("DOWN", start)
        repeat(10) { step -> motion("MOVE", start + direction * baseline.width * (step + 1) / 32) }
        val middle =
            FluentWait(screenshot).withTimeout(Duration.ofSeconds(5)).until { capture ->
                capture().takeIf { changedContentPixels(baseline, it) > 200 }
            }
        output.resolve("predictive-$edge-progress.png").writeBytes(middle)
    } finally {
        motion("CANCEL", start)
    }
    val restored =
        FluentWait(screenshot).withTimeout(Duration.ofSeconds(5)).until { capture ->
            capture().takeIf { changedContentPixels(baseline, it) < 100 }
        }
    output.resolve("predictive-$edge-cancel.png").writeBytes(restored)
}

private fun changedContentPixels(before: BufferedImage, bytes: ByteArray): Int {
    val after = ImageIO.read(ByteArrayInputStream(bytes))
    assertEquals(before.width, after.width)
    assertEquals(before.height, after.height)
    var changed = 0
    // Exclude system chrome and edge arrows: only page pixels can satisfy the assertion.
    for (y in before.height / 6 until before.height * 5 / 6) {
        for (x in before.width / 4 until before.width * 3 / 4) {
            val first = before.getRGB(x, y)
            val second = after.getRGB(x, y)
            if (listOf(0, 8, 16).any { shift ->
                    abs((first shr shift and 255) - (second shr shift and 255)) > 30
                }
            ) {
                changed++
            }
        }
    }
    return changed
}
