/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.sha256
import io.ktor.http.ContentType
import kotlinx.io.Buffer

fun AppiumTestBase<PanelAppiumHelper, AndroidAppiumHelper>.testAndroidPanelVideoPreview() =
    runAppiumBlockingTest {
        var panel: PanelAuthenticatedSession? = null
        try {
            platformHelper.runTest(testName, targetHelper, setup = { ports ->
                val session = targetHelper.createAuthenticatedSession(ports)
                panel = session
                val user = AppAppiumHelper().createAuthenticatedSession(ports)
                val file =
                    try {
                        uploadPanelVideo(user)
                    } finally {
                        user.sessionManager.client.close()
                    }
                AppiumTestSetup(PanelVideoPreview(session, file))
            }) { scope, data ->
                val driver = scope.driver as AndroidAppTestDriver
                scenarioPanelSignin(driver, data.panel.session.pemPrivateKey)
                driver.clickByDescription("Menu")
                driver.clickByText("All files")
                driver.clickFileByName(data.file.name)
                driver.clickByText("Fullscreen preview")
                driver.withAnimatedMediaControls {
                    driver.clickByDescription("play")
                    driver.assertVideoPlaying(data.file.name)
                    driver.saveSnapshot("panel-uploaded-video-playing")
                    driver.navigateBack()
                }
                driver.assertVisibleByText("Fullscreen preview")
            }
        } finally {
            panel?.sessionManager?.client?.close()
        }
    }

private suspend fun uploadPanelVideo(user: AuthenticatedSession): FileInfo {
    val kind = PreviewFile.VIDEO
    val bytes =
        requireNotNull(PreviewFile::class.java.getResourceAsStream("/media/${kind.filename}"))
            .buffered().use { it.readBytes() }
    val upload =
        UploadData(
            bytes.size.toLong(),
            kind.filename,
            ContentType.parse(kind.contentType),
            sha256(Buffer().apply { write(bytes) }),
        ) {
            Buffer().apply { write(bytes) }
        }
    val uid = requireNotNull(user.sessionManager.model.uid)
    return user.sessionManager
        .upload(ObjectTuple(uid, ObjectType.USER), upload)
        .getOrThrow().data.single()
}

private data class PanelVideoPreview(val panel: PanelAuthenticatedSession, val file: FileInfo)
