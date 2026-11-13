/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.OnTitleCreated
import com.storyteller_f.alphaheart.app.common.createTitleComposeViewModel
import com.storyteller_f.alphaheart.app.error.TitleActionError
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch

@Composable
fun CommunityTitleComposePage(communityId: PrimaryKey) {
    LocalUserInfo.current ?: return
    CommunityTitleComposeInternal(communityId)
}

@Composable
private fun CommunityTitleComposeInternal(communityId: PrimaryKey) {
    val scope = communityId ob ObjectType.COMMUNITY
    val vm =
        createTitleComposeViewModel(
            initialScope = scope,
            initialType = com.storyteller_f.shared.model.TitleType.JOIN,
            lockScope = true,
            lockType = true,
        )
    val appNavFactory = LocalAppNavFactory.current
    val coroutineScope = rememberCoroutineScope()
    val globalDialog = LocalGlobalDialog.current

    CommonComposePage({
        coroutineScope.launch {
            globalDialog.useResult {
                createTitle(vm.buildNewTitle().getOrThrow())
            }.onSuccess {
                appNavFactory.newAppNav().back()
            }
        }
    }) {
        TitleComposeInternalEdit(vm)
    }
    TitleComposeSheet(vm)
}

private suspend fun AppGlobalDialogController.createTitle(
    newTitle: NewTitle,
): CustomResult<TitleDetail, TitleActionError> =
    request {
        createTitle(newTitle)
    }.onSuccess { title ->
        emitEvent(OnTitleCreated(title))
    }.mapError(TitleActionError::fromError)
