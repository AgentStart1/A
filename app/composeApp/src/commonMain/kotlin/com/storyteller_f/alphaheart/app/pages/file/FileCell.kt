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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.audio_content_description
import com.storyteller_f.alphaheart.app.copy_name
import com.storyteller_f.alphaheart.app.file_content_description
import com.storyteller_f.alphaheart.app.file_refs
import com.storyteller_f.alphaheart.app.image_dimension
import com.storyteller_f.alphaheart.app.image_requires_crop_hint
import com.storyteller_f.alphaheart.app.video_content_description
import com.storyteller_f.alphaheart.app.view
import com.storyteller_f.alphaheart.client.compose_core.components.CustomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.FileIcon
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.copyText
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.checkMediaFileDimensionRatioMatch
import com.storyteller_f.shared.utils.formatTime
import kotlinx.coroutines.launch
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun FileCell(fileInfo: FileInfo?, requiredDimension: Dimension? = null, clickItem: (List<FileInfo>) -> Unit) {
    if (fileInfo != null) {
        var expanded by remember { mutableStateOf(false) }
        val labels = CreateFileCellLabels(fileInfo, requiredDimension)
        val click = { clickItem(listOf(fileInfo)) }
        val expand = { expanded = true }
        Row(
            modifier =
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                contentDescription = labels.describe(fileInfo.name)
            }.appiumSemantics(text = fileInfo.name, onClick = click, onLongClick = expand)
                .combinedClickable(role = Role.Button, onLongClick = expand, onClick = click),
        ) {
            FileIcon(fileInfo)
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(fileInfo.name, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    Text(labels.modified, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(labels.size, style = MaterialTheme.typography.labelSmall)
                }
                labels.dimension?.let { dimension ->
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(dimension, style = MaterialTheme.typography.labelSmall)
                }
                labels.warning?.let { warning ->
                    Text(warning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        FileCellMenu(expanded, {
            expanded = it
        }, fileInfo)
    }
}

private data class FileCellLabels(
    val modified: String,
    val size: String,
    val dimension: String?,
    val warning: String?,
) {
    fun describe(name: String) = listOfNotNull(name, modified, size, dimension, warning).joinToString(", ")
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
    return FileCellLabels(file.lastModified.formatTime(), HumanReadable.fileSize(file.size), dimensionText, warning)
}

@Composable
fun FileCellMenu(expanded: Boolean, updateExpanded: (Boolean) -> Unit, fileInfo: FileInfo) {
    val appNavFactory = LocalAppNavFactory.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            updateExpanded(false)
        },
    ) {
        DropdownMenuItem(
            leadingIcon = {
                CustomIcon(IconRes.Vector(Icons.Default.Fullscreen))
            },
            text = { Text(stringResource(Res.string.view)) },
            onClick = {
                updateExpanded(false)
                appNavFactory.newAppNav().gotoMedia(fileInfo)
            },
        )
        val clipboardManager = LocalClipboard.current
        val toast = LocalToaster.current
        val scope = rememberCoroutineScope()
        DropdownMenuItem(
            leadingIcon = {
                CustomIcon(IconRes.Vector(Icons.Default.Fullscreen))
            },
            text = { Text(stringResource(Res.string.copy_name)) },
            onClick = {
                updateExpanded(false)
                scope.launch {
                    clipboardManager.copyText(fileInfo.name, toast)
                }
            },
        )
        DropdownMenuItem(
            leadingIcon = {
                CustomIcon(IconRes.Vector(Icons.Default.AttachFile))
            },
            text = { Text(stringResource(Res.string.file_refs)) },
            onClick = {
                updateExpanded(false)
                appNavFactory.newAppNav().gotoFileRefs(fileInfo.id)
            },
        )
    }
}

@Composable
fun UploadIcon(contentType: String) {
    val modifier = Modifier.padding(4.dp)
    when {
        contentType.startsWith("audio") ->
            Icon(
                Icons.Default.AudioFile,
                contentDescription = stringResource(Res.string.audio_content_description),
            )

        contentType.startsWith("video") ->
            Icon(
                Icons.Default.VideoFile,
                contentDescription = stringResource(Res.string.video_content_description),
            )

        else ->
            Icon(
                Icons.Default.AttachFile,
                contentDescription = stringResource(Res.string.file_content_description),
                modifier = modifier,
            )
    }
}
