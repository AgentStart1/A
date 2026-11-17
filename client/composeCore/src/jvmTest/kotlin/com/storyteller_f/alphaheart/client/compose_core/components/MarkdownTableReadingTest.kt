/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TABLE_HEADER = "窄屏表格的这一列完整标题应当自动换行而不能省略文字"
private const val TABLE_BODY = "图片、音频、视频与 PDF"

@OptIn(ExperimentalTestApi::class)
internal class MarkdownTableReadingTest {
    @Test
    fun tableHeaderRemainsReadableAcrossWidths() = assertTableCellReadable(TABLE_HEADER)

    @Test
    fun tableBodyRemainsReadableAcrossWidths() = assertTableCellReadable(TABLE_BODY)

    private fun assertTableCellReadable(text: String) =
        runComposeUiTest {
            val width = mutableStateOf(350.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.value)) {
                        CustomMarkdown(
                            plain = "| 类型 | $TABLE_HEADER |\n| --- | --- |\n| 附件 | $TABLE_BODY |",
                            isEmbed = false,
                            imageTransformer = tableNoImages,
                            dimensionMap = persistentMapOf(),
                            codeFence = { error("This table contains no code fence") },
                        )
                    }
                }
            }
            for (size in listOf(350.dp, 900.dp, 350.dp)) {
                runOnIdle { width.value = size }
                val layouts = mutableListOf<TextLayoutResult>()
                onNodeWithText(text, substring = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    assertTrue(it(layouts))
                }
                val layout = layouts.single()
                assertFalse(layout.hasVisualOverflow, "Table cell must show its complete text at $size")
                if (size == 350.dp) assertTrue(layout.lineCount > 1)
                onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)).assertExists()
            }
        }
}

private val tableNoImages =
    object : ImageTransformer {
        @Composable
        override fun transform(link: String): ImageData = error("This table contains no images")
    }
