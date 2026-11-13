/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import java.net.InetSocketAddress

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testDesktopVideoContinuity() =
    withUploadedFile(PreviewFile.VIDEO, topic = false) { scope, data ->
        val driver = scope.driver as DesktopAppTestDriver
        openUploadedPreview(driver, data)
        driver.assertVisibleByDescription("Pause")
        driver.media.assertProgressAfter(1)
        driver.media.assertVideoFrame()
        driver.clickByDescription("Pause")
        driver.assertVisibleByDescription("Play")
        val paused = driver.media.position()
        driver.clickByDescription("Expand player")
        driver.assertVisibleByDescription("Exit expanded player")
        driver.assertVisibleByDescription("Play")
        driver.media.assertPosition(paused..paused + 1)
        driver.media.assertVideoFrame()
        driver.saveSnapshot("desktop-video-expanded-paused")
        driver.clickByDescription("Play")
        driver.media.assertProgressAfter(paused)
        val expanded = driver.media.position()
        driver.clickByDescription("Exit expanded player")
        driver.assertVisibleByDescription("Pause")
        driver.media.assertProgressAfter(expanded)
        driver.media.assertVideoFrame()
        driver.saveSnapshot("desktop-video-restored-playing")
    }

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testDesktopMediaReplay(kind: PreviewFile) =
    withUploadedFile(kind, topic = false) { scope, data ->
        val driver = scope.driver as DesktopAppTestDriver
        openUploadedPreview(driver, data)
        if (kind == PreviewFile.AUDIO) {
            driver.assertVisibleByDescription("Play")
            driver.clickByDescription("Play")
        }
        driver.media.assertProgressAfter(1)
        driver.clickByDescription("Pause")
        driver.assertVisibleByDescription("Play")
        driver.media.seekNearEnd()
        driver.clickByDescription("Play")
        driver.assertVisibleByDescription("Pause")
        driver.assertVisibleByDescription("Play")
        driver.saveSnapshot("desktop-media-ended")
        driver.clickByDescription("Play")
        driver.media.assertPosition(1..10)
        driver.assertVisibleByDescription("Pause")
        if (kind == PreviewFile.VIDEO) driver.media.assertVideoFrame()
        driver.saveSnapshot("desktop-media-replayed")
    }

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testDesktopMediaVolume(kind: PreviewFile) =
    withUploadedFile(
        kind,
        topic = false,
        fixtureName = if (kind == PreviewFile.VIDEO) "sample-with-audio.mp4" else kind.filename,
    ) { scope, data ->
        val driver = scope.driver as DesktopAppTestDriver
        openUploadedPreview(driver, data)
        if (kind == PreviewFile.AUDIO) driver.clickByDescription("Play")
        driver.media.assertProgressAfter(1)
        driver.clickByDescription("Volume")
        val volume = driver.media.lowerVolume()
        driver.clickByText("Mute")
        driver.clickByDescription("Volume")
        driver.assertVisibleByText("Unmute")
        driver.media.assertVolume(0.0)
        driver.clickByText("Unmute")
        driver.clickByDescription("Volume")
        driver.media.assertVolume(volume)
        driver.saveSnapshot("desktop-media-volume-restored")
    }

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testDesktopMediaRetry(kind: PreviewFile) {
    val available = CompletableDeferred<Unit>()
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val bytes =
        checkNotNull(PreviewFile::class.java.getResourceAsStream("/media/${kind.filename}"))
            .buffered().use { it.readBytes() }
    try {
        server.createContext("/") { exchange ->
            exchange.use {
                if (available.isCompleted) {
                    it.responseHeaders.add("Content-Type", kind.contentType)
                    it.sendResponseHeaders(200, bytes.size.toLong())
                    it.responseBody.buffered().use { output -> output.write(bytes) }
                } else {
                    it.sendResponseHeaders(503, -1)
                }
            }
        }
        server.start()
        val platform = DesktopAppiumHelper(filesystemPublicUrl = "http://127.0.0.1:${server.address.port}")
        withUploadedFile(kind, topic = false, platform = platform) { scope, data ->
            val driver = scope.driver as DesktopAppTestDriver
            openUploadedPreview(driver, data)
            if (kind == PreviewFile.AUDIO) driver.clickByDescription("Play")
            driver.assertVisibleByText("Retry")
            driver.saveSnapshot("desktop-media-network-error")
            available.complete(Unit)
            driver.clickByText("Retry")
            driver.assertVisibleByDescription("Pause")
            driver.media.assertProgressAfter(1)
            if (kind == PreviewFile.VIDEO) driver.media.assertVideoFrame()
            driver.saveSnapshot("desktop-media-network-recovered")
        }
    } finally {
        server.stop(0)
    }
}
