/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun FileIcon(it: FileInfo) {
    val contentType = it.contentType
    val modifier = Modifier.size(40.dp)
    when {
        contentType.startsWith("image/") -> {
            val fallback = rememberVectorPainter(Icons.Default.Image)
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
            Icon(Icons.Default.VideoFile, null, modifier)

        contentType.startsWith("audio/") ->
            Icon(Icons.Default.AudioFile, null, modifier)

        else -> Icon(Icons.Default.AttachFile, null, modifier)
    }
}
