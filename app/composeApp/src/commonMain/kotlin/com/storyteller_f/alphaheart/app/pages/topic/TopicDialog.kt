/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.belongs_to_community
import com.storyteller_f.alphaheart.app.belongs_to_room
import com.storyteller_f.alphaheart.app.components.ActionMenuOrigin
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.components.ReadingTranslationResult
import com.storyteller_f.alphaheart.app.copy
import com.storyteller_f.alphaheart.app.pages.community.CommunityDialogInternal
import com.storyteller_f.alphaheart.app.pages.community.CommunityMenuRefCell
import com.storyteller_f.alphaheart.app.pages.room.RoomDialogInternal
import com.storyteller_f.alphaheart.app.pages.room.RoomMenuRefCell
import com.storyteller_f.alphaheart.app.pin
import com.storyteller_f.alphaheart.app.published_at
import com.storyteller_f.alphaheart.app.service.NoOpGPT
import com.storyteller_f.alphaheart.app.service.buildGPT
import com.storyteller_f.alphaheart.app.service.buildTranslatePrompt
import com.storyteller_f.alphaheart.app.snapshot
import com.storyteller_f.alphaheart.app.success
import com.storyteller_f.alphaheart.app.topic.TopicMenuTarget
import com.storyteller_f.alphaheart.app.topic.TranslationState
import com.storyteller_f.alphaheart.app.topic.originalPlainContent
import com.storyteller_f.alphaheart.app.topic.pinOrUnpinTopic
import com.storyteller_f.alphaheart.app.topic_copy_unavailable
import com.storyteller_f.alphaheart.app.translate_action
import com.storyteller_f.alphaheart.app.translation_empty
import com.storyteller_f.alphaheart.app.translation_failed_retry
import com.storyteller_f.alphaheart.app.translation_failure_message
import com.storyteller_f.alphaheart.app.translation_loading
import com.storyteller_f.alphaheart.app.translation_platform_unavailable
import com.storyteller_f.alphaheart.app.translation_retry
import com.storyteller_f.alphaheart.app.translation_unavailable
import com.storyteller_f.alphaheart.app.unpin
import com.storyteller_f.alphaheart.app.utils.rememberStringPreference
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAdaptiveOverlay
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.client.compose_core.utils.getCurrentLanguage
import com.storyteller_f.alphaheart.client.core.createTopicSnapshot
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.ObjectType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDialog(topicInfo: TopicDetail?, showDialog: Boolean, dismiss: () -> Unit) {
    if (topicInfo != null && showDialog) {
        var target by remember(topicInfo.id) { mutableStateOf<TopicMenuTarget>(TopicMenuTarget.Actions) }
        TopicActionDialog(
            translating = target == TopicMenuTarget.Translation,
            dismiss = dismiss,
            menu = {
                when (val selected = target) {
                    TopicMenuTarget.Actions ->
                        TopicDialogInternal(
                            topicInfo,
                            dismiss,
                            onCommunityIcon = { target = TopicMenuTarget.Community(it) },
                            onRoomIcon = { target = TopicMenuTarget.Room(it) },
                            translate = { target = TopicMenuTarget.Translation },
                        )

                    is TopicMenuTarget.Community -> CommunityDialogInternal(selected.info, dismiss)

                    is TopicMenuTarget.Room ->
                        RoomDialogInternal(selected.info, dismiss) {
                            target = TopicMenuTarget.Community(it)
                        }

                    TopicMenuTarget.Translation -> Unit
                }
            },
            translation = {
                TranslationSurface(dismiss) { TopicTranslationBody(topicInfo) }
            },
        )
    }
}

/** Menu and translation replace one another within the same modal owner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopicActionDialog(
    translating: Boolean,
    dismiss: () -> Unit,
    menu: @Composable () -> Unit,
    translation: @Composable () -> Unit,
) {
    val size = if (translating) ReadingOverlaySize.FORM else ReadingOverlaySize.MENU
    ReadingAdaptiveOverlay(dismiss, size) {
        if (translating) translation() else menu()
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun TopicDialogInternal(
    topicInfo: TopicDetail,
    dismissDialog: () -> Unit,
    onCommunityIcon: (CommunityInfo) -> Unit,
    onRoomIcon: (RoomInfo) -> Unit,
    translate: () -> Unit,
) {
    val appNavFactory = LocalAppNavFactory.current
    ReadingActionMenuSurface(onDismiss = dismissDialog, heading = {
        Text(
            stringResource(Res.string.published_at, topicInfo.createdTime.formatDisplayTime()),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }) {
        when (topicInfo.rootType) {
            ObjectType.COMMUNITY ->
                ActionMenuOrigin(stringResource(Res.string.belongs_to_community)) {
                    CommunityMenuRefCell(topicInfo.rootId, onClickIcon = onCommunityIcon) {
                        dismissDialog()
                        appNavFactory.newAppNav().gotoCommunity(topicInfo.rootId, false)
                    }
                }

            ObjectType.ROOM ->
                ActionMenuOrigin(stringResource(Res.string.belongs_to_room)) {
                    RoomMenuRefCell(topicInfo.rootId, onClickIcon = onRoomIcon) {
                        dismissDialog()
                        appNavFactory.newAppNav().gotoRoom(topicInfo.rootId, false)
                    }
                }

            else -> {}
        }
        TopicDialogMenuList(topicInfo, dismissDialog, translate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDialogMenuList(topicInfo: TopicDetail, dismissDialog: () -> Unit, translate: () -> Unit) {
    val content = topicInfo.content
    IconActionMenu(topSpacing = 18.dp) {
        CopyButton(content)
        SnapshotButton(topicInfo)
        TopicPinButton(topicInfo, dismissDialog)
        TranslateButton(translate)
        FavoriteButton(topicInfo.favoriteId, topicInfo.tuple())
        SubscriptionButton(topicInfo.subscriptionId, topicInfo.tuple())
    }
}

@Composable
private fun CopyButton(content: TopicContent) {
    val toast = LocalToaster.current
    val unavailable = stringResource(Res.string.topic_copy_unavailable)
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ButtonNav(
        actionIcon(ActionGlyph.COPY),
        stringResource(Res.string.copy),
    ) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val source = content.originalPlainContent()
            if (source != null) {
                clipboardManager.copyText(source.plain, toast)
            } else {
                toast.showMessage(unavailable)
            }
        }
    }
}

@Composable
private fun SnapshotButton(topicInfo: TopicDetail) {
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    val instance by LocalUiViewModel.current.instance.collectAsState()
    val alreadyLoginIn = instance !is IAccountInstance.None
    if (!alreadyLoginIn) return
    val globalDialogController = LocalGlobalDialog.current
    ButtonNav(
        actionIcon(ActionGlyph.FILE_DOWN),
        stringResource(Res.string.snapshot),
    ) {
        scope.launch {
            globalDialogController.useResult {
                request { createTopicSnapshot(topicInfo.id) }
            }.onSuccess {
                toast.showSuccess(getString(Res.string.success))
            }
        }
    }
}

@Composable
private fun TopicPinButton(topicInfo: TopicDetail, dismissDialog: () -> Unit) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    ButtonNav(
        actionIcon(ActionGlyph.PIN),
        stringResource(if (topicInfo.isPin) Res.string.unpin else Res.string.pin),
    ) {
        scope.launch {
            globalDialogController.pinOrUnpinTopic(topicInfo).onSuccess {
                dismissDialog()
            }
        }
    }
}

@Composable
private fun TranslateButton(translate: () -> Unit) {
    ButtonNav(
        actionIcon(ActionGlyph.LANGUAGES),
        stringResource(Res.string.translate_action),
        onClick = translate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicTranslateSheet(showSheet: Boolean, topicInfo: TopicDetail, hideSheet: () -> Unit) {
    if (showSheet) {
        ReadingAdaptiveOverlay(hideSheet, ReadingOverlaySize.FORM) {
            TranslationSurface(hideSheet) { TopicTranslationBody(topicInfo) }
        }
    }
}

@Composable
private fun TranslationSurface(dismiss: () -> Unit, content: @Composable () -> Unit) {
    ReadingActionMenuSurface(
        onDismiss = dismiss,
        heading = { Text(stringResource(Res.string.translate_action), fontSize = 17.sp) },
        maxWidth = 620.dp,
    ) {
        Box(Modifier.padding(top = 22.dp)) { content() }
    }
}

@Composable
private fun TopicTranslationBody(topicInfo: TopicDetail) {
    val currentModel by rememberStringPreference("gpt_model", "")
    val language = getCurrentLanguage()
    val content = topicInfo.content.originalPlainContent()
    var attempt by remember(topicInfo.id) { mutableStateOf(0) }
    val translation by produceState<TranslationState>(
        TranslationState.Loading,
        content,
        currentModel,
        language,
        attempt,
    ) {
        value = TranslationState.Loading
        if (content == null) {
            value = TranslationState.Unavailable
            return@produceState
        }
        var output = ""
        try {
            val generator = withContext(Dispatchers.Default) { buildGPT() }
            if (generator is NoOpGPT) {
                value = TranslationState.PlatformUnavailable
                return@produceState
            }
            val flow =
                withContext(Dispatchers.Default) {
                    generator.generate(currentModel, buildTranslatePrompt(content.plain, language)).getOrThrow()
                }
            flow.collect {
                output += it.text
                value = TranslationState.Streaming(output)
            }
            value = if (output.isBlank()) TranslationState.Empty else TranslationState.Complete(output)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            Napier.w(failure) { "Topic translation failed" }
            value = TranslationState.Failed(output)
        }
    }
    TranslationContent(translation, topicInfo) { attempt += 1 }
}

@Composable
private fun TranslationContent(state: TranslationState, topicInfo: TopicDetail, retry: () -> Unit) {
    ReadingTranslationResult {
        when (state) {
            TranslationState.Loading ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(Res.string.translation_loading))
                }

            TranslationState.Unavailable -> Text(stringResource(Res.string.translation_unavailable))

            TranslationState.PlatformUnavailable -> TranslationPlatformUnavailable()

            TranslationState.Empty -> TextButton(retry) { Text(stringResource(Res.string.translation_empty)) }

            is TranslationState.Failed -> {
                if (state.text.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(Res.string.translation_failure_message))
                        ReadingOutlinedButton(retry) { Text(stringResource(Res.string.translation_retry)) }
                    }
                } else {
                    TranslatedOutput(topicInfo, state.text, retry)
                }
            }

            is TranslationState.Streaming -> TranslatedOutput(topicInfo, state.text)

            is TranslationState.Complete -> TranslatedOutput(topicInfo, state.text)
        }
    }
}

@Composable
internal fun TranslationPlatformUnavailable() {
    Text(stringResource(Res.string.translation_platform_unavailable))
}

@Composable
private fun TranslatedOutput(topicInfo: TopicDetail, text: String, retry: (() -> Unit)? = null) {
    val content = topicInfo.content.originalPlainContent() ?: return
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(bottom = if (retry != null) 44.dp else 0.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            AppTopicContentView(
                topicInfo.copy(topicInfo = topicInfo.topicInfo.copy(content = content.copy(plain = text))),
            )
        }
        if (retry != null) {
            Row(Modifier.align(Alignment.BottomCenter)) {
                TextButton(retry) { Text(stringResource(Res.string.translation_failed_retry)) }
            }
        }
    }
}
