/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidPip(paused: Boolean) =
    withUploadedFile(PreviewFile.VIDEO, topic = false) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedPreview(driver, data)
        driver.withAnimatedMediaControls {
            driver.clickByDescription("Play")
            driver.assertVideoPlaying(data.file.name)
            if (paused) driver.pauseVideo(data.file.name)
            val snapshots =
                if (paused) {
                    listOf("system-picture-in-picture")
                } else {
                    listOf("system-picture-in-picture", "picture-in-picture-reentered")
                }
            for (snapshot in snapshots) {
                driver.pressHomeAndAssertPip(targetHelper.androidApp.packageName, expected = !paused)
                if (!paused) driver.assertVideoFrame(targetHelper.androidApp.packageName, data.file.name)
                driver.saveSnapshot(snapshot)
                driver.restoreFromPip(targetHelper.androidApp.packageName)
                driver.assertVisibleByText(data.file.name)
                if (!paused) driver.assertVideoPlaying(data.file.name)
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidInlineVideo() =
    withUploadedFile(PreviewFile.VIDEO, topic = true) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedTopic(driver, data)
        driver.assertVisibleByText("Preview: ${data.file.name}")
        driver.withAnimatedMediaControls {
            driver.clickByDescription("Play")
            driver.assertVideoPlaying(data.file.name)
            driver.assertVisibleByText(requireNotNull(data.topicMarker))
            driver.saveSnapshot("topic-inline-video-playing")
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidVideoExpansion() =
    withUploadedFile(PreviewFile.VIDEO, topic = false) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedPreview(driver, data)
        driver.withAnimatedMediaControls {
            driver.clickByDescription("Play")
            driver.assertVideoPlaying(data.file.name)
            driver.assertPlaybackAdvancesAfter(2)
            val before = driver.playbackPositionSeconds()
            driver.clickByDescription("Expand player")
            driver.assertVisibleByDescription("Pause")
            driver.assertExpandedVideoFrame()
            driver.assertPlaybackAdvancesAfter(before)
            driver.saveSnapshot("expanded-native-video")
            val expanded = driver.playbackPositionSeconds()
            driver.clickByDescription("Exit expanded player")
            driver.assertVideoPlaying(data.file.name)
            driver.assertPlaybackAdvancesAfter(expanded)
            driver.saveSnapshot("restored-native-video")
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidVideoPlaylist() =
    withUploadedFile(PreviewFile.VIDEO, topic = false) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedPreview(driver, data)
        driver.withAnimatedMediaControls {
            driver.clickByDescription("Play")
            driver.assertVideoPlaying(data.file.name)
            driver.pauseVideo(data.file.name)
            driver.clickByDescription("Media options")
            driver.clickByText("Playlist")
            driver.assertVisibleByDescription("play")
            driver.saveSnapshot("native-video-playlist")
            driver.clickByDescription("play")
            driver.assertVideoPlaying(data.file.name)
            driver.saveSnapshot("native-video-playlist-selected")
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidAudioReplay() =
    withUploadedFile(PreviewFile.AUDIO, topic = false) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedPreview(driver, data)
        driver.withAnimatedMediaControls {
            driver.clickByDescription("Play")
            driver.assertPlaybackAdvancesAfter(1)
            driver.clickByDescription("Pause")
            driver.seekPlaybackNearEnd()
            driver.clickByDescription("Play")
            driver.assertVisibleByDescription("Pause")
            driver.assertVisibleByDescription("Play")
            driver.saveSnapshot("native-audio-ended")
            driver.clickByDescription("Play")
            driver.assertPlaybackRestarted()
            driver.assertVisibleByDescription("Pause")
            driver.saveSnapshot("native-audio-replayed")
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidAudioVolume() =
    withUploadedFile(PreviewFile.AUDIO, topic = false) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedPreview(driver, data)
        driver.withAnimatedMediaControls {
            driver.clickByDescription("Play")
            driver.assertPlaybackAdvancesAfter(1)
            driver.clickByDescription("Volume")
            val volume = driver.lowerPlaybackVolume()
            driver.clickByText("Mute")
            driver.clickByDescription("Volume")
            driver.assertVisibleByText("Unmute")
            driver.assertPlaybackVolume(0f)
            driver.clickByText("Unmute")
            driver.clickByDescription("Volume")
            driver.assertPlaybackVolume(volume)
            driver.saveSnapshot("native-audio-volume-restored")
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidMediaRetry(kind: PreviewFile) =
    withUploadedFile(kind, topic = false) { scope, data ->
        val driver = scope.driver as AndroidAppTestDriver
        openUploadedPreview(driver, data)
        driver.withAnimatedMediaControls {
            driver.withFailedMediaResponse(data.bytes) { restore ->
                driver.clickByDescription("Play")
                driver.assertVisibleByText("Retry")
                driver.saveSnapshot("native-media-network-error")
                restore()
                driver.clickByText("Retry")
                if (kind == PreviewFile.VIDEO) driver.assertVideoPlaying(data.file.name)
                driver.assertPlaybackAdvancesAfter(1)
                driver.assertVisibleByDescription("Pause")
                driver.saveSnapshot("native-media-network-recovered")
            }
        }
    }
