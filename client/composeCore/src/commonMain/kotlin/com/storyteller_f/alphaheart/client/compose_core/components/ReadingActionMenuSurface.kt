/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.close
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.LocalAdaptiveOverlay
import org.jetbrains.compose.resources.stringResource

/** Reading dialog chrome; action menus retain their default compact width. */
@Composable
fun ReadingActionMenuSurface(
    onDismiss: () -> Unit,
    heading: @Composable () -> Unit,
    compact: Boolean? = null,
    maxWidth: Dp = 320.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val padding = if (compact ?: (windowWidth <= 600.dp)) 18.dp else 20.dp
    val adaptive = LocalAdaptiveOverlay.current
    val width = if (adaptive) Modifier else Modifier.widthIn(max = maxWidth)
    Surface(
        width.fillMaxWidth().testTag("action-menu-surface"),
        shape = RoundedCornerShape(16.dp),
        color = if (adaptive) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer,
        border = if (adaptive) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(1.dp).padding(padding)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f)) { heading() }
                Box(
                    Modifier.size(29.dp).testTag("action-menu-close")
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    ReadingIcon(actionIcon(ActionGlyph.X), stringResource(Res.string.close), Modifier.size(17.dp))
                }
            }
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}
