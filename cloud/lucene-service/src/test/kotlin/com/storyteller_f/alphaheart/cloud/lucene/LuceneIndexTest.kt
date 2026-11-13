/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.lucene

import com.storyteller_f.services.lucene.api.RpcLuceneDocValues
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneField
import com.storyteller_f.services.lucene.api.RpcLuceneIndex
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneSort
import com.storyteller_f.services.lucene.api.RpcLuceneSortType
import com.storyteller_f.services.lucene.api.RpcLuceneStoredDocument
import com.storyteller_f.services.lucene.api.RpcLuceneTextQuery
import com.storyteller_f.services.lucene.api.RpcLuceneValue
import kotlinx.coroutines.runBlocking
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.LongField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.FSDirectory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LuceneIndexTest {
    @Test
    fun `legacy fields retain their original names`() =
        runBlocking {
            val base = createTempDirectory("legacy-index-")
            FSDirectory.open(base.resolve("products")).use { directory ->
                IndexWriter(directory, IndexWriterConfig(StandardAnalyzer())).use { writer ->
                    writer.addDocument(
                        Document().apply {
                            add(LongField("id1", 3, Field.Store.YES))
                            add(TextField("title", "legacy", Field.Store.YES))
                        },
                    )
                }
            }
            val service = LuceneRpcImpl(base)
            assertEquals(
                listOf(
                    RpcLuceneStoredDocument(
                        mapOf(
                            "id1" to listOf(RpcLuceneValue.LongNumber(3)),
                            "title" to listOf(RpcLuceneValue.Text("legacy")),
                        ),
                    ),
                ),
                service.search("products", RpcLuceneQuery()).documents,
            )
        }

    @Test
    fun `generic fields filter text and numbers`() =
        withProducts { service, _, second ->
            val query =
                RpcLuceneQuery(
                    mustLong = mapOf("stock" to 3),
                    text = listOf(RpcLuceneTextQuery("apple", listOf("title"))),
                )
            assertEquals(listOf(second.stored()), service.search("products", query).documents)
        }

    @Test
    fun `generic fields sort numerically`() =
        withProducts { service, first, second ->
            val descending =
                RpcLuceneQuery(
                    sort = listOf(RpcLuceneSort("stock", RpcLuceneSortType.LONG, descending = true)),
                )
            assertEquals(listOf(second.stored(), first.stored()), service.search("products", descending).documents)
        }

    @Test
    fun `generic fields paginate sorted results`() =
        withProducts { service, first, _ ->
            val descending =
                RpcLuceneQuery(
                    sort = listOf(RpcLuceneSort("stock", RpcLuceneSortType.LONG, descending = true)),
                )
            assertEquals(
                listOf(first.stored()),
                service.search("products", descending.copy(offset = 1, size = 1)).documents,
            )
        }

    @Test
    fun `generic fields sort alphabetically`() =
        withProducts { service, first, second ->
            val alphabetical = RpcLuceneQuery(sort = listOf(RpcLuceneSort("title", RpcLuceneSortType.STRING)))
            assertEquals(listOf(second.stored(), first.stored()), service.search("products", alphabetical).documents)
        }

    @Test
    fun `delete removes only matching documents`() =
        withProducts { service, first, _ ->
            val query = RpcLuceneQuery(mustLong = mapOf("stock" to 3))
            service.delete("products", query)
            assertEquals(listOf(first.stored()), service.search("products", RpcLuceneQuery()).documents)
        }

    @Test
    fun `missing index has no search results`() =
        runBlocking {
            val service = LuceneRpcImpl(createTempDirectory("missing-index-"))
            assertEquals(0L, service.search("another", RpcLuceneQuery()).total)
        }

    @Test
    fun `clean rejects paths outside index root`() =
        runBlocking<Unit> {
            val service = LuceneRpcImpl(createTempDirectory("index-path-"))
            assertFailsWith<IllegalArgumentException> { service.clean("../outside") }
        }

    @Test
    fun `index storage and DocValues are independent`() =
        runBlocking {
            val service = LuceneRpcImpl(createTempDirectory("field-modes-"))
            val document =
                RpcLuceneDocument(
                    listOf(
                        text("title", "red apple", RpcLuceneIndex.TEXT),
                        text("code", "PRODUCT-1", RpcLuceneIndex.EXACT),
                        text("privateIndex", "hidden", RpcLuceneIndex.TEXT, stored = false),
                        text("description", "display only"),
                        number("quantity", 9),
                        number("stock", 2, RpcLuceneIndex.EXACT, stored = false),
                        number("rank", 1, stored = false, docValues = RpcLuceneDocValues.NUMERIC),
                    ),
                )
            service.save("products", listOf(document))
            assertEquals(listOf(document.stored()), service.search("products", RpcLuceneQuery()).documents)
            assertEquals(
                1L,
                service.search("products", RpcLuceneQuery(mustKeyword = mapOf("code" to "PRODUCT-1"))).total,
            )
            assertEquals(0L, service.search("products", RpcLuceneQuery(mustKeyword = mapOf("code" to "PRODUCT"))).total)
            assertEquals(1L, service.search("products", textQuery("hidden", "privateIndex")).total)
            assertEquals(0L, service.search("products", textQuery("display", "description")).total)
            assertEquals(1L, service.search("products", RpcLuceneQuery(mustLong = mapOf("stock" to 2))).total)
            assertEquals(0L, service.search("products", RpcLuceneQuery(mustLong = mapOf("quantity" to 9))).total)
            assertEquals(0L, service.search("products", RpcLuceneQuery(mustLong = mapOf("rank" to 1))).total)
        }

    @Test
    fun `documents need no ID and preserve repeated fields`() =
        runBlocking {
            val service = LuceneRpcImpl(createTempDirectory("anonymous-index-"))
            val document =
                RpcLuceneDocument(
                    listOf(
                        text("tag", "one", RpcLuceneIndex.EXACT),
                        text("tag", "two", RpcLuceneIndex.EXACT),
                        text("id1", "ordinary field"),
                        text("id2", "ordinary field"),
                        text("_payload", "ordinary field"),
                    ),
                )
            service.save("products", listOf(document))
            assertEquals(listOf(document.stored()), service.search("products", RpcLuceneQuery()).documents)
            assertEquals(
                listOf(document.stored()),
                service.search(
                    "products",
                    RpcLuceneQuery(mustKeyword = mapOf("tag" to "two")),
                ).documents,
            )
        }

    @Test
    fun `invalid field settings do not save partial batches`() =
        runBlocking {
            val service = LuceneRpcImpl(createTempDirectory("invalid-fields-"))
            val invalidFields =
                listOf(
                    number("number", 3, RpcLuceneIndex.TEXT),
                    number("number", 3, docValues = RpcLuceneDocValues.SORTED),
                    text("text", "invalid", docValues = RpcLuceneDocValues.NUMERIC),
                    text("", "invalid"),
                    text("unused", "invalid", stored = false),
                )
            for (field in invalidFields) {
                assertFailsWith<IllegalArgumentException> {
                    service.save("products", listOf(product("apple", 2), RpcLuceneDocument(listOf(field))))
                }
                assertEquals(0L, service.search("products", RpcLuceneQuery()).total)
            }
        }

    @Test
    fun `sort can use DocValues only and multiple fields`() =
        runBlocking {
            val service = LuceneRpcImpl(createTempDirectory("sort-index-"))
            fun item(label: String, group: String, rank: Long) =
                RpcLuceneDocument(
                    listOf(
                        text("label", label),
                        text("group", group, stored = false, docValues = RpcLuceneDocValues.SORTED),
                        number("rank", rank, stored = false, docValues = RpcLuceneDocValues.NUMERIC),
                    ),
                )
            val first = item("first", "b", 1)
            val second = item("second", "a", 1)
            val third = item("third", "a", 2)
            service.save("products", listOf(first, second, third))
            val query =
                RpcLuceneQuery(
                    sort =
                    listOf(
                        RpcLuceneSort("group", RpcLuceneSortType.STRING),
                        RpcLuceneSort("rank", RpcLuceneSortType.LONG, descending = true),
                    ),
                )
            assertEquals(
                listOf(third.stored(), second.stored(), first.stored()),
                service.search("products", query).documents,
            )
        }

    @Test
    fun `clean resets fields and keeps other indexes`() =
        runBlocking {
            val base = Files.createTempDirectory(Files.createDirectories(Path.of("build/test")), "reset-index-")
            val service = LuceneRpcImpl(base)
            val previous = RpcLuceneDocument(listOf(number("value", 1, docValues = RpcLuceneDocValues.NUMERIC)))
            FSDirectory.open(base.resolve("products")).use { directory ->
                IndexWriter(directory, IndexWriterConfig(StandardAnalyzer())).use { writer ->
                    writer.addDocument(Document().apply { add(LongField("value", 1, Field.Store.YES)) })
                }
            }
            service.save("other", listOf(previous))
            service.clean("products")
            assertEquals(0L, service.search("products", RpcLuceneQuery()).total)
            val replacement = RpcLuceneDocument(listOf(text("value", "new field type", RpcLuceneIndex.TEXT)))
            service.save("products", listOf(replacement))
            assertEquals(listOf(replacement.stored()), service.search("products", RpcLuceneQuery()).documents)
            assertEquals(listOf(previous.stored()), service.search("other", RpcLuceneQuery()).documents)
        }

    private fun withProducts(block: suspend (LuceneRpcImpl, RpcLuceneDocument, RpcLuceneDocument) -> Unit) =
        runBlocking {
            val service = LuceneRpcImpl(createTempDirectory("generic-index-"))
            val first = product("red apple", 2)
            val second = product("green apple", 3)
            service.save("products", listOf(first, second))
            block(service, first, second)
        }

    private fun product(title: String, stock: Long) =
        RpcLuceneDocument(
            listOf(
                text("title", title, RpcLuceneIndex.TEXT, docValues = RpcLuceneDocValues.SORTED),
                number("stock", stock, RpcLuceneIndex.EXACT, docValues = RpcLuceneDocValues.NUMERIC),
            ),
        )

    private fun text(
        name: String,
        value: String,
        index: RpcLuceneIndex = RpcLuceneIndex.NONE,
        stored: Boolean = true,
        docValues: RpcLuceneDocValues = RpcLuceneDocValues.NONE,
    ) = RpcLuceneField(name, RpcLuceneValue.Text(value), index, stored, docValues)

    private fun number(
        name: String,
        value: Long,
        index: RpcLuceneIndex = RpcLuceneIndex.NONE,
        stored: Boolean = true,
        docValues: RpcLuceneDocValues = RpcLuceneDocValues.NONE,
    ) = RpcLuceneField(name, RpcLuceneValue.LongNumber(value), index, stored, docValues)

    private fun textQuery(word: String, field: String) =
        RpcLuceneQuery(
            text = listOf(RpcLuceneTextQuery(word, listOf(field))),
        )
    private fun RpcLuceneDocument.stored() =
        RpcLuceneStoredDocument(
            fields.filter { it.stored }.groupBy { it.name }.mapValues { (_, fields) -> fields.map { it.value } },
        )
}
