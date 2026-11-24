/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.CommonImage
import com.storyteller_f.shared.model.CommunityInfo

@Composable
fun CommunityPoster(communityInfo: CommunityInfo?, shape: Shape = RoundedCornerShape(14.dp)) {
    val url = communityInfo?.poster?.url
    if (url != null) {
        CommonImage(url, contentDescription = "community poster", modifier = Modifier.fillMaxSize().clip(shape))
    } else {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shape).fillMaxSize())
    }
}
