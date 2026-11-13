/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrElse
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration
import kotlin.test.Test

@RunWith(Parameterized::class)
class WasmAppiumTest(private val browserType: WasmBrowser) :
    AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>(
        AppAppiumHelper(),
        WasmAppiumHelper(
            resolveWasmDistribution(),
            By.cssSelector("[aria-label='avatar']"),
            browserType,
        ),
    ) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun browsers() = WasmBrowser.entries
    }

    @Test
    fun `test open source libraries shows bundled dependencies`() = testProjectLibraries()

    @Test
    fun `test project menu requests repository and removes obsolete links`() = testProjectRepository()

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
    fun `test download from topic attachment saves exact bytes`() = testUploadedDownload(topic = true)

    @Test
    fun `test download from uploaded file list saves exact bytes`() = testUploadedDownload(topic = false)

    @Test
    fun `test file picker upload persists exact bytes owner and restart record`() = testClientFileUpload()

    @Test
    fun `test file picker quota failure stores no file or consumed quota`() = testClientFileQuota()

    @Test
    fun `test file upload progress pauses and resumes without replaying stored chunks`() = testClientUploadResume()

    @Test
    fun `test cancelling partial file upload releases quota and survives restart`() = testClientUploadCancel()

    @Test
    fun `test room search uses query and preserves server order`() = testClientSearchOrder()

    @Test
    fun `test room search continues opaque token and refresh preserves anchor`() = testClientSearchPagingRefresh()

    @Test
    fun `test community creation persists owner policy and membership after restart`() = testCommunityCreation()

    @Test
    fun `test community exit and rejoin persist membership`() = testCommunityMembershipByHelper()

    @Test
    fun `test topic comment persists author parent and reentry`() = testTopicCommentByHelper()

    @Test
    fun `test invited notification recipient reads but cannot reply over HTTP or WS`() = testNotificationPermissions()

    @Test
    fun `test invalid private key retains guest after restart`() = testInvalidKeyByHelper()

    @Test
    fun `test unknown private key rejects login and retains guest after restart`() = testUnknownKeyByHelper()

    @Test
    fun `test two clients present RTC video propagate mute and release capture`() = testTwoClientRtc()

    @Test
    fun `test leaving RTC before joining never captures media`() = testCancelRtcBeforeJoining()

    @Test
    fun `test leaving RTC during capture releases late media`() = testCancelRtcDuringCapture()

    @Test
    fun `test readonly user retains room reads and cannot write over HTTP or WS`() = testReadOnlyClientPermissions()

    @Test
    fun `test nonmember reads public room but cannot write over HTTP or WS`() = testNonmemberClientPermissions()

    @Test
    fun `test private room denies outsider plaintext and mutations`() = testPrivateOutsiderIsolation()

    @Test
    fun `test two clients exchange unread messages and retain read state`() = testTwoClientMessages()

    @Test
    fun `test private room authorizes peer decryption across restart`() = testPrivateClientIsolation()

    @Test
    fun `test activity layout preserves preview and unread across sizes`() = testActivityLayoutByHelper()

    @Test
    fun `test room draft survives viewport resizing`() = testActivityDraftByHelper()

    @Test
    fun `test activity context opens room and pinned topic`() = testActivityContextByHelper()

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
    fun `test room refresh clears cross session unread state`() = testRoomActivityRefreshByHelper()

    @Test
    fun `test browser socket server expiry stops reconnect`() = testBrowserSocketServerExpiry()

    @Test
    fun `test browser socket expired cookie stops reconnect`() = testBrowserSocketCookieExpiry()

    @Test
    fun `test browser socket rejects foreign origin`() = testBrowserSocketOrigin()

    @Test
    fun `test browser socket reconnect and logout`() = testBrowserSocketReconnect()

    @Test
    fun `test browser socket cookie loss stops reconnect`() = testBrowserSocketCookieLoss()

    @Test
    fun `test browser socket revocation stops reconnect`() = testBrowserSocketRevocation()

    @Test
    fun `test signed out state survives restart`() = testSignoutPersistenceByHelper()

    @Test
    fun `test sign in survives restart and revocation requires explicit login`() = testSigninLifecycleByHelper()

    @Test
    fun `test browser profile survives restart`() =
        runAppiumBlockingTest { testWasmProfilePersistence(browserType, resolveWasmDistribution()) }

    @Test
    fun `test html semantics bridge mounts`() =
        runAppiumBlockingTest {
            WasmDistributionServer(
                resolveWasmDistribution(),
                coverageDirectory =
                createWasmCoverageDirectory(
                    "WasmAppiumTest",
                    "test html semantics bridge mounts",
                    browserType,
                ),
            ).use { server ->
                browserType.withBrowser(beforeClose = server::collectCoverage) { driver ->
                    driver.get(server.bootstrapUrl)
                    preferDownloadedFonts(driver)
                    driver.get("${server.url}?appium=true")
                    captureFailure(ArtifactCollectionError::CollectionFailed) {
                        WebDriverWait(driver, Duration.ofSeconds(30)).until(
                            ExpectedConditions.presenceOfElementLocated(By.cssSelector("[aria-label='avatar']")),
                        )
                    }.getOrElse { cause ->
                        val body = driver.findElement(By.tagName("body")).getAttribute("innerHTML").orEmpty()
                        val console =
                            captureFailure(
                                ArtifactCollectionError::CollectionFailed,
                            ) { driver.manage().logs().get("browser").all }.getOrNull().orEmpty()
                        val errors =
                            (driver as org.openqa.selenium.JavascriptExecutor)
                                .executeScript("return window.appiumErrors || []")
                                ?: emptyList<Any>()
                        throw AssertionError(
                            "Wasm semantics did not mount; requested=${server.requestedPaths}; body=$body; " +
                                "console=$console; errors=$errors",
                            cause.cause,
                        )
                    }
                }
            }
        }

    @Test
    fun `test sign up identity and session survive restart`() = testSignUpByHelper()

    @Test
    fun `test sign in as system user`() = testSignInAsSystemUserByHelper()

    @Test
    fun `test sign in by injected session`() = testInjectedSessionByHelper()

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
    fun `test publish topic in community room`() = testPublishTopicInCommunityRoomByHelper()
}

private fun resolveWasmDistribution(): File {
    val distribution = File(System.getProperty("e2e.coverage.wasmDistribution", "build/wasmDistribution"))
    return distribution.takeIf { File(it, "index.html").isFile }
        ?: error("Wasm distribution was not prepared; run :app:wasmAppium:prepareWasmDistribution first.")
}
