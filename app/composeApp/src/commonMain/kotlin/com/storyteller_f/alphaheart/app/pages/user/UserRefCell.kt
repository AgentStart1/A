/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.RefCellStateView
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.PrimaryKey

@Composable
fun UserRefCell(userId: PrimaryKey, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    val handler = LocalRefCellHandlerProvider.current.userHandler(userId)
    UserRefCellInternal(handler) {
        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
    }
}

@Composable
fun UserRefCell(userAid: String, onClick: ((UserInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    val handler = LocalRefCellHandlerProvider.current.userHandler(userAid)
    UserRefCellInternal(handler) {
        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
    }
}

@Composable
private fun UserRefCellInternal(handler: LoadingHandler<UserInfo>, onClick: ((UserInfo) -> Unit)? = null) {
    val userInfo by handler.data.collectAsState()
    val shape = RoundedCornerShape(10.dp)
    val appNavFactory = LocalAppNavFactory.current
    RefCellStateView(
        handler,
        modifier =
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, shape)
            .clip(shape)
            .clickable {
                userInfo?.let {
                    onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoUser(it.id)
                }
            },
    ) { info ->
        UnboundedUserCell(info)
    }
}
