/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.title

import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.common.OnTitleCreated
import com.storyteller_f.alphaheart.app.error.TitleActionError
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TitleDetail

internal suspend fun AppGlobalDialogController.communityTitleComposePageCreateTitle(
    newTitle: NewTitle,
): CustomResult<TitleDetail, TitleActionError> =
    request {
        communityTitleComposePageCreateTitle(newTitle)
    }.onSuccess { title ->
        emitEvent(OnTitleCreated(title))
    }.mapError(TitleActionError::fromError)
