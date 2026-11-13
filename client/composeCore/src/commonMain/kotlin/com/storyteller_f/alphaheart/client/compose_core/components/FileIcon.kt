/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.storyteller_f.shared.model.FileInfo

@Composable
fun FileIcon(it: FileInfo) {
    val contentType = it.contentType
    val modifier = Modifier.size(40.dp)
    if (contentType.startsWith("image") || contentType.startsWith("video")) {
        AsyncImage(
            it.url,
            it.name,
            modifier = modifier.clip(RoundedCornerShape(5.dp)),
            contentScale = ContentScale.Crop,
        )
    } else if (contentType.startsWith("audio")) {
        Icon(Icons.Default.AudioFile, "audio file", modifier)
    } else {
        Icon(Icons.Default.AttachFile, "other file", modifier)
    }
}
