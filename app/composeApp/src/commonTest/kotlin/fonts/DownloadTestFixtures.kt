/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.paging.PagingSource
import com.storyteller_f.a.app.common.DownloadContext
import com.storyteller_f.a.app.common.DownloadEngine
import com.storyteller_f.a.app.common.DownloadFileStore
import com.storyteller_f.a.app.common.Downloader
import com.storyteller_f.a.app.common.SimpleTaskRegister
import com.storyteller_f.a.app.pages.community.CommunityFontStore
import com.storyteller_f.a.app.pages.community.fontFileKey
import com.storyteller_f.a.client.core.ConstPassHolder
import com.storyteller_f.a.client.core.UserSessionManager
import com.storyteller_f.a.client.core.UserSessionModel
import com.storyteller_f.a.client.core.WebSocketClientImpl
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadInfoStorage
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

internal class DownloadTestRecords(private val onSave: suspend (DownloadInfo) -> Unit = {}) : DownloadInfoStorage {
    private val rows = MutableStateFlow<Map<Long, DownloadInfo>>(emptyMap())
    val history = mutableListOf<DownloadInfo>()
    override suspend fun save(item: DownloadInfo) {
        onSave(item)
        history += item
        rows.update { it + (item.fileInfo.id to item) }
    }
    override suspend fun clean() {
        rows.value = emptyMap()
    }
    override suspend fun getDocumentByFileId(fileId: Long): DownloadInfo? = rows.value[fileId]
    override fun observeDocumentByFileId(fileId: Long): Flow<DownloadInfo?> =
        rows.map { it[fileId] }.distinctUntilChanged()
    override fun observeData(): PagingSource<Int, DownloadInfo> = error("Paging is not part of this fixture")
}

internal class DownloadTestFixture(
    scope: CoroutineScope,
    val files: DownloadFileStore,
    client: HttpClient,
    val namespace: String = "test-${Uuid.random().toHexString()}",
    val records: DownloadInfoStorage = DownloadTestRecords(),
) {
    private val session =
        object : UserSessionManager {
            override val client = client
            override val model = UserSessionModel()
            override val passHolder = ConstPassHolder(null)
            override val webSocketClient: WebSocketClientImpl get() = error("Downloads do not use WebSockets")
        }
    private val engine =
        DownloadEngine(
            DownloadContext(namespace, scope, records, session),
            SimpleTaskRegister(scope),
            files,
        )
    val downloader =
        object : Downloader {
            override suspend fun download(fileInfo: FileInfo, retry: Boolean): DownloadInfo =
                engine.start(fileInfo, retry)
            override fun resume(id: Long) = engine.resume(id)
            override fun pause(id: Long) = engine.pause(id)
        }
    suspend fun close() {
        files.delete(files.location(namespace))
    }
}

internal fun fontFile(bytes: ByteArray, name: String = "Regular.ttf", id: Long = 1): FileInfo =
    FileInfo.EMPTY.copy(id = id, name = name, size = bytes.size.toLong(), url = "https://fonts.invalid/$id")

internal fun createFontStore(scope: CoroutineScope, fetch: suspend (FileInfo) -> ByteArray): CommunityFontStore =
    CommunityFontStore(scope) { file, _ -> testFontBundle(fontFileKey(file), file.name, fetch(file)) }
