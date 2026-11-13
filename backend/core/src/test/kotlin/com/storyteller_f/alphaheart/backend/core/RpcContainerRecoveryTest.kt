/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.result.getOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

class RpcContainerRecoveryTest {
    @Test
    fun `search adapter recovers after service restart`() =
        runTest {
            withContainer("a-lucene:latest", 8821) { container ->
                val env =
                    MergedEnv(
                        listOf(
                            mapOf(
                                "SEARCH_SERVICE" to "lucene",
                                "LUCENE_RPC_URL" to "ws://${container.host}:${container.getMappedPort(8821)}/rpc",
                                "BUILD_TYPE" to "test",
                            ),
                        ),
                    )
                val adapter = buildTopicSearchService(env)
                adapter.saveDocument(listOf(TopicDocument(1, "before", 1, "ROOM", 1, "ROOM", 1))).getOrThrow()
                restart(container, 8821)
                val expected = TopicDocument(2, "after", 1, "ROOM", 1, "ROOM", 2)
                adapter.saveDocument(listOf(expected)).getOrThrow()
                assertEquals(listOf(expected), adapter.getDocuments(listOf(2)).getOrThrow())
            }
        }

    @Test
    fun `storage adapter recovers after service restart`() =
        runTest {
            withContainer("a-filesystem:latest", 8820) { container ->
                val env =
                    MergedEnv(
                        listOf(
                            mapOf(
                                "MEDIA_SERVICE" to "filesystem",
                                "FILESYSTEM_RPC_URL" to "ws://${container.host}:${container.getMappedPort(8820)}/rpc",
                            ),
                        ),
                    )
                val adapter = mediaService(env)
                val content = "restart recovery".encodeToByteArray()
                val path =
                    Files.createTempFile(
                        Files.createDirectories(Path.of("build/test/recovery")),
                        "upload-",
                        ".txt",
                    )
                try {
                    Files.write(path, content)
                    val sha = MessageDigest.getInstance("SHA-256").digest(content).toHexString()
                    adapter.upload(
                        "bucket",
                        listOf(UploadPack(path.toFile(), "before", content.size.toLong(), "before", sha)),
                    ).getOrThrow()
                    restart(container, 8820)
                    adapter.upload(
                        "bucket",
                        listOf(UploadPack(path.toFile(), "after", content.size.toLong(), "after", sha)),
                    ).getOrThrow()
                    assertEquals(
                        listOf("after"),
                        adapter.get("bucket", listOf("after")).getOrThrow().map { it.fullName },
                    )
                } finally {
                    Files.deleteIfExists(path)
                }
            }
        }

    private suspend fun withContainer(image: String, port: Int, block: suspend (GenericContainer<*>) -> Unit) {
        val container =
            GenericContainer(DockerImageName.parse(image)).apply {
                withTmpFs(mapOf("/data" to "rw,uid=1000,gid=1000"))
                withExposedPorts(port)
                withEnv("FILESYSTEM_PUBLIC_URL", "http://filesystem:8822")
                waitingFor(Wait.forHttp("/health").forPort(port))
            }
        try {
            withContext(Dispatchers.IO) { container.start() }
            block(container)
        } finally {
            withContext(Dispatchers.IO) { container.close() }
        }
    }

    private suspend fun restart(container: GenericContainer<*>, port: Int) =
        withContext(Dispatchers.IO) {
            // Random Docker publications may change on restart; keep the adapter endpoint fixed.
            val publishedPort = container.getMappedPort(port)
            container.stop()
            container.setPortBindings(listOf("$publishedPort:$port"))
            container.start()
        }
}
