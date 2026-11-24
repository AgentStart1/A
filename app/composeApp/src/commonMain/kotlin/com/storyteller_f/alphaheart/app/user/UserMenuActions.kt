/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.user

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.AppNav
import com.storyteller_f.alphaheart.app.common.finishSignOut
import com.storyteller_f.alphaheart.app.utils.unregisterPushService
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.getUserInfoByAid
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.signOut
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.UserInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

class UserInfoPreviewProvider : PreviewParameterProvider<UserInfo> {
    override val values: Sequence<UserInfo>
        get() = sequenceOf(UserInfo.EMPTY.copy(nickname = "hello"))
}

suspend fun AppGlobalDialogController.signOut(uiViewModel: UIViewModel, appNav: AppNav) {
    val address = uiViewModel.instance.value.address
    useResult {
        request {
            signOut()
        }
    }.onSuccess {
        uiViewModel.historyManager.exitSession(address)
        unregisterPushService()
        uiViewModel.logout()
        appNav.finishSignOut()
    }
}

internal suspend fun refreshMyInfo(my: UserInfo?, sessionManager: UserSessionManager) {
    try {
        val sessionModel = sessionManager.model
        if (my == null) {
            sessionManager.restoreSession().onSuccess { sessionModel.updateUser(it) }
        } else {
            val aid = my.aid
            val userInfo =
                if (aid.isNullOrBlank()) {
                    sessionManager.getUserInfo(my.id)
                } else {
                    sessionManager.getUserInfoByAid(aid)
                }.getOrThrow()
            sessionModel.updateUser(userInfo)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e(e) {
            "refresh user info"
        }
    }
}
