/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test

/** Explicit Windows integration suite; ordinary desktop suites retain their platform contract. */
class WindowsMediaAppiumTest :
    AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>(
        AppAppiumHelper(),
        DesktopAppiumHelper(),
    ) {
    @Test
    fun testVideoPauseAndExpansionContinuity() = testDesktopVideoContinuity()

    @Test
    fun testVideoSeekEndAndReplay() = testDesktopMediaReplay(PreviewFile.VIDEO)

    @Test
    fun testAudioSeekEndAndReplay() = testDesktopMediaReplay(PreviewFile.AUDIO)

    @Test
    fun testVideoMuteRestoresSelectedVolume() = testDesktopMediaVolume(PreviewFile.VIDEO)

    @Test
    fun testAudioMuteRestoresSelectedVolume() = testDesktopMediaVolume(PreviewFile.AUDIO)

    @Test
    fun testVideoNetworkErrorRecoversAfterRetry() = testDesktopMediaRetry(PreviewFile.VIDEO)

    @Test
    fun testAudioNetworkErrorRecoversAfterRetry() = testDesktopMediaRetry(PreviewFile.AUDIO)
}
