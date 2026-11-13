/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.common

import androidx.compose.runtime.compositionLocalOf
import io.ktor.client.HttpClient

val LocalClient =
    compositionLocalOf<HttpClient> {
        error("no client")
    }
