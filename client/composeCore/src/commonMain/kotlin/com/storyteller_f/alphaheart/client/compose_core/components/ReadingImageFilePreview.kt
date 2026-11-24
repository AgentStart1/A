/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Scaling
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.ZoomIn
import com.composables.icons.lucide.ZoomOut
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.image_fit_window
import com.storyteller_f.alphaheart.client.compose_core.image_original_size
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_in
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_out
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReadingImageFilePreview(
    model: Any?,
    description: String,
    onSuccess: (AsyncImagePainter.State.Success) -> Unit = {},
    onError: (AsyncImagePainter.State.Error) -> Unit = {},
    onLongPress: ((Offset) -> Unit)? = null,
) {
    val zoomState = rememberCoilZoomState()
    val zoom = zoomState.zoomable
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth().testTag("image-preview-tools"), Arrangement.spacedBy(4.dp)) {
            val ready = zoom.contentSize.width > 0
            ImageAction(Lucide.Scan, stringResource(Res.string.image_fit_window), ready) {
                scope.launch { zoom.reset() }
            }
            ImageAction(Lucide.Scaling, stringResource(Res.string.image_original_size), ready) {
                val originalWidth = zoom.contentOriginSize.width.takeIf { it > 0 } ?: zoom.contentSize.width
                scope.launch { zoom.scale(originalWidth.toFloat() / zoom.contentSize.width, animated = true) }
            }
            ImageAction(Lucide.ZoomOut, stringResource(Res.string.pdf_zoom_out), ready) {
                scope.launch { zoom.scale(zoom.transform.scaleX / 1.25f, animated = true) }
            }
            ImageAction(Lucide.ZoomIn, stringResource(Res.string.pdf_zoom_in), ready) {
                scope.launch { zoom.scale(zoom.transform.scaleX * 1.25f, animated = true) }
            }
        }
        CoilZoomAsyncImage(
            model = model,
            contentDescription = description,
            modifier = Modifier.weight(1f).fillMaxWidth().appiumSemantics(description = description),
            zoomState = zoomState,
            contentScale = ContentScale.Inside,
            onSuccess = onSuccess,
            onError = onError,
            onLongPress = onLongPress,
        )
    }
}

@Composable
private fun ImageAction(icon: ImageVector, label: String, enabled: Boolean, action: () -> Unit) {
    ReadingIconButton(
        action,
        Modifier.appiumSemantics(description = label, onClick = action.takeIf { enabled }),
        enabled = enabled,
    ) {
        ReadingIcon(icon, label)
    }
}
