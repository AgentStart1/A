/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job

internal fun closePanelClientWithScope(scope: CoroutineScope, client: HttpClient) {
    // Completion also runs immediately when restoration creates an account after cancellation.
    scope.coroutineContext.job.invokeOnCompletion { client.close() }
}
