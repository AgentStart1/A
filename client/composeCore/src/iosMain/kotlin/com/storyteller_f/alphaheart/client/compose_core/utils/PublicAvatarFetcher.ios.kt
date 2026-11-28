/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.Fetcher
import coil3.network.DeDupeConcurrentRequestStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.storyteller_f.alphaheart.client.core.getClient

@OptIn(ExperimentalCoilApi::class)
internal actual fun createPublicAvatarFetcher(): Fetcher.Factory<Uri> =
    KtorNetworkFetcherFactory(getClient {}, DeDupeConcurrentRequestStrategy())
