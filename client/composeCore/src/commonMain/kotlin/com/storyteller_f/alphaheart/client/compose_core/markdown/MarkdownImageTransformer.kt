/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.markdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.storyteller_f.alphaheart.client.compose_core.components.imageRequestInMarkdown
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.FileInfo

class CustomCoil3ImageTransformerImpl(private val mediaMap: Map<String, FileInfo>, val onClick: (FileInfo) -> Unit) :
    ImageTransformer {
    @OptIn(ExperimentalRichTextApi::class)
    @Composable
    override fun transform(link: String): ImageData =
        if (link.startsWith("file:///")) {
            val model = link.substring(7)
            val painter = rememberAsyncImagePainter(model = model)
            ImageData(painter)
        } else {
            val info = mediaMap[link]
            val model = imageRequestInMarkdown(info)
            val painter = rememberAsyncImagePainter(model = model)
            ImageData(
                painter,
                modifier =
                Modifier.appiumSemantics(
                    description = info?.let { "open file ${it.name}" },
                    onClick = info?.let { { onClick(it) } },
                ).clip(RoundedCornerShape(10.dp)).clickable(info != null) {
                    info?.let { it1 -> onClick(it1) }
                },
            )
        }
}
