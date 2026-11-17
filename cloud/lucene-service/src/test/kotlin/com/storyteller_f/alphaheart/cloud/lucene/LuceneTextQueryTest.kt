/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.lucene

import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneField
import com.storyteller_f.services.lucene.api.RpcLuceneIndex
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneTextQuery
import com.storyteller_f.services.lucene.api.RpcLuceneValue
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals

class LuceneTextQueryTest {
    @Test
    fun chineseLiteralMatchesAllAnalyzedCharacters() =
        withIndex(
            document("poster", field("name", "海报社区")),
            document("view", field("name", "海景社区")),
        ) { service ->
            assertEquals(listOf("poster"), service.labels("海报", "name"))
        }

    @Test
    fun mixedLatinChineseLiteralMatchesIndexedTokens() =
        withIndex(
            document("verified", field("name", "UI验证·海报社区")),
            document("design", field("name", "UI设计社区")),
        ) { service ->
            assertEquals(listOf("verified"), service.labels("UI验证", "name"))
        }

    @Test
    fun mixedWordsRequireBothAcrossFieldsAndUnicodeWhitespace() =
        withIndex(
            document("both", field("name", "UI验证"), field("body", "Programming")),
            document("one", field("name", "UI验证"), field("body", "Physics")),
        ) { service ->
            for (separator in listOf(" ", "\u3000", "\u00a0")) {
                assertEquals(listOf("both"), service.labels("UI验证${separator}programming", "name", "body"))
            }
        }

    @Test
    fun latinSubstringRemainsSearchable() =
        withIndex(
            document("programming", field("name", "Programming")),
            document("other", field("name", "Statistics")),
        ) { service ->
            assertEquals(listOf("programming"), service.labels("gram", "name"))
        }

    @Test
    fun prefixBoostStillRanksAheadOfEmbeddedMatch() =
        withIndex(
            document("embedded", field("name", "Metaprogramming")),
            document("prefix", field("name", "Programming")),
        ) { service ->
            assertEquals(listOf("prefix", "embedded"), service.labels("pro", "name"))
        }

    @Test
    fun explicitWildcardsRemainTermPatternsRatherThanAnalyzedWords() =
        withIndex(
            document("pattern", field("name", "Programming")),
            document("split-star", field("name", "pro gram")),
            document("split-question", field("name", "pro ram")),
        ) { service ->
            for (pattern in listOf("pro*gram", "pro?ram")) {
                assertEquals(listOf("pattern"), service.labels(pattern, "name"))
            }
        }

    @Test
    fun escapedWildcardRetainsLiteralMeaning() =
        withIndex(
            document("literal", field("code", "tag*value", RpcLuceneIndex.EXACT)),
            document("other", field("code", "tagXvalue", RpcLuceneIndex.EXACT)),
        ) { service ->
            assertEquals(listOf("literal"), service.labels("tag\\*value", "code"))
        }

    @Test
    fun supplementaryLetterPrefixUsesAnalyzerNormalization() =
        withIndex(document("unicode", field("name", "𐐀code"))) { service ->
            assertEquals(listOf("unicode"), service.labels("𐐀c", "name"))
        }

    @Test
    fun wildcardQuestionMatchesSupplementaryCodePoint() =
        withIndex(
            document("unicode", field("name", "𐐀code")),
            document("bare", field("name", "code")),
        ) { service ->
            assertEquals(listOf("unicode"), service.labels("?code", "name"))
        }

    @Test
    fun emojiAndChineseLiteralUseTheSameAnalysis() =
        withIndex(
            document("launch", field("name", "🚀计划")),
            document("other", field("name", "🚀发布")),
        ) { service ->
            assertEquals(listOf("launch"), service.labels("🚀计划", "name"))
        }

    @Test
    fun exactFileFieldDoesNotRequireTokenPositions() =
        withIndex(
            document(
                "file",
                field("nameExact", "image.png", RpcLuceneIndex.EXACT),
                field("name", "image.png"),
            ),
        ) { service ->
            assertEquals(listOf("file"), service.labels("image.png", "nameExact", "name"))
        }

    private fun withIndex(vararg documents: RpcLuceneDocument, block: suspend (LuceneRpcImpl) -> Unit) =
        runBlocking {
            val root = Files.createDirectories(Path.of("build", "test", "text-query").toAbsolutePath().normalize())
            val base = Files.createTempDirectory(root, "index-")
            try {
                val service = LuceneRpcImpl(base)
                service.save("entries", documents.toList())
                block(service)
            } finally {
                Files.walk(base).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach(Files::delete)
                }
            }
        }

    private fun document(label: String, vararg fields: RpcLuceneField) =
        RpcLuceneDocument(listOf(field("label", label, RpcLuceneIndex.NONE)) + fields)

    private fun field(name: String, value: String, index: RpcLuceneIndex = RpcLuceneIndex.TEXT) =
        RpcLuceneField(name, RpcLuceneValue.Text(value), index)

    private suspend fun LuceneRpcImpl.labels(word: String, vararg fields: String) =
        search("entries", RpcLuceneQuery(text = listOf(RpcLuceneTextQuery(word, fields.toList()))))
            .documents.map { (it.fields.getValue("label").single() as RpcLuceneValue.Text).value }
}
