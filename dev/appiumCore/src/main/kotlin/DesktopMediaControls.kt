/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import io.appium.java_client.AppiumDriver
import io.appium.java_client.remote.AppiumWebElement
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.OutputType
import org.openqa.selenium.Point
import org.openqa.selenium.Rectangle
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Reads native controls and verifies the real composed frame, without a player test bridge. */
internal class DesktopMediaControls(private val driver: AppiumDriver, private val synchronizeWindow: () -> Unit) {
    private val wait
        get() =
            WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException::class.java)

    fun position(): Int {
        synchronizeWindow()
        val progress =
            driver.findElements(By.tagName("Text")).mapNotNull { it.getAttribute("Name") }
                .lastOrNull { it.matches(Regex("[0-9]+:[0-9]{2} / [0-9]+:[0-9]{2}")) }
                ?: throw org.openqa.selenium.NoSuchElementException("No playback time is available")
        val parts = progress.substringBefore(" / ").split(':').map(String::toInt)
        return parts[0] * 60 + parts[1]
    }

    fun assertProgressAfter(seconds: Int) {
        wait.until { position() > seconds }
    }

    fun assertPosition(range: IntRange) {
        wait.until { position() in range }
    }

    fun assertVideoFrame() {
        wait.until {
            synchronizeWindow()
            val slider = driver.findElement(By.name("Playback position")).rect
            val options = driver.findElement(By.name("Media options")).rect
            val bounds = Rectangle(Point(slider.x, options.y), Dimension(slider.width, slider.y - options.y))
            hasVideoTestPattern(driver.getScreenshotAs(OutputType.BYTES), bounds)
        }
    }

    fun seekNearEnd() {
        dragSlider("Playback position", 0.05, 0.9)
        assertPosition(50..57)
    }

    fun lowerVolume(): Double {
        dragSlider("Volume", 0.85, 0.25)
        var previous: Double? = null
        return wait.until {
            val current = volume()
            val stable = previous?.let { kotlin.math.abs(it - current) < 0.001 } == true
            previous = current
            current.takeIf { it in 0.1..0.4 && stable }
        }
    }

    fun assertVolume(expected: Double) {
        wait.until { kotlin.math.abs(volume() - expected) < 0.02 }
    }

    private fun volume(): Double {
        synchronizeWindow()
        val slider = findSlider("Volume")
        val elementId = requireNotNull((slider as AppiumWebElement).id)
        val handle = driver.windowHandle
        require(elementId.matches(Regex("[0-9]+(\\.[0-9]+)+")))
        require(handle.matches(Regex("0x[0-9a-fA-F]+")))
        val script =
            checkNotNull(javaClass.getResourceAsStream("/desktop-media-volume.ps1"))
                .bufferedReader().use { it.readText() }
                .replace("__WINDOW_HANDLE__", handle.removePrefix("0x"))
                .replace("__RUNTIME_ID__", elementId)
        val output = requireNotNull(driver.executeScript("powerShell", mapOf("script" to script)))
        return (output as String).toDouble()
    }

    private fun findSlider(name: String): org.openqa.selenium.WebElement =
        driver.findElements(By.tagName("Slider")).singleOrNull { it.getAttribute("Name") == name }
            ?: throw org.openqa.selenium.NoSuchElementException("No unique $name slider in the active window")

    private fun dragSlider(name: String, start: Double, end: Double) {
        var previous: Rectangle? = null
        val slider =
            wait.until {
                synchronizeWindow()
                val element = findSlider(name)
                val current = element.rect
                val stable = current == previous && current.width > 0 && element.isDisplayed
                previous = current
                element.takeIf { stable }
            }
        val bounds = slider.rect
        val elementId = requireNotNull((slider as AppiumWebElement).id)
        driver.executeScript(
            "windows: clickAndDrag",
            mapOf(
                "startElementId" to elementId,
                "endElementId" to elementId,
                "startX" to (bounds.width * start).toInt(),
                "startY" to bounds.height / 2,
                "endX" to (bounds.width * end).toInt(),
                "endY" to bounds.height / 2,
                "durationMs" to 500,
            ),
        )
    }
}
