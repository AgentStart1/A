/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.close
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseSheet(
    showSheet: Boolean,
    sheetState: SheetState,
    hideSheet: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!showSheet) return
    key(sheetState) {
        Dialog(hideSheet, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Surface(
                    Modifier.widthIn(max = 620.dp).fillMaxWidth().heightIn(max = maxHeight),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    androidx.compose.foundation.layout.Box {
                        Column(Modifier.fillMaxWidth().padding(24.dp), content = content)
                        IconButton(hideSheet, Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            Icon(actionIcon(ActionGlyph.X), stringResource(Res.string.close))
                        }
                    }
                }
            }
        }
    }
}
