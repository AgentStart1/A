/*
 * This is a private project. All rights reserved.
 */
package com.storyteller_f.alphaheart.cloud.lucene

import com.storyteller_f.services.lucene.api.LuceneRpc
import com.storyteller_f.services.lucene.api.RpcLuceneDocValues
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneField
import com.storyteller_f.services.lucene.api.RpcLuceneIndex
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneResult
import com.storyteller_f.services.lucene.api.RpcLuceneSort
import com.storyteller_f.services.lucene.api.RpcLuceneSortType
import com.storyteller_f.services.lucene.api.RpcLuceneStoredDocument
import com.storyteller_f.services.lucene.api.RpcLuceneValue
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.NumericDocValuesField
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexNotFoundException
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.BytesRef
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories

internal class LuceneRpcImpl(private val base: Path) : LuceneRpc {
    private val analyzer = StandardAnalyzer()
    private val indexes = ConcurrentHashMap<String, Mutex>()
    init {
        base.createDirectories()
    }
    override suspend fun health() = "ok"
    override suspend fun save(index: String, documents: List<RpcLuceneDocument>) =
        useIndex(index) {
            IndexWriter(this, IndexWriterConfig(analyzer)).use { it.addDocuments(documents.map(::toDocument)) }
            Unit
        }
    override suspend fun replace(index: String, query: RpcLuceneQuery, documents: List<RpcLuceneDocument>) =
        useIndex(index) { replaceDocuments(query, documents) }

    private fun FSDirectory.replaceDocuments(query: RpcLuceneQuery, documents: List<RpcLuceneDocument>) {
        val replacements = documents.map(::toDocument)
        val selection = buildQuery(query)
        IndexWriter(this, IndexWriterConfig(analyzer)).use { writer ->
            try {
                writer.deleteDocuments(selection)
                writer.addDocuments(replacements)
                writer.commit()
            } catch (failure: Throwable) {
                try {
                    writer.rollback()
                } catch (cleanup: Throwable) {
                    failure.addSuppressed(cleanup)
                }
                throw failure
            }
        }
    }
    override suspend fun clean(index: String) =
        useIndex(index) {
            IndexWriter(this, IndexWriterConfig(analyzer)).use { it.deleteDocuments(MatchAllDocsQuery.INSTANCE) }
            Unit
        }
    override suspend fun delete(index: String, query: RpcLuceneQuery) =
        useIndex(index) {
            IndexWriter(this, IndexWriterConfig(analyzer)).use { it.deleteDocuments(buildQuery(query)) }
            Unit
        }
    override suspend fun search(index: String, query: RpcLuceneQuery): RpcLuceneResult =
        useIndex(index) {
            require(query.offset >= 0 && query.size > 0) { "invalid pagination" }
            val limit = Math.addExact(query.offset, query.size)
            try {
                DirectoryReader.open(this).use { reader ->
                    val searcher = IndexSearcher(reader)
                    val sort = buildSort(query.sort)
                    val hits = searcher.search(buildQuery(query), limit, sort)
                    val documents =
                        hits.scoreDocs.drop(
                            query.offset,
                        ).map { readStoredDocument(searcher.storedFields().document(it.doc)) }
                    RpcLuceneResult(documents, hits.totalHits.value)
                }
            } catch (_: IndexNotFoundException) {
                RpcLuceneResult(emptyList(), 0)
            }
        }
    private suspend fun <T> useIndex(index: String, block: FSDirectory.() -> T): T =
        withContext(Dispatchers.IO) {
            require(index.matches(Regex("[a-zA-Z0-9_-]+"))) { "invalid index name" }
            val path = base.resolve(index)
            require(!Files.isSymbolicLink(path)) { "symbolic index paths are not allowed" }
            indexes.computeIfAbsent(index) { Mutex() }.withLock {
                FSDirectory.open(path.createDirectories()).use(block)
            }
        }
    private fun toDocument(source: RpcLuceneDocument) =
        Document().apply {
            require(source.fields.all { it.name.isNotBlank() }) { "field names must not be blank" }
            source.fields.forEach { addField(it.name, it) }
        }

    private fun buildSort(fields: List<RpcLuceneSort>): Sort {
        if (fields.isEmpty()) {
            return Sort.RELEVANCE
        }
        // Construct the Java vararg array inline instead of copying an existing array.
        return Sort(
            *Array(fields.size) { index ->
                val field = fields[index]
                require(field.field.isNotBlank()) { "sort field names must not be blank" }
                SortField(
                    field.field,
                    when (field.type) {
                        RpcLuceneSortType.LONG -> SortField.Type.LONG
                        RpcLuceneSortType.STRING -> SortField.Type.STRING
                    },
                    field.descending,
                )
            },
        )
    }

    private fun Document.addField(name: String, field: RpcLuceneField) {
        require(field.stored || field.index != RpcLuceneIndex.NONE || field.docValues != RpcLuceneDocValues.NONE) {
            "fields must be stored, indexed or have DocValues"
        }
        when (val value = field.value) {
            is RpcLuceneValue.Text -> {
                when (field.index) {
                    RpcLuceneIndex.TEXT -> add(TextField(name, value.value, Field.Store.NO))
                    RpcLuceneIndex.EXACT -> add(StringField(name, value.value, Field.Store.NO))
                    RpcLuceneIndex.NONE -> Unit
                }
                require(field.docValues != RpcLuceneDocValues.NUMERIC) { "text fields cannot use numeric DocValues" }
                if (field.docValues == RpcLuceneDocValues.SORTED) add(SortedDocValuesField(name, BytesRef(value.value)))
                if (field.stored) add(StoredField(name, value.value))
            }

            is RpcLuceneValue.LongNumber -> {
                require(field.index != RpcLuceneIndex.TEXT) { "numeric fields cannot use text indexing" }
                require(
                    field.docValues != RpcLuceneDocValues.SORTED,
                ) { "numeric fields cannot use sorted string DocValues" }
                if (field.docValues == RpcLuceneDocValues.NUMERIC) add(NumericDocValuesField(name, value.value))
                if (field.index == RpcLuceneIndex.EXACT) add(LongPoint(name, value.value))
                if (field.stored) add(StoredField(name, value.value))
            }
        }
    }

    private fun readStoredDocument(document: Document) =
        RpcLuceneStoredDocument(
            document.fields.groupBy { it.name() }.mapValues { (_, fields) ->
                fields.map { field ->
                    field.numericValue()?.let { RpcLuceneValue.LongNumber(it.toLong()) }
                        ?: RpcLuceneValue.Text(field.stringValue())
                }
            },
        )
    private fun buildQuery(source: RpcLuceneQuery) =
        BooleanQuery.Builder().apply {
            source.mustLong.forEach { (field, value) ->
                add(
                    LongPoint.newExactQuery(field, value),
                    BooleanClause.Occur.MUST,
                )
            }
            source.mustNotLong.forEach { (field, value) ->
                add(
                    LongPoint.newExactQuery(field, value),
                    BooleanClause.Occur.MUST_NOT,
                )
            }
            source.mustLongSet.forEach { (field, values) ->
                add(
                    LongPoint.newSetQuery(field, values),
                    BooleanClause.Occur.MUST,
                )
            }
            source.mustKeyword.forEach { (field, value) ->
                add(TermQuery(Term(field, value)), BooleanClause.Occur.MUST)
            }
            source.text.forEach { text -> addTextQuery(text) }
            val filters =
                listOf(
                    source.mustLong.size,
                    source.mustLongSet.size,
                    source.mustKeyword.size,
                    source.text.size,
                )
            if (filters.all { it == 0 }) {
                add(MatchAllDocsQuery.INSTANCE, BooleanClause.Occur.MUST)
            }
        }.build()
}

fun main() {
    val port = System.getenv("LUCENE_RPC_PORT")?.toIntOrNull() ?: 8821
    LoggerFactory.getLogger("lucene-service").info("Lucene service starting: RPC port={}", port)
    val service = LuceneRpcImpl(Paths.get(System.getenv("LUCENE_BASE_PATH") ?: "/data"))
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        install(WebSockets) { maxFrameSize = Long.MAX_VALUE }
        install(Krpc)
        routing {
            get(
                "/health",
            ) {
                call.respondText(
                    "ok",
                )
            }
            rpc("/rpc") {
                rpcConfig { serialization { json() } }
                registerService<LuceneRpc> { service }
            }
        }
    }.start(wait = true)
}
