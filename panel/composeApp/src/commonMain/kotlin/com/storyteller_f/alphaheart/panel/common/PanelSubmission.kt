/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogContext
import com.storyteller_f.alphaheart.client.core.RequestError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.onSuccess

/** Owns the request in the global dialog and publishes completion only after server acknowledgement. */
internal fun <C, T> CustomGlobalDialogController<GlobalDialogContext<C>>.submitPanelRequest(
    request: suspend C.() -> CustomResult<T, RequestError>,
    onSuccess: (T) -> Unit,
) {
    val controller = this
    launch {
        // The nested launch receiver's useResult only unwinds loading; the outer controller owns error display.
        controller.useResult { context.request(request) }.onSuccess(onSuccess)
    }
}
