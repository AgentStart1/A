/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import com.storyteller_f.services.filesystem.api.FilesystemRpc
import com.storyteller_f.services.filesystem.api.RpcUploadPack
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.takeFrom
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.server.websocket.WebSockets as ServerWebSockets

class UploadDisconnectTest {
    @Test
    fun `disconnect removes only its own unfinished uploads`() =
        testApplication {
            val base = Files.createTempDirectory(Files.createDirectories(Path.of("build/test/disconnect")), "objects-")
            val temporary = Files.createDirectories(base.resolve("uploads"))
            val storage = LocalFileSystemObjectStorageService("http://localhost", base.resolve("objects"))
            application {
                install(ServerWebSockets)
                install(Krpc)
                routing { configureFilesystemRpc(storage, temporary) }
            }
            val first =
                createClient {
                    install(WebSockets)
                    installKrpc()
                }
            val second =
                createClient {
                    install(WebSockets)
                    installKrpc()
                }
            try {
                val firstConnection =
                    first.rpc {
                        url { takeFrom("ws://localhost/rpc") }
                        rpcConfig { serialization { json() } }
                    }
                val firstRpc = firstConnection.withService<FilesystemRpc>()
                val secondRpc =
                    second.rpc {
                        url { takeFrom("ws://localhost/rpc") }
                        rpcConfig { serialization { json() } }
                    }.withService<FilesystemRpc>()
                firstRpc.beginUpload("bucket", RpcUploadPack("first", "file", 1, "file", "digest"))
                firstRpc.uploadChunk("first", byteArrayOf(1))
                secondRpc.beginUpload("bucket", RpcUploadPack("second", "file", 1, "file", "digest"))
                assertEquals(2L, countFiles(temporary))
                firstConnection.webSocketSession.await().cancel()
                withTimeout(5_000) { while (countFiles(temporary) != 1L) delay(10) }
                assertEquals("ok", secondRpc.health())
                secondRpc.abortUpload("second")
                assertEquals(0L, countFiles(temporary))
            } finally {
                first.close()
                second.close()
                Files.walk(base).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
            }
        }

    private fun countFiles(directory: Path) = Files.list(directory).use { it.count() }
}
