/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.community

import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.common.OnCommunityUpdated
import com.storyteller_f.alphaheart.app.user.SettingOption
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.updateCommunityInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.type.PrimaryKey

internal suspend fun AppGlobalDialogController.resetCommunityMedia(communityId: PrimaryKey, body: UpdateCommunityBody) {
    useResult {
        request { updateCommunityInfo(communityId, body) }
    }.onSuccess { newInfo ->
        emitEvent(OnCommunityUpdated(newInfo))
    }
}

internal suspend fun AppGlobalDialogController.updateCommunity(
    communityId: PrimaryKey,
    string: String,
    showInputDialog: SettingOption?,
    closeDialog: () -> Unit,
) {
    val body =
        when (showInputDialog) {
            is SettingOption.Name -> {
                UpdateCommunityBody(name = string)
            }

            else -> {
                null
            }
        } ?: return
    useResult {
        request { updateCommunityInfo(communityId, body) }
    }.onSuccess { newInfo ->
        emitEvent(OnCommunityUpdated(newInfo))
        closeDialog()
    }
}
