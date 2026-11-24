/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.media

import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnMediaUploaded
import com.storyteller_f.alphaheart.app.error.UploadError
import com.storyteller_f.alphaheart.app.invalid_image_file
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.app.utils.Recorder
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogStateProgress
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.utils.generateImageMarkdownContent
import com.storyteller_f.shared.utils.generateObjectMarkdownContent
import com.storyteller_f.shared.utils.sha256
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import org.jetbrains.compose.resources.getString

suspend fun AppGlobalDialogController.selectFileAndUpload(
    mediaTarget: ObjectTuple,
    uploadSuccess: (List<FileInfo>) -> Unit,
    imageOnly: Boolean = false,
) {
    useResult {
        val f = FileKit.openFilePicker(type = if (imageOnly) FileKitType.Image else FileKitType.File())
        if (f != null) {
            val invalidImage = getString(Res.string.invalid_image_file)
            require(!imageOnly || ContentType.defaultForFileExtension(f.extension).toString().startsWith("image/")) {
                invalidImage
            }
            val bytes = f.readBytes()
            val fileSha256 = Buffer().also { it.write(bytes) }.use { sha256(it) }
            val uploadData = getUploadDataFromPlatformFile(f, fileSha256, bytes)
            upload(
                mediaTarget,
                uploadData,
            ) { p, t ->
                emitProgress {
                    GlobalDialogState.Loading(progress = GlobalDialogStateProgress(p, t))
                }
            }.map {
                require(
                    !imageOnly || it.all { info -> info.contentType.startsWith("image/") && info.dimension != null },
                ) {
                    invalidImage
                }
                it as List<FileInfo>?
            }
        } else {
            CustomResult.success(null)
        }
    }.getOrNull()?.let {
        uploadSuccess(it)
    }
}

private fun getUploadDataFromPlatformFile(f: PlatformFile, sha256: String, bytes: ByteArray) =
    UploadData(
        f.size(),
        f.name,
        ContentType.defaultForFileExtension(f.extension),
        sha256,
    ) {
        Buffer().also { it.write(bytes) }
    }

class PlatformClientFile private constructor(val platformFile: PlatformFile, private val bytes: ByteArray) :
    ClientFile {
    override val name: String get() = platformFile.name
    override val contentType: ContentType get() = ContentType.defaultForFileExtension(platformFile.extension)
    override val size: Long get() = platformFile.size()
    override val path: String get() = platformFile.path

    override fun source(): Source = ByteArrayRawSource(bytes).buffered()

    companion object {
        suspend operator fun invoke(platformFile: PlatformFile) =
            PlatformClientFile(platformFile, platformFile.readBytes())
    }
}

private class ByteArrayRawSource(private val bytes: ByteArray) : RawSource {
    private var offset = 0
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (offset >= bytes.size) return -1L
        val count = minOf(byteCount, (bytes.size - offset).toLong()).toInt()
        sink.write(bytes, offset, offset + count)
        offset += count
        return count.toLong()
    }
    override fun close() = Unit
}

internal suspend fun AppGlobalDialogController.uploadRecording(
    recorder: Recorder,
    mediaTarget: ObjectTuple,
): CustomResult<List<FileInfo>?, UploadError> =
    useResult {
        captureFailure(UploadError::from) {
            recorder.stopRecord()?.let { createRecordingUpload(it) }
        }.andThen(UploadError::fromError) { data ->
            if (data == null) {
                CustomResult.success(null)
            } else {
                upload(mediaTarget, data) { progress, total ->
                    emitProgress {
                        GlobalDialogState.Loading(progress = GlobalDialogStateProgress(progress, total))
                    }
                }.map { it as List<FileInfo>? }
            }
        }
    }.mapError(UploadError::fromError)

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private suspend fun createRecordingUpload(bytes: ByteArray): UploadData {
    val digest = Buffer().also { it.write(bytes) }.use { sha256(it) }
    return UploadData(
        bytes.size.toLong(),
        "${kotlin.uuid.Uuid.random().toHexString()}.wav",
        ContentType.parse("audio/wav"),
        digest,
    ) { Buffer().also { it.write(bytes) } }
}

suspend fun AppGlobalDialogController.upload(
    mediaTarget: ObjectTuple,
    uploadData: UploadData,
    onUpload: suspend (Long, Long?) -> Unit = { _, _ -> },
): CustomResult<List<FileInfo>, UploadError> {
    if (uploadData.size > 100 * 1024 * 1024) {
        return CustomResult.Failure(
            UploadError.from(Exception("file size is too large")),
        ).mapError(UploadError::fromError)
    }

    return request { this.upload(mediaTarget, uploadData, onUpload) }.map {
        it.data
    }.onSuccess {
        emitEvent(OnMediaUploaded(it))
    }.mapError(UploadError::fromError)
}

fun insertContent(it: FileInfo, input: String, updateInput: (String) -> Unit) {
    val text =
        if (it.contentType.startsWith("image/")) {
            "\n${generateImageMarkdownContent(it)}"
        } else {
            "\n${generateObjectMarkdownContent(it)}"
        }
    updateInput(input + text)
}
