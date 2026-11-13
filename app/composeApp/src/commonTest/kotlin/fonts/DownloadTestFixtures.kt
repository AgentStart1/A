/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.paging.PagingSource
import com.storyteller_f.alphaheart.app.common.DownloadContext
import com.storyteller_f.alphaheart.app.common.DownloadSession
import com.storyteller_f.alphaheart.app.common.DownloaderImpl
import com.storyteller_f.alphaheart.app.common.FileEngine
import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.UserSessionModel
import com.storyteller_f.alphaheart.client.core.WebSocketClientImpl
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
    override fun forServer(server: String): DownloadInfoStorage = error("Use SQLite for collection isolation")
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
    val files: FileEngine,
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
    val downloader =
        DownloaderImpl(
            DownloadContext(namespace, scope, records) { DownloadSession(session) {} },
            SimpleTaskRegister(scope),
            files,
        )
    suspend fun close() {
        files.delete(files.location("public-resources-v1/$namespace"))
    }
}

internal fun fontFile(bytes: ByteArray, name: String = "Regular.ttf", id: Long = 1): FileInfo =
    FileInfo.EMPTY.copy(id = id, name = name, size = bytes.size.toLong(), url = "https://fonts.invalid/$id")
