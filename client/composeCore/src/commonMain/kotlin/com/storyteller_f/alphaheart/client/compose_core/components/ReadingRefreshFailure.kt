/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CircleAlert
import com.composables.icons.lucide.Lucide
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.refresh_failed_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReadingRefreshFailure(retry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.padding(top = 8.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant),
            shadowElevation = 3.dp,
        ) {
            ReadingTextButton(retry, Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                ReadingIcon(Lucide.CircleAlert, null, Modifier.size(14.dp), tint = colors.onSurfaceVariant)
                Text(
                    stringResource(Res.string.refresh_failed_retry),
                    Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface,
                )
            }
        }
        Canvas(Modifier.align(Alignment.TopCenter).offset(y = (-4).dp).size(8.dp)) {
            val top = Offset(size.width / 2, 0f)
            val left = Offset(0f, size.height / 2)
            val right = Offset(size.width, size.height / 2)
            drawPath(
                Path().apply {
                    moveTo(left.x, size.height)
                    lineTo(left.x, left.y)
                    lineTo(top.x, top.y)
                    lineTo(right.x, right.y)
                    lineTo(right.x, size.height)
                    close()
                },
                colors.surfaceContainer,
            )
            drawLine(colors.outlineVariant, left, top, 1.dp.toPx())
            drawLine(colors.outlineVariant, top, right, 1.dp.toPx())
        }
    }
}
