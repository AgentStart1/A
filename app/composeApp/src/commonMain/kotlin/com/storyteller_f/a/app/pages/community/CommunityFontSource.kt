/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import com.storyteller_f.a.app.ClientFileProvider
import com.storyteller_f.a.app.IAccountInstance
import com.storyteller_f.a.app.common.DownloadFileStore
import com.storyteller_f.a.app.common.Downloader
import com.storyteller_f.a.app.common.downloadTaskKey
import com.storyteller_f.a.app.common.extractedFiles
import com.storyteller_f.a.app.common.getDownloadRevision
import com.storyteller_f.a.app.common.isDownloadArchive
import com.storyteller_f.a.app.common.withLock
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadInfoStorage
import com.storyteller_f.storage.DownloadStatus
import kotlinx.coroutines.flow.first

private const val MAX_FONT_FACES = 18

internal expect fun createCommunityFontStore(
    instance: IAccountInstance,
    provider: ClientFileProvider,
): CommunityFontStore

internal fun createDownloadedFontStore(
    instance: IAccountInstance,
    files: DownloadFileStore,
    onLoaded: suspend (String) -> Unit = {},
    getDownloader: suspend () -> Downloader,
): CommunityFontStore =
    CommunityFontStore(instance.accountScope) { file, retry ->
        CommunityFontDownload(
            instance.database.download,
            getDownloader(),
            files,
            md5(instance.address),
            onLoaded,
        ).load(file, retry)
    }

/** Download returns after registration. Completion is observed from the existing database projection. */
internal class CommunityFontDownload(
    private val records: DownloadInfoStorage,
    private val downloader: Downloader,
    private val files: DownloadFileStore,
    private val namespace: String,
    private val onLoaded: suspend (String) -> Unit = {},
) {
    suspend fun load(file: FileInfo, retry: Boolean = false): CommunityFontBundle {
        require(file.size in 1..MAX_FONT_SOURCE_BYTES) { "Unsupported font size" }
        val registered = downloader.download(file, retry)
        val record =
            if (registered.isTerminal()) {
                registered
            } else {
                checkNotNull(
                    records.observeDocumentByFileId(file.id).first { current ->
                        current != null && (
                            getDownloadRevision(
                                current.fileInfo,
                            ) != getDownloadRevision(file) || current.isTerminal()
                            )
                    },
                )
            }
        check(getDownloadRevision(record.fileInfo) == getDownloadRevision(file)) { "Font request was superseded" }
        check(record.status == DownloadStatus.PROCESSED) { "Font download has not completed: ${record.status}" }
        val bundle = files.withLock(downloadTaskKey(namespace, file.id)) { readFontBundle(file, record, files) }
        onLoaded(record.path)
        return bundle
    }
}

private fun DownloadInfo.isTerminal(): Boolean =
    status in
        listOf(
            DownloadStatus.PROCESSED,
            DownloadStatus.DOWNLOAD_FAILED,
            DownloadStatus.PROCESS_FAILED,
            DownloadStatus.PAUSED,
        )

private suspend fun readFontBundle(
    file: FileInfo,
    record: DownloadInfo,
    files: DownloadFileStore,
): CommunityFontBundle {
    check(files.size(record.path) == file.size) { "Font cache is missing or incomplete" }
    var remaining = MAX_FONT_DECODED_BYTES
    val faces =
        if (file.isDownloadArchive()) {
            val entries =
                checkNotNull(files.extractedFiles(record.path)) { "Font extraction is incomplete" }
                    .filter { isFontName(it.path) }
            require(entries.size in 1..MAX_FONT_FACES) { "Font archive has no usable family or too many faces" }
            entries.map { entry ->
                require(entry.size in 1..remaining) { "Font family exceeds the decoded size limit" }
                val bytes = files.read(entry.path, entry.size)
                check(bytes.size.toLong() == entry.size) { "Font size mismatch" }
                remaining -= bytes.size
                createFontFace(entry.path, bytes)
            }
        } else {
            require(isFontName(file.name)) { "Unsupported font format" }
            listOf(createFontFace(file.name, files.read(record.path, remaining)))
        }
    return CommunityFontBundle(fontFileKey(file), faces)
}
