/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.copy_name
import com.storyteller_f.alphaheart.app.file.FileCellLabels
import com.storyteller_f.alphaheart.app.file_actions
import com.storyteller_f.alphaheart.app.file_refs
import com.storyteller_f.alphaheart.app.image_dimension
import com.storyteller_f.alphaheart.app.image_requires_crop_hint
import com.storyteller_f.alphaheart.app.view
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.FileIcon
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAdaptiveOverlay
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.checkMediaFileDimensionRatioMatch
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@OptIn(ExperimentalTime::class)
@Composable
fun FileCell(
    fileInfo: FileInfo?,
    requiredDimension: Dimension? = null,
    previewFromMenu: Boolean = false,
    clickItem: (List<FileInfo>) -> Unit,
) {
    if (fileInfo != null) {
        var expanded by remember { mutableStateOf(false) }
        val labels = CreateFileCellLabels(fileInfo, requiredDimension)
        val click = { clickItem(listOf(fileInfo)) }
        val expand = { expanded = true }
        Row(
            modifier =
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = labels.describe(fileInfo.name, fileInfo.contentType)
            }.appiumSemantics(text = fileInfo.name, onClick = click, onLongClick = expand)
                .combinedClickable(role = Role.Button, onLongClick = expand, onClick = click),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FileIcon(fileInfo)
            Spacer(modifier = Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(fileInfo.name, style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    listOf(fileInfo.contentType, labels.size).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                )
                labels.dimension?.let { dimension ->
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(dimension, style = MaterialTheme.typography.labelSmall)
                }
                labels.warning?.let { warning ->
                    Text(warning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (expanded) FileCellMenu(fileInfo, previewFromMenu) { expanded = false }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun CreateFileCellLabels(file: FileInfo, requiredDimension: Dimension?): FileCellLabels {
    val dimension = file.dimension?.takeIf { file.contentType.startsWith("image/") }
    val dimensionText = dimension?.let { stringResource(Res.string.image_dimension, it.width, it.height) }
    val warning =
        if (dimension != null && requiredDimension != null &&
            !checkMediaFileDimensionRatioMatch(dimension, requiredDimension)
        ) {
            stringResource(Res.string.image_requires_crop_hint)
        } else {
            null
        }
    return FileCellLabels(
        file.lastModified.formatDisplayTime(),
        HumanReadable.fileSize(file.size),
        dimensionText,
        warning,
    )
}

@Composable
private fun FileCellMenu(fileInfo: FileInfo, previewFromMenu: Boolean, onDismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    ReadingAdaptiveOverlay(onDismiss, ReadingOverlaySize.COMPACT) {
        ReadingActionMenuSurface(
            onDismiss = onDismiss,
            heading = { Text(stringResource(Res.string.file_actions), style = MaterialTheme.typography.titleMedium) },
        ) {
            IconActionMenu(twoActions = !previewFromMenu) {
                if (previewFromMenu) {
                    ButtonNav(actionIcon(ActionGlyph.MAXIMIZE_2), stringResource(Res.string.view)) {
                        onDismiss()
                        appNavFactory.newAppNav().gotoMedia(fileInfo)
                    }
                }
                ButtonNav(actionIcon(ActionGlyph.COPY), stringResource(Res.string.copy_name)) {
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        clipboard.copyText(fileInfo.name, toast)
                        onDismiss()
                    }
                }
                ButtonNav(actionIcon(ActionGlyph.LINK), stringResource(Res.string.file_refs)) {
                    onDismiss()
                    appNavFactory.newAppNav().gotoFileRefs(fileInfo.id)
                }
            }
        }
    }
}

@Composable
fun UploadIcon(contentType: String) {
    val modifier = Modifier.padding(4.dp)
    when {
        contentType.startsWith("audio") ->
            Icon(
                Icons.Default.AudioFile,
                contentDescription = null,
            )

        contentType.startsWith("video") ->
            Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
            )

        else ->
            Icon(
                Icons.Default.AttachFile,
                contentDescription = null,
                modifier = modifier,
            )
    }
}
