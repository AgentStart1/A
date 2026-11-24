/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createTitleComposeViewModel
import com.storyteller_f.alphaheart.app.pages.community.CommunityIconWithSettingsMenu
import com.storyteller_f.alphaheart.app.title.communityTitleComposePageCreateTitle
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onSuccess
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

    val community by createCommunityViewModel(communityId).handler.data.collectAsState()
    CommonComposePage({
        coroutineScope.launch {
            globalDialog.useResult {
                communityTitleComposePageCreateTitle(vm.buildNewTitle().getOrThrow())
            }.onSuccess {
                appNavFactory.newAppNav().back()
            }
        }
    }, subtitle = community?.name, leading = {
        CommunityIconWithSettingsMenu(community)
    }) { actions ->
        TitleComposeInternalEdit(vm, actions)
    }
    TitleComposeSheet(vm)
}
