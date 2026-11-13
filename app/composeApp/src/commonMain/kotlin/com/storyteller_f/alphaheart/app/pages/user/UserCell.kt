/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.UserInfo

@Composable
fun UserCell(userInfo: UserInfo?, iconSize: Dp = 40.dp, onClickCell: (UserInfo) -> Unit = {}) {
    val shape = RoundedCornerShape(8.dp)
    val openUser = userInfo?.let { info -> { onClickCell(info) } }
    val modifier =
        Modifier.fillMaxWidth()
            .clip(shape)
            .clickable(userInfo != null) { openUser?.invoke() }
            .padding(8.dp)
            .appiumSemantics(
                text = userInfo?.let { "${it.nickname} ${it.address}" },
                onClick = openUser,
            )
    UserCellInternal(modifier, userInfo, iconSize)
}

@Composable
fun UnboundedUserCell(userInfo: UserInfo?, iconSize: Dp = 40.dp) =
    UserCellInternal(Modifier.fillMaxWidth().padding(8.dp), userInfo, iconSize)

@Composable
fun UserCellInternal(modifier: Modifier, userInfo: UserInfo?, iconSize: Dp) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UserIconWithDialog(userInfo, setClickEvent = true, size = iconSize)
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
