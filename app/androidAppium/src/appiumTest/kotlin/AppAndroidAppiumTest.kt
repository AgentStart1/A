/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test

class AppAndroidAppiumTest :
    AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>(
        AppAppiumHelper(),
        AndroidAppiumHelper(),
    ) {
    @Test
    fun `test video network error recovers after user retry`() = testAndroidMediaRetry(PreviewFile.VIDEO)

    @Test
    fun `test audio network error recovers after user retry`() = testAndroidMediaRetry(PreviewFile.AUDIO)

    @Test
    fun `test audio volume restores after native mute`() = testAndroidAudioVolume()

    @Test
    fun `test audio seeks and replays after native playback ends`() = testAndroidAudioReplay()

    @Test
    fun `test video playlist selection resumes native playback`() = testAndroidVideoPlaylist()

    @Test
    fun `test video expansion preserves native frames and playback position`() = testAndroidVideoExpansion()

    @Test
    fun `test native download cancellation returns to file preview`() = testNativeDownloadCancellation()

    @Test
    fun `test open source libraries shows bundled dependencies`() = testProjectLibraries()

    @Test
    fun `test playing video enters picture in picture and restores playback`() = testAndroidPip(paused = false)

    @Test
    fun `test topic video renders inline attachment`() = testAndroidInlineVideo()

    @Test
    fun `test paused video stays out of picture in picture`() = testAndroidPip(paused = true)

    @Test
    fun `test image fullscreen preview from topic attachment`() = testUploadedPreview(PreviewFile.IMAGE, topic = true)

    @Test
    fun `test image fullscreen preview from uploaded file list`() =
        testUploadedPreview(
            PreviewFile.IMAGE,
            topic = false,
        )

    @Test
    fun `test audio fullscreen preview from topic attachment`() = testUploadedPreview(PreviewFile.AUDIO, topic = true)

    @Test
    fun `test audio fullscreen preview from uploaded file list`() =
        testUploadedPreview(
            PreviewFile.AUDIO,
            topic = false,
        )

    @Test
    fun `test video fullscreen preview from topic attachment`() = testUploadedPreview(PreviewFile.VIDEO, topic = true)

    @Test
    fun `test video fullscreen preview from uploaded file list`() =
        testUploadedPreview(
            PreviewFile.VIDEO,
            topic = false,
        )

    @Test
    fun `test pdf fullscreen preview from topic attachment`() = testUploadedPreview(PreviewFile.PDF, topic = true)

    @Test
    fun `test pdf fullscreen preview from uploaded file list`() = testUploadedPreview(PreviewFile.PDF, topic = false)

    @Test
    fun `test unsupported file shows download details from topic attachment`() =
        testUploadedPreview(
            PreviewFile.BINARY,
            topic = true,
        )

    @Test
    fun `test unsupported file shows download details from uploaded file list`() =
        testUploadedPreview(
            PreviewFile.BINARY,
            topic = false,
        )

    @Test
    fun `test protected storage write failure rejects persistent login`() = testAndroidProtectedStorageFailure()

    @Test
    fun `test child switch remains process local when protected writes fail`() = testAndroidChildStorageFailure()

    @Test
    fun `test community exit and rejoin persist membership`() = testCommunityMembershipByHelper()

    @Test
    fun `test topic comment persists author parent and reentry`() = testTopicCommentByHelper()

    @Test
    fun `test invalid private key retains guest after restart`() = testInvalidKeyByHelper()

    @Test
    fun `test unknown private key rejects login and retains guest after restart`() = testUnknownKeyByHelper()

    @Test
    fun `test signed out state survives restart`() = testSignoutPersistenceByHelper()

    @Test
    fun `test child switch is process local and restart restores parent`() = testChildSigninByHelper()

    @Test
    fun `test sign in survives restart and revocation requires explicit login`() = testSigninLifecycleByHelper()

    @Test
    fun `test sign up identity and session survive restart`() = testSignUpByHelper()

    @Test
    fun `test sign in as system user`() = testSignInAsSystemUserByHelper()

    @Test
    fun `test sign in by injected private session`() = testInjectedSessionByHelper()

    @Test
    fun `test publish topic in user space`() = testPublishTopicInUserSpaceByHelper()

    @Test
    fun `test favorite topic from topic page`() = testFavoriteTopicByHelper()

    @Test
    fun `test opens asciidoc preview`() = testOpenAsciidocPreviewByHelper()

    @Test
    fun `test subscribe topic from community page`() = testSubscribeTopicByHelper()

    @Test
    fun `test community favorite toggle persists across navigation`() = testCommunityFavoriteByHelper()

    @Test
    fun `test community subscription toggle persists across navigation`() = testCommunitySubscriptionByHelper()

    @Test
    fun `test community member opens the selected profile`() = testCommunityMemberByHelper()

    @Test
    fun `test publish topic in joined community`() = testPublishTopicInCommunityByHelper()

    @Test
    fun `test wide activity context opens room and pinned topic`() = testActivityContextByHelper()

    @Test
    fun `test reading activity clears unread`() = testActivityReadByHelper()

    @Test
    fun `test sending updates persisted activity preview`() = testActivitySendByHelper()

    @Test
    fun `test activity room navigation preserves latest message`() = testActivityNavigationByHelper()

    @Test
    fun `test private room reading decrypts preview and clears unread`() = testActivityReadByHelper(privateRoom = true)

    @Test
    fun `test private room sending updates decrypted preview`() = testActivitySendByHelper(privateRoom = true)

    @Test
    fun `test room refresh clears unread after another session reads`() = testRoomActivityRefreshByHelper()

    @Test
    fun `test publish topic in community room`() = testPublishTopicInCommunityRoomByHelper()
}
