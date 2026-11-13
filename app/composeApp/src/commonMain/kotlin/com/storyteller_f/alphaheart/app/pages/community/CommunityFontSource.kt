/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.Downloader
import com.storyteller_f.alphaheart.app.common.FileEngine
import com.storyteller_f.alphaheart.app.common.downloadTaskKey
import com.storyteller_f.alphaheart.app.common.extractedFiles
import com.storyteller_f.alphaheart.app.common.getDownloadRevision
import com.storyteller_f.alphaheart.app.common.isDownloadArchive
import com.storyteller_f.alphaheart.app.common.withLock
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadInfoStorage
import com.storyteller_f.storage.DownloadStatus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext

private const val MAX_FONT_FACES = 18

internal expect suspend fun buildCommunityFont(
    identity: String,
    faces: List<CommunityFontFace>,
    files: FileEngine,
): LoadedCommunityFont

internal expect fun createCommunityFontStore(viewModel: UIViewModel): CommunityFontStore

internal fun createDownloadedFontStore(
    viewModel: UIViewModel,
    onLoaded: suspend (String) -> Unit = {},
): CommunityFontStore {
    val source =
        CommunityFontDownload(
            viewModel.resourceRecords,
            viewModel.downloader,
            viewModel.resourceFiles,
            viewModel.resourceNamespace,
            onLoaded,
        )
    return CommunityFontStore(viewModel.viewModelScope, source::observe) { viewModel.downloader.resume(it) }
}

/** Database snapshots select successful generations, never inferred intermediate status events. */
internal class CommunityFontDownload(
    private val records: DownloadInfoStorage,
    private val downloader: Downloader,
    private val files: FileEngine,
    private val namespace: String,
    private val onLoaded: suspend (String) -> Unit = {},
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observe(file: FileInfo): Flow<CommunityFontState> =
        flow {
            try {
                require(file.size in 1..MAX_FONT_SOURCE_BYTES) { "Unsupported font size" }
                downloader.download(file)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                emit(CommunityFontState.Failed(failure))
            }
            records.observeDocumentByFileId(file.id)
                .map { it?.takeIf { row -> getDownloadRevision(row.fileInfo) == getDownloadRevision(file) } }
                .filterNotNull()
                .map(::selectFontRecord)
                .distinctUntilChanged()
                .transformLatest { selection ->
                    when (selection) {
                        FontRecordSelection.Loading -> emit(CommunityFontState.Loading)

                        is FontRecordSelection.Failed ->
                            emit(CommunityFontState.Failed(IllegalStateException(selection.message)))

                        is FontRecordSelection.Ready -> {
                            emit(CommunityFontState.Loading)
                            emit(construct(file, selection))
                        }
                    }
                }.collect { emit(it) }
        }.combine(downloader.observeTask(file.id)) { state, active ->
            // A terminal database snapshot can precede writer/lease cleanup. Keep retry disabled until idle.
            if (active && state is CommunityFontState.Failed) CommunityFontState.Loading else state
        }

    private suspend fun construct(file: FileInfo, selection: FontRecordSelection.Ready): CommunityFontState =
        try {
            val bundle =
                files.withLock(downloadTaskKey(namespace, file.id)) {
                    val current = records.getDocumentByFileId(file.id)
                    check(
                        current != null && current.status == DownloadStatus.PROCESSED &&
                            current.path == selection.path && current.processedGeneration == selection.generation &&
                            getDownloadRevision(current.fileInfo) == getDownloadRevision(file),
                    ) { "Font download changed before reading" }
                    withContext(Dispatchers.Default) { readFontBundle(file, current, files) }
                }
            onLoaded(selection.path)
            CommunityFontState.Ready(bundle)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Napier.w(failure) { "Community font construction failed" }
            CommunityFontState.Failed(failure)
        }
}

private fun selectFontRecord(row: DownloadInfo): FontRecordSelection =
    when (row.status) {
        DownloadStatus.PROCESSED -> FontRecordSelection.Ready(row.path, row.processedGeneration)

        DownloadStatus.PAUSED, DownloadStatus.DOWNLOAD_FAILED, DownloadStatus.PROCESS_FAILED ->
            FontRecordSelection.Failed(row.message.ifEmpty { row.status.name })

        else -> FontRecordSelection.Loading
    }

private sealed interface FontRecordSelection {
    data object Loading : FontRecordSelection
    data class Ready(val path: String, val generation: Long) : FontRecordSelection
    data class Failed(val message: String) : FontRecordSelection
}

private suspend fun readFontBundle(file: FileInfo, record: DownloadInfo, files: FileEngine): CommunityFontBundle {
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
                remaining -= entry.size
                createFontFace(entry.path)
            }
        } else {
            require(isFontName(file.name)) { "Unsupported font format" }
            listOf(createFontFace(file.name).copy(name = record.path))
        }
    return CommunityFontBundle(faces, buildCommunityFont("${record.path}:${record.processedGeneration}", faces, files))
}
