/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.lucene

import com.storyteller_f.services.lucene.api.RpcLuceneDocValues
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneField
import com.storyteller_f.services.lucene.api.RpcLuceneIndex
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneValue
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LuceneReplacementTest {
    @Test
    fun replacementPersistsAndKeepsUnmatchedDocuments() =
        runBlocking {
            withReplacementIndex { service, base ->
                service.save(
                    "products",
                    listOf(document("a", "old-one"), document("a", "old-two"), document("b", "keep")),
                )
                val query = RpcLuceneQuery(mustKeyword = mapOf("group" to "a"))
                service.replace("products", query, listOf(document("a", "new")))
                val all = service.search("products", RpcLuceneQuery())
                assertEquals(2L, all.total)
                assertEquals(
                    setOf("keep", "new"),
                    all.documents.map { it.fields.getValue("label").single() }.map {
                        (it as RpcLuceneValue.Text).value
                    }.toSet(),
                )
                val reopened = LuceneRpcImpl(base).search("products", query)
                assertEquals(1L, reopened.total)
                assertEquals(setOf("group", "label"), reopened.documents.single().fields.keys)
                assertEquals(listOf(RpcLuceneValue.Text("new")), reopened.documents.single().fields["label"])
            }
        }

    @Test
    fun failedIndexWriteRollsBackReplacement() =
        runBlocking {
            withReplacementIndex { service, _ ->
                service.save("products", listOf(document("a", "original")))
                val query = RpcLuceneQuery(mustKeyword = mapOf("group" to "a"))
                val duplicateValues =
                    document("a", "invalid").copy(
                        fields =
                        document("a", "invalid").fields +
                            listOf(1L, 2L).map { value ->
                                RpcLuceneField(
                                    "metric",
                                    RpcLuceneValue.LongNumber(value),
                                    docValues = RpcLuceneDocValues.NUMERIC,
                                )
                            },
                    )
                assertFailsWith<IllegalArgumentException> {
                    service.replace("products", query, listOf(duplicateValues))
                }
                val remaining = service.search("products", query)
                assertEquals(1L, remaining.total)
                assertEquals(listOf(RpcLuceneValue.Text("original")), remaining.documents.single().fields["label"])
            }
        }

    @Test
    fun invalidReplacementLeavesOriginalDocuments() =
        runBlocking {
            withReplacementIndex { service, _ ->
                service.save("products", listOf(document("a", "original")))
                val query = RpcLuceneQuery(mustKeyword = mapOf("group" to "a"))
                val invalid = RpcLuceneDocument(listOf(RpcLuceneField("", RpcLuceneValue.Text("invalid"))))
                assertFailsWith<IllegalArgumentException> { service.replace("products", query, listOf(invalid)) }
                val remaining = service.search("products", query)
                assertEquals(1L, remaining.total)
                assertEquals(listOf(RpcLuceneValue.Text("original")), remaining.documents.single().fields["label"])
            }
        }
}

private fun document(group: String, label: String) =
    RpcLuceneDocument(
        listOf(
            RpcLuceneField("group", RpcLuceneValue.Text(group), RpcLuceneIndex.EXACT),
            RpcLuceneField("label", RpcLuceneValue.Text(label), RpcLuceneIndex.TEXT),
        ),
    )

private suspend fun withReplacementIndex(block: suspend (LuceneRpcImpl, Path) -> Unit) {
    val root = Path.of("build", "test", "replacement").toAbsolutePath().normalize().createDirectories()
    val base = Files.createTempDirectory(root, "index-")
    try {
        block(LuceneRpcImpl(base), base)
    } finally {
        check(base.startsWith(root))
        base.toFile().deleteRecursively()
    }
}
