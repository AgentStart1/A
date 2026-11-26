/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_attachment
import com.storyteller_f.alphaheart.app.clear_input
import com.storyteller_f.alphaheart.app.composer_readonly
import com.storyteller_f.alphaheart.app.media.insertContent
import com.storyteller_f.alphaheart.app.message_placeholder
import com.storyteller_f.alphaheart.app.open_in_full
import com.storyteller_f.alphaheart.app.pages.topic.FilePicker
import com.storyteller_f.alphaheart.app.reply_placeholder
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.imeAnimation
import com.storyteller_f.shared.obj.ObjectTuple
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

@Composable
fun InputGroupInternal(
    mediaTarget: ObjectTuple,
    backgroundColor: Color,
    input: String,
    updateInput: (String) -> Unit,
    gotoCompose: () -> Unit,
    topContent: @Composable () -> Unit = {},
    sendButton: @Composable () -> Unit,
    enabled: Boolean = true,
    replyingToTopic: Boolean = false,
) {
    Box(
        Modifier.fillMaxWidth().background(backgroundColor.copy(alpha = 0.25f))
            .navigationBarsPadding().imePadding().imeAnimation(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 740.dp).fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            HorizontalDivider()
            Box(Modifier.fillMaxWidth()) {
                if (enabled) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InputGroupSuffix(input, updateInput, mediaTarget)
                        OutlinedTextField(
                            value = input,
                            onValueChange = updateInput,
                            placeholder = { Text(composerPlaceholder(replyingToTopic)) },
                            minLines = 1,
                            maxLines = 6,
                            modifier =
                            Modifier.weight(1f).appiumSemantics(
                                input = true,
                                inputValue = input,
                                onInputValueChange = updateInput,
                            ),
                            trailingIcon = {
                                Row {
                                    if (input.isNotEmpty()) {
                                        IconButton(
                                            { updateInput("") },
                                        ) { Icon(actionIcon(ActionGlyph.X), stringResource(Res.string.clear_input)) }
                                    }
                                    IconButton(
                                        gotoCompose,
                                    ) {
                                        Icon(
                                            actionIcon(ActionGlyph.MAXIMIZE_2),
                                            stringResource(Res.string.open_in_full),
                                        )
                                    }
                                }
                            },
                        )
                        Box(Modifier.size(44.dp).padding(bottom = 2.dp)) { sendButton() }
                    }
                } else {
                    ReadOnlyComposerNotice()
                }
                topContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputGroupSuffix(input: String, updateInput: (String) -> Unit, mediaTarget: ObjectTuple) {
    val uIViewModel = LocalUiViewModel.current
    val instance by uIViewModel.instance.collectAsState()
    val appNavFactory = LocalAppNavFactory.current
    val alreadySignIn = instance !is IAccountInstance.None
    var showSheet by remember {
        mutableStateOf(false)
    }

    val openAttachments = {
        if (alreadySignIn) {
            showSheet = true
        } else {
            appNavFactory.newAppNav().requestSignIn()
        }
    }
    val attachmentDescription = stringResource(Res.string.add_attachment)
    IconButton(
        openAttachments,
        modifier = Modifier.appiumSemantics(description = attachmentDescription, onClick = openAttachments),
    ) {
        Icon(actionIcon(ActionGlyph.PAPERCLIP), contentDescription = attachmentDescription)
    }
    FilePicker(
        showSheet,
        mediaTarget,
        onClickItems = { info ->
            showSheet = false
            insertContent(info.first(), input, updateInput)
        },
    ) {
        showSheet = false
    }
}

@Composable
private fun composerPlaceholder(replyingToTopic: Boolean): String =
    stringResource(
        if (replyingToTopic) Res.string.reply_placeholder else Res.string.message_placeholder,
    )

@Composable
private fun ReadOnlyComposerNotice() {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(actionIcon(ActionGlyph.LOCK), null)
        Text(
            stringResource(Res.string.composer_readonly),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
