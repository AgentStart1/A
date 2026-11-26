/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun FileIcon(it: FileInfo) {
    val contentType = it.contentType
    val modifier = Modifier.size(40.dp)
    when {
        contentType.startsWith("image/") -> {
            val fallback = rememberVectorPainter(actionIcon(ActionGlyph.IMAGE))
            AsyncImage(
                it.url,
                null,
                modifier = modifier.clip(RoundedCornerShape(5.dp)),
                contentScale = ContentScale.Crop,
                placeholder = fallback,
                error = fallback,
                fallback = fallback,
            )
        }

        contentType.startsWith("video/") ->
            Icon(actionIcon(ActionGlyph.FILE_VIDEO), null, modifier)

        contentType.startsWith("audio/") ->
            Icon(actionIcon(ActionGlyph.FILE_AUDIO), null, modifier)

        else -> Icon(actionIcon(ActionGlyph.PAPERCLIP), null, modifier)
    }
}
