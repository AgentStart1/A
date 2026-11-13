/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.UserInfo

@Composable
fun UserCell(userInfo: UserInfo?, onClick: (() -> Unit)? = null) {
    Row(
        modifier =
        Modifier.fillMaxWidth().let { m ->
            if (onClick != null) m.clickable { onClick() } else m
        }.appiumSemantics(text = userInfo?.address, onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserIcon(
            setClickEvent = false,
            avatarUrl = userInfo?.avatar?.url,
        ) {
        }
        if (userInfo != null) {
            Column {
                Text(userInfo.nickname, style = MaterialTheme.typography.titleMedium)
                val aid = userInfo.aid
                if (aid != null) {
                    Text(CoreStrings.aid(aid), style = MaterialTheme.typography.labelSmall)
                } else {
                    Text(CoreStrings.ad(userInfo.address), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
