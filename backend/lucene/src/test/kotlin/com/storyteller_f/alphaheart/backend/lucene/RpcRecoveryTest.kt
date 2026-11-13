/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.lucene

import com.storyteller_f.alphaheart.backend.core.ManagedRpcService
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.RestartableRpcServer
import com.storyteller_f.alphaheart.backend.core.service.FileDocument
import com.storyteller_f.alphaheart.backend.core.service.FileDocumentSearch
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.services.lucene.api.LuceneRpc
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneResult
import com.storyteller_f.services.lucene.api.RpcLuceneStoredDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.rpc.withService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RpcRecoveryTest {
    @Test
    fun `failed establishment does not poison later calls`() =
        runTest {
            withAdapter { adapter, server, _ ->
                adapter.clean().getOrThrow()
                server.stop()
                assertFailsWith<Exception> { adapter.clean().getOrThrow() }
                server.start()
                adapter.clean().getOrThrow()
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `same adapter recovers after server restart`() =
        runTest {
            withAdapter { adapter, server, _ ->
                adapter.saveDocument(listOf(FileDocument(1, "report.txt", 2))).getOrThrow()
                server.stop()
                server.start()
                assertEquals("report.txt", search(adapter).single().name)
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `concurrent calls share one reconnected transport`() =
        runTest {
            withAdapter { adapter, server, _ ->
                adapter.clean().getOrThrow()
                server.stop()
                server.start()
                coroutineScope { List(16) { async { adapter.clean().getOrThrow() } }.awaitAll() }
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `interrupted save is not replayed`() =
        runTest {
            withAdapter { adapter, server, state ->
                state.pauseSave.value = true
                coroutineScope {
                    val pending = async { adapter.saveDocument(listOf(FileDocument(1, "report.txt", 2))) }
                    state.entered.await()
                    server.stop()
                    assertFailsWith<Exception> { pending.await().getOrThrow() }
                }
                state.pauseSave.value = false
                server.start()
                assertEquals("report.txt", search(adapter).single().name)
                assertEquals(1, state.saves.value)
            }
        }

    @Test
    fun `caller cancellation retains healthy connection`() =
        runTest {
            withAdapter { adapter, server, state ->
                state.pauseSearch.value = true
                coroutineScope {
                    val pending = async { search(adapter) }
                    state.entered.await()
                    pending.cancel(CancellationException("caller cancelled"))
                    assertFailsWith<CancellationException> { pending.await() }
                    pending.join()
                }
                state.pauseSearch.value = false
                adapter.clean().getOrThrow()
                assertEquals(1, server.connections.value)
            }
        }

    @Test
    fun `cancelled establishment leaves no cached client`() =
        runTest {
            withAdapter { adapter, server, state ->
                state.pauseHealth.value = true
                coroutineScope {
                    val pending = async { adapter.clean() }
                    state.entered.await()
                    pending.cancel(CancellationException("cancel establishment"))
                    assertFailsWith<CancellationException> { pending.await() }
                    pending.join()
                }
                state.pauseHealth.value = false
                adapter.clean().getOrThrow()
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `cancelled waiter preserves establishing client`() =
        runTest {
            withAdapter { adapter, server, state ->
                state.pauseHealth.value = true
                coroutineScope {
                    val owner = async { adapter.clean().getOrThrow() }
                    state.entered.await()
                    val queued = CompletableDeferred<Unit>()
                    val waiter =
                        async {
                            queued.complete(Unit)
                            adapter.clean()
                        }
                    queued.await()
                    waiter.cancel(CancellationException("cancel queued request"))
                    assertFailsWith<CancellationException> { waiter.await() }
                    state.healthRelease.complete(Unit)
                    owner.await()
                }
                assertEquals(1, server.connections.value)
            }
        }

    private suspend fun search(adapter: LuceneFileSearchService) =
        adapter.searchDocument(FileDocumentSearch.Keyword("report", fetch = OffsetFetch(null, 10))).getOrThrow().list

    private suspend fun withAdapter(
        block: suspend (LuceneFileSearchService, RestartableRpcServer<LuceneRpc>, SearchState) -> Unit,
    ) {
        val state = SearchState()
        val server = RestartableRpcServer(LuceneRpc::class) { state }
        server.start()
        val connection = ManagedRpcService(server.url, { it.withService<LuceneRpc>() }, { it.health() })
        try {
            block(LuceneFileSearchService(connection), server, state)
        } finally {
            connection.close()
            server.stop()
        }
    }
}

private class SearchState : LuceneRpc {
    val pauseSave = MutableStateFlow(false)
    val pauseSearch = MutableStateFlow(false)
    val pauseHealth = MutableStateFlow(false)
    val entered = CompletableDeferred<Unit>()
    val healthRelease = CompletableDeferred<Unit>()
    val saves = MutableStateFlow(0)
    private val documents = MutableStateFlow<List<RpcLuceneDocument>>(emptyList())

    override suspend fun health(): String {
        if (pauseHealth.value) {
            entered.complete(Unit)
            healthRelease.await()
        }
        return "ok"
    }
    override suspend fun save(index: String, documents: List<RpcLuceneDocument>) {
        this.documents.value = documents
        saves.update { it + 1 }
        if (pauseSave.value) {
            entered.complete(Unit)
            awaitCancellation()
        }
    }
    override suspend fun replace(index: String, query: RpcLuceneQuery, documents: List<RpcLuceneDocument>): Unit =
        error("Unexpected replacement")
    override suspend fun clean(index: String) {
        documents.value = emptyList()
    }
    override suspend fun delete(index: String, query: RpcLuceneQuery): Unit = error("Unused")
    override suspend fun search(index: String, query: RpcLuceneQuery): RpcLuceneResult {
        if (pauseSearch.value) {
            entered.complete(Unit)
            awaitCancellation()
        }
        return RpcLuceneResult(
            documents.value.map { document ->
                RpcLuceneStoredDocument(
                    document.fields.filter { it.stored }.groupBy { it.name }
                        .mapValues { (_, fields) -> fields.map { it.value } },
                )
            },
            documents.value.size.toLong(),
        )
    }
}
