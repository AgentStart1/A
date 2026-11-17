/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.lucene

import com.storyteller_f.services.lucene.api.RpcLuceneTextQuery
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.WildcardQuery
import org.apache.lucene.util.QueryBuilder
import kotlin.math.pow

internal fun BooleanQuery.Builder.addTextQuery(text: RpcLuceneTextQuery, analyzer: Analyzer) {
    require(text.fields.isNotEmpty()) { "text query has no fields" }
    val keywords =
        text.word.trim().split(Regex("(?U)\\s+"))
            .filter { it.isNotBlank() }
    val queryBuilder = QueryBuilder(analyzer)
    keywords.forEachIndexed { position, keyword ->
        val isLiteral = keyword.none { it == '*' || it == '?' || it == '\\' }
        val alternatives = BooleanQuery.Builder()
        text.fields.forEachIndexed { fieldPosition, field ->
            val fieldBoost = 10f.pow(text.fields.lastIndex - fieldPosition)
            val boost = fieldBoost * (keywords.size - position)
            val normalized = analyzer.normalize(field, keyword).utf8ToString()
            alternatives.add(BoostQuery(WildcardQuery(Term(field, "*$normalized*")), boost), BooleanClause.Occur.SHOULD)
            // A literal can span multiple index tokens (CJK or mixed scripts). Use
            // the indexing analyzer, but retain explicit wildcard syntax as term patterns.
            if (isLiteral) {
                queryBuilder.createBooleanQuery(field, keyword, BooleanClause.Occur.MUST)?.let {
                    alternatives.add(BoostQuery(it, boost), BooleanClause.Occur.SHOULD)
                }
            }
            if (keywords.size == 1) {
                val prefixBoost = if (text.fields.size == 1) 10f else 100f
                alternatives.add(
                    BoostQuery(PrefixQuery(Term(field, normalized)), boost * prefixBoost),
                    BooleanClause.Occur.SHOULD,
                )
            }
        }
        add(alternatives.build(), BooleanClause.Occur.MUST)
    }
}
