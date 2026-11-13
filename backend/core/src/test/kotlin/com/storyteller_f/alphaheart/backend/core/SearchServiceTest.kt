/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.result.getOrThrow
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchServiceTest {
    private val saveAndGetDocuments: suspend (TopicSearchService) -> Unit = { service ->
        val docs =
            listOf(
                TopicDocument(1, "hello world", 1, "ROOM", 1, "ROOM", 100),
                TopicDocument(2, "kotlin test", 1, "ROOM", 1, "ROOM", 100),
            )
        service.saveDocument(docs).getOrThrow()

        val result = service.getDocuments(listOf(1, 2)).getOrThrow()
        assertEquals(2, result.size)
        assertEquals("hello world", result[0]?.content)
        assertEquals("kotlin test", result[1]?.content)
    }

    private val saveMultiDocument: suspend (TopicSearchService) -> Unit = { service ->
        service.saveDocument(
            listOf(
                TopicDocument(0, "test", 0, "ROOM", 0, "ROOM", 0),
                TopicDocument(1, "test", 1, "ROOM", 1, "ROOM", 1),
            ),
        ).getOrThrow()
    }

    private val searchDocuments: suspend (TopicSearchService) -> Unit = { service ->
        val docs =
            listOf(
                TopicDocument(10, "apple banana cherry", 1, "ROOM", 1, "ROOM", 100),
                TopicDocument(11, "banana orange grape", 1, "ROOM", 1, "ROOM", 101),
                TopicDocument(12, "watermelon kiwi", 1, "ROOM", 1, "ROOM", 102),
            )
        service.saveDocument(docs).getOrThrow()

        val searchResult =
            service.searchDocument(
                TopicDocumentSearch.All("banana", OffsetFetch(null, 10)),
            ).getOrThrow()
        assertTrue(searchResult.list.isNotEmpty())
        assertTrue(searchResult.list.all { it.content.contains("banana") })
    }

    private val cleanDocuments: suspend (TopicSearchService) -> Unit = { service ->
        service.saveDocument(
            listOf(TopicDocument(20, "to be cleaned", 1, "ROOM", 1, "ROOM", 100)),
        ).getOrThrow()

        service.clean().getOrThrow()

        val result = service.getDocuments(listOf(20)).getOrThrow()
        assertTrue(result.all { it == null })
    }

    @Test
    fun `test save and get documents rpc`() = testSearchRpc(saveAndGetDocuments)

    @Test
    fun `test save and get documents container`() = testSearchContainer(saveAndGetDocuments)

    @Test
    fun `test save multi document rpc`() = testSearchRpc(saveMultiDocument)

    @Test
    fun `test save multi document container`() = testSearchContainer(saveMultiDocument)

    @Test
    fun `test search documents rpc`() = testSearchRpc(searchDocuments)

    @Test
    fun `test search documents container`() = testSearchContainer(searchDocuments)

    @Test
    fun `test clean documents rpc`() = testSearchRpc(cleanDocuments)

    @Test
    fun `test clean documents container`() = testSearchContainer(cleanDocuments)

    @Test
    fun `test reconnect search after service restart`() =
        runBlocking {
            System.setProperty("api.version", "1.44")
            createLuceneContainer().use { first ->
                first.start()
                val port = first.getMappedPort(8821)
                val env =
                    MergedEnv(
                        listOf(
                            mapOf(
                                "SEARCH_SERVICE" to "lucene",
                                "LUCENE_RPC_URL" to "ws://${first.host}:$port/rpc",
                            ),
                        ),
                    )
                val service = buildTopicSearchService(env)
                service.saveDocument(listOf(TopicDocument(1, "before restart", 1, "ROOM", 1, "ROOM", 100))).getOrThrow()
                first.stop()
                assertTrue(service.getDocuments(listOf(1)).isFailure)
                createLuceneContainer().apply {
                    setPortBindings(listOf("$port:8821"))
                }.use { replacement ->
                    replacement.start()
                    val rebuiltService = buildTopicSearchService(env)
                    rebuiltService.saveDocument(
                        listOf(TopicDocument(2, "after restart", 1, "ROOM", 1, "ROOM", 100)),
                    ).getOrThrow()
                    assertEquals("after restart", rebuiltService.getDocuments(listOf(2)).getOrThrow().single()?.content)
                }
            }
        }

    private fun createLuceneContainer(): GenericContainer<*> =
        GenericContainer(DockerImageName.parse("a-lucene:latest")).apply {
            withTmpFs(mapOf("/data" to "rw,uid=1000,gid=1000"))
            withExposedPorts(8821)
            waitingFor(Wait.forHttp("/health").forPort(8821).withStartupTimeout(Duration.ofSeconds(90)))
        }
}
