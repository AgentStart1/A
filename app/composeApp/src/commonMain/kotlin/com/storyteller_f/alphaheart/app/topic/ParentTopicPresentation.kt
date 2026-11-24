/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.storyteller_f.shared.utils.normalizeMarkdownLineEndings

@OptIn(ExperimentalRichTextApi::class)
internal fun parentReferenceSummary(markdown: String): String =
    RichTextState().setMarkdown(normalizeMarkdownLineEndings(markdown)).annotatedString.text.trim()

// Lucide 1.17.0 ArrowUpLeft, the exact icon version used by the approved preview (bundled ISC license).
internal val parentReferenceArrow by lazy {
    ImageVector.Builder("ParentReferenceArrow", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = PathParser().parsePathString("M7 17V7h10 M17 17 7 7").toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()
}
