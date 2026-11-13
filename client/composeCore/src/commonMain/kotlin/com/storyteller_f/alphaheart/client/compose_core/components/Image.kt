/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.model.ImageData
import com.storyteller_f.alphaheart.client.compose_core.common.LocalClient
import io.github.aakira.napier.Napier

@Composable
fun CommonImage(
    model: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    SubcomposeAsyncImage(
        globalLoader(model),
        contentDescription = contentDescription,
        modifier,
        loading = {
            ImageLoading()
        },
        error = {
            ImageError()
        },
        contentScale = contentScale,
    )
}

@Composable
fun ImageError() {
    CustomIcon(IconRes.Vector(Icons.Default.Error))
}

@Composable
fun ImageLoading() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize(), Alignment.Center) {
        val size = minOf(this.maxWidth, this.maxHeight, 20.dp)
        val strokeWidth = if (size > 20.dp) 2.dp else 1.dp
        CircularProgressIndicator(modifier = Modifier.size(size), strokeWidth = strokeWidth)
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun globalLoader(url: String): ImageRequest {
    val client = LocalClient.current
    val platformContext = LocalPlatformContext.current
    return remember(url) {
        ImageRequest.Builder(platformContext).data(url).crossfade(true).fetcherFactory(
            KtorNetworkFetcherFactory(client),
        ).build()
    }
}

@Composable
fun CustomMarkdownImage(imageData: ImageData) {
    val painter = imageData.painter
    if (painter is AsyncImagePainter) {
        val state by painter.state.collectAsState()
        when (val s = state) {
            is AsyncImagePainter.State.Empty,
            is AsyncImagePainter.State.Loading,
            -> {
                ImageLoading()
            }

            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = imageData.contentDescription,
                    modifier = imageData.modifier,
                    alignment = imageData.alignment,
                    contentScale = imageData.contentScale,
                    alpha = imageData.alpha,
                    colorFilter = imageData.colorFilter,
                )
            }

            is AsyncImagePainter.State.Error -> {
                Napier.e(s.result.throwable) {
                    "CustomMarkdownImage"
                }
                ImageError()
            }
        }
    } else {
        Image(
            painter = painter,
            contentDescription = imageData.contentDescription,
            modifier = imageData.modifier,
            alignment = imageData.alignment,
            contentScale = imageData.contentScale,
            alpha = imageData.alpha,
            colorFilter = imageData.colorFilter,
        )
    }
}
