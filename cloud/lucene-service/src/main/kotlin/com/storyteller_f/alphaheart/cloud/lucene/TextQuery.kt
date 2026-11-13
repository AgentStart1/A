/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.lucene

import com.storyteller_f.services.lucene.api.RpcLuceneTextQuery
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.PrefixQuery
import org.apache.lucene.search.WildcardQuery
import kotlin.math.pow

internal fun BooleanQuery.Builder.addTextQuery(text: RpcLuceneTextQuery) {
    require(text.fields.isNotEmpty()) { "text query has no fields" }
    val keywords =
        text.word.trim().lowercase().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    keywords.forEachIndexed { position, keyword ->
        val alternatives = BooleanQuery.Builder()
        text.fields.forEachIndexed { fieldPosition, field ->
            val fieldBoost = 10f.pow(text.fields.lastIndex - fieldPosition)
            val boost = fieldBoost * (keywords.size - position)
            alternatives.add(BoostQuery(WildcardQuery(Term(field, "*$keyword*")), boost), BooleanClause.Occur.SHOULD)
            if (keywords.size == 1) {
                val prefixBoost = if (text.fields.size == 1) 10f else 100f
                alternatives.add(
                    BoostQuery(PrefixQuery(Term(field, keyword)), boost * prefixBoost),
                    BooleanClause.Occur.SHOULD,
                )
            }
        }
        add(alternatives.build(), BooleanClause.Occur.MUST)
    }
}
