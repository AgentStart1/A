/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.graphics.ImageBitmap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import com.storyteller_f.alphaheart.client.compose_core.error.ImageError
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.FileInfo
import io.ktor.client.HttpClient

/**
 * Load a remote image as ImageBitmap using Coil and Ktor client.
 */
suspend fun getRemoteImageBitmap(
    sessionManager: UserSessionManager,
    context: PlatformContext,
    info: FileInfo,
): CustomResult<ImageBitmap, ImageError>? {
    val imageRequest =
        imageRequest(context, sessionManager.client, info)
            .androidAllowHardware(false)
            .build()
    val image =
        SingletonImageLoader.get(context)
            .execute(imageRequest)
            .image
    return image?.coilImageToImageBitmap()
}

@OptIn(ExperimentalCoilApi::class)
fun imageRequest(context: PlatformContext, client: HttpClient, info: FileInfo?) =
    ImageRequest.Builder(context)
        .fetcherFactory(KtorNetworkFetcherFactory(client))
        .data(info?.url)
