/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.generateImageMarkdownContent
import com.storyteller_f.shared.utils.generateObjectMarkdownContent
import com.storyteller_f.shared.utils.sha256
import io.ktor.http.ContentType
import kotlinx.io.Buffer

/** Synthetic media is bundled so acceptance never depends on external presets or remote examples. */
enum class PreviewFile(val filename: String, val contentType: String) {
    IMAGE("sample.png", "image/png"),
    VIDEO("sample.mp4", "video/mp4"),
    AUDIO("sample.wav", "audio/wav"),
    PDF("sample.pdf", "application/pdf"),
    BINARY("sample.bin", "application/octet-stream"),
}

internal data class UploadedFileScenario(
    val user: AuthenticatedSession,
    val file: FileInfo,
    val bytes: ByteArray,
    val topicMarker: String?,
)

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testUploadedPreview(kind: PreviewFile, topic: Boolean) =
    withUploadedFile(kind, topic) { scope, data ->
        openUploadedPreview(scope.driver, data)
        val inspectAndReturn: suspend () -> Unit = {
            assertUploadedPreview(scope.driver, kind, data.file, data.bytes)
            scope.driver.saveSnapshot("uploaded-preview")
            scope.driver.navigateBack()
        }
        val driver = scope.driver
        if (driver is AndroidAppTestDriver && (kind == PreviewFile.VIDEO || kind == PreviewFile.AUDIO)) {
            driver.withAnimatedMediaControls(inspectAndReturn)
        } else {
            inspectAndReturn()
        }
        if (topic) {
            scope.driver.assertVisibleByText(requireNotNull(data.topicMarker))
        } else {
            assertUploadedListRestored(scope.driver, data.file.name)
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testNativeDownloadCancellation() =
    withUploadedFile(PreviewFile.BINARY, topic = false) { scope, data ->
        val driver = scope.driver
        openUploadedPreview(driver, data)
        driver.clickByText("Download")
        when (driver) {
            is DesktopAppTestDriver -> driver.cancelNativeSaveDialog()
            is AndroidAppTestDriver -> driver.cancelNativeSaveDialog(targetHelper.androidApp.packageName)
            else -> error("Native save-dialog coverage requires a native target")
        }
        driver.assertVisibleByText(data.file.name)
        driver.assertVisibleByText("Preview unavailable for this file type")
        driver.saveSnapshot("native-download-cancelled")
        driver.navigateBack()
        assertUploadedListRestored(driver, data.file.name)
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testUploadedDownload(topic: Boolean) =
    withUploadedFile(PreviewFile.BINARY, topic) { scope, data ->
        openUploadedPreview(scope.driver, data)
        scope.driver.clickByText("Download")
        (scope.driver as WasmAppTestDriver).assertDownloadedBytes(data.file.name, data.bytes)
        scope.driver.assertVisibleByText(data.file.name)
    }

internal fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.withUploadedFile(
    kind: PreviewFile,
    topic: Boolean,
    platform: PlatformAppiumHelper = platformHelper,
    fixtureName: String = kind.filename,
    block: suspend (AppiumTestScope, UploadedFileScenario) -> Unit,
) = runAppiumBlockingTest {
    platform.runTest(testName, targetHelper, setup = { ports ->
        val user = targetHelper.createAuthenticatedSession(ports)
        var prepared = false
        try {
            val bytes =
                checkNotNull(PreviewFile::class.java.getResourceAsStream("/media/$fixtureName")) {
                    "Missing synthetic media fixture: $fixtureName"
                }.buffered().use { it.readBytes() }
            val uid = requireNotNull(user.sessionManager.model.uid)
            val upload =
                UploadData(
                    bytes.size.toLong(),
                    kind.filename,
                    ContentType.parse(kind.contentType),
                    sha256(Buffer().apply { write(bytes) }),
                ) {
                    Buffer().apply { write(bytes) }
                }
            val file =
                user.sessionManager.upload(ObjectTuple(uid, ObjectType.USER), upload)
                    .getOrThrow().data.single()
            val marker = if (topic) "uploaded attachment contract" else null
            if (marker != null) {
                val embedded =
                    if (kind == PreviewFile.IMAGE) {
                        generateImageMarkdownContent(
                            file,
                        )
                    } else {
                        generateObjectMarkdownContent(file)
                    }
                user.sessionManager.createTopic(ObjectType.USER, uid, "$marker\n\n$embedded").getOrThrow()
            }
            AppiumTestSetup(UploadedFileScenario(user, file, bytes, marker), user.session).also { prepared = true }
        } finally {
            if (!prepared) {
                user.sessionManager.client.close()
            }
        }
    }) { scope, data ->
        try {
            block(scope, data)
        } finally {
            data.user.sessionManager.client.close()
        }
    }
}

internal suspend fun openUploadedTopic(driver: AppTestDriver, data: UploadedFileScenario) {
    driver.clickByDescription("avatar")
    driver.clickByTextContaining(data.user.session.address)
    driver.clickByText(requireNotNull(data.topicMarker))
}

internal suspend fun openUploadedPreview(driver: AppTestDriver, data: UploadedFileScenario) {
    if (data.topicMarker != null) {
        openUploadedTopic(driver, data)
        when {
            driver is WasmAppTestDriver || driver is DesktopAppTestDriver ->
                driver.clickByDescription(
                    "open file ${data.file.name}",
                )

            data.file.contentType.startsWith("image/") -> driver.clickByText(data.file.name)

            data.file.contentType == FileInfo.PDF_CONTENT_TYPE -> driver.clickByDescription("fullscreen")

            else -> driver.clickByText("Preview: ${data.file.name}")
        }
    } else {
        driver.clickByDescription("avatar")
        driver.clickByText("File Explorer")
        if (driver is AndroidAppTestDriver) {
            driver.clickFileByName(data.file.name)
        } else {
            driver.clickByText(data.file.name)
        }
    }
    driver.assertVisibleByText(data.file.name)
    driver.assertVisibleByText("Download")
    if (data.topicMarker != null) {
        driver.assertNotVisibleByText(data.topicMarker)
    } else if (driver is AndroidAppTestDriver) {
        driver.assertNotVisibleByText("Input to search files")
    } else {
        driver.assertNotVisibleByDescription("search query")
    }
}

private suspend fun assertUploadedListRestored(driver: AppTestDriver, name: String) {
    if (driver is AndroidAppTestDriver) {
        driver.assertVisibleByText("Input to search files")
        driver.assertFileNameVisible(name)
    } else {
        driver.assertVisibleByDescription("search query")
        driver.assertVisibleByText(name)
    }
}

private suspend fun assertUploadedPreview(
    driver: AppTestDriver,
    kind: PreviewFile,
    file: FileInfo,
    expectedBytes: ByteArray,
) {
    when (kind) {
        PreviewFile.IMAGE -> driver.assertVisibleByDescription("view image")

        PreviewFile.PDF ->
            if (driver is WasmAppTestDriver) {
                driver.assertPdfPreview(expectedBytes)
            } else {
                driver.assertVisibleByDescription("Page 0")
            }

        PreviewFile.BINARY -> driver.assertVisibleByText("Preview unavailable for this file type")

        PreviewFile.VIDEO, PreviewFile.AUDIO -> {
            if (driver is WasmAppTestDriver) {
                driver.clickByText("Play")
                driver.assertPlaybackProgress()
                driver.clickByText("Pause")
                driver.assertVisibleByText("Play")
            } else if (driver is AndroidAppTestDriver) {
                driver.clickByDescription("Play")
                if (kind == PreviewFile.VIDEO) {
                    driver.assertVideoPlaying(file.name)
                } else {
                    driver.assertVisibleByDescription("Pause")
                }
            } else {
                if (kind == PreviewFile.AUDIO) driver.clickByDescription("Play")
                driver.assertVisibleByDescription("Pause")
                if (kind == PreviewFile.VIDEO) {
                    if (DesktopTestPlatform.current() == DesktopTestPlatform.Windows) {
                        (driver as DesktopAppTestDriver).media.assertVideoFrame()
                    } else {
                        driver.assertVisibleByDescription("video frame ${file.name}")
                    }
                }
            }
        }
    }
}
