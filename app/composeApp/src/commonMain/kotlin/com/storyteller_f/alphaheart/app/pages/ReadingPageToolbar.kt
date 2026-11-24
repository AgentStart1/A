/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.getUserOverviewViewModel
import com.storyteller_f.alphaheart.app.pages.user.SelfUserIconWithDialog
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
internal fun ReadingPageToolbar(
    title: String,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
    showAccount: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val nav = LocalAppNavFactory.current
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton({ nav.newAppNav().back() }) { Icon(actionIcon(ActionGlyph.ARROW_LEFT), "back") }
                leading()
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (!subtitle.isNullOrBlank() && subtitle != title) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
                actions()
                if (showAccount) {
                    SelfUserIconWithDialog(LocalUserInfo.current, overviewHandler = getUserOverviewViewModel().handler)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
