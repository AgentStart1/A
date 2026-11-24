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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.UserAddressLine
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.UserInfo

@Composable
fun UserCell(userInfo: UserInfo?, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    var showIdentity by remember(userInfo?.id) { mutableStateOf(false) }
    val label = userInfo?.let { listOfNotNull(it.nickname, it.aid).joinToString(" ") }
    Row(
        modifier =
        Modifier.fillMaxWidth().panelDirectorySelection(selected).let { m ->
            if (onClick != null) m.clickable { onClick() } else m
        }.appiumSemantics(text = label, onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserIcon(setClickEvent = userInfo != null, avatarUrl = userInfo?.avatar?.url) {
            showIdentity = true
        }
        userInfo?.let { UserIdentityText(it) }
    }
    if (showIdentity && userInfo != null) {
        val dismiss = { showIdentity = false }
        ReadingBasicAlertDialog(dismiss) {
            ReadingActionMenuSurface(dismiss, heading = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UserIcon(false, userInfo.avatar?.url) {}
                    UserIdentityText(userInfo)
                }
            }) {
                UserAddressLine(userInfo.address)
            }
        }
    }
}

@Composable
private fun UserIdentityText(info: UserInfo) {
    Column {
        Text(info.nickname, style = MaterialTheme.typography.titleMedium)
        info.aid?.let { Text(CoreStrings.aid(it), style = MaterialTheme.typography.labelSmall) }
    }
}
