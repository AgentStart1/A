/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.signOutAll
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertIs

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testDownloadOwnerRevocation(embedded: Boolean) =
    withUploadedFile(if (embedded) PreviewFile.VIDEO else PreviewFile.BINARY, topic = embedded) { scope, data ->
        val driver = scope.driver as WasmAppTestDriver
        if (embedded) {
            openUploadedTopic(driver, data)
        } else {
            openUploadedPreview(driver, data)
        }
        val probe = driver.downloadProbe()
        probe.holdResponse(data.file.url)
        try {
            if (embedded) driver.clickByText("Media options")
            driver.clickByText("Download")
            probe.awaitHeld()
            data.user.sessionManager.signOutAll().getOrThrow()
            assertEquals(
                HttpStatusCode.Unauthorized,
                assertIs<ServerErrorException>(data.user.sessionManager.restoreSession().exceptionOrNull()).status,
            )
            probe.awaitAborted()
            probe.release()
            probe.assertLateResponseDiscarded()
            if (embedded) driver.clickByDescription("avatar")
            driver.assertVisibleByText("Sign in")
            driver.saveSnapshot("revoked-download-owner")
        } finally {
            probe.release()
        }
    }
