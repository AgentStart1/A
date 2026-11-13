/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.copy
import com.storyteller_f.alphaheart.app.error.ModelGenerationError
import com.storyteller_f.alphaheart.app.error.TopicActionError
import com.storyteller_f.alphaheart.app.pages.community.CommunityRefCell
import com.storyteller_f.alphaheart.app.pages.room.RoomRefCell
import com.storyteller_f.alphaheart.app.service.GPTOutput
import com.storyteller_f.alphaheart.app.service.buildGPT
import com.storyteller_f.alphaheart.app.service.buildTranslatePrompt
import com.storyteller_f.alphaheart.app.snapshot
import com.storyteller_f.alphaheart.app.success
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.alphaheart.app.utils.rememberStringPreference
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.ExceptionView
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.SheetContainer
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.components.copyText
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.getCurrentLanguage
import com.storyteller_f.alphaheart.client.core.createTopicSnapshot
import com.storyteller_f.alphaheart.client.core.pinTopic
import com.storyteller_f.alphaheart.client.core.unpinTopic
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.formatTime
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDialog(topicInfo: TopicDetail?, showDialog: Boolean, dismiss: () -> Unit) {
    if (topicInfo != null && showDialog) {
        BasicAlertDialog({
            dismiss()
        }) {
            TopicDialogInternal(topicInfo, dismiss)
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun TopicDialogInternal(topicInfo: TopicDetail, dismissDialog: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    DialogContainer(onDismissRequest = dismissDialog) {
        Text("pub: ${topicInfo.createdTime.formatTime()}")

        when (topicInfo.rootType) {
            ObjectType.COMMUNITY ->
                CommunityRefCell(topicInfo.rootId) {
                    dismissDialog()
                    appNavFactory.newAppNav().gotoCommunity(topicInfo.rootId, false)
                }

            ObjectType.ROOM ->
                RoomRefCell(topicInfo.rootId) {
                    dismissDialog()
                    appNavFactory.newAppNav().gotoRoom(topicInfo.rootId, false)
                }

            else -> {}
        }
        TopicDialogMenuList(topicInfo, dismissDialog)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDialogMenuList(topicInfo: TopicDetail, dismissDialog: () -> Unit) {
    val content = topicInfo.content
    Column {
        CopyButton(content)
        SnapshotButton(topicInfo)
        TopicPinButton(topicInfo, dismissDialog)
        TranslateButton(content, topicInfo)
        FavoriteButton(topicInfo.favoriteId, topicInfo.tuple())
        SubscriptionButton(topicInfo.subscriptionId, topicInfo.tuple())
    }
}

@Composable
private fun CopyButton(content: TopicContent) {
    val toast = LocalToaster.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    ButtonNav(
        Icons.Default.ContentCopy,
        stringResource(Res.string.copy),
    ) {
        scope.launch {
            if (content is TopicContent.Plain) {
                clipboardManager.copyText(content.plain, toast)
            } else {
                toast.showMessage("failed")
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
        Icons.Default.PictureAsPdf,
        stringResource(Res.string.snapshot),
    ) {
        scope.launch {
            globalDialogController.useResult {
                request { createTopicSnapshot(topicInfo.id) }
            }.onSuccess {
                toast.showMessage(getString(Res.string.success))
            }
        }
    }
}

@Composable
private fun TopicPinButton(topicInfo: TopicDetail, dismissDialog: () -> Unit) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    ButtonNav(
        if (topicInfo.isPin) MaterialSymbolsOutlined.KeepOff else MaterialSymbolsOutlined.Keep,
        if (topicInfo.isPin) "Unpin" else "Pin",
    ) {
        scope.launch {
            globalDialogController.pinOrUnpinTopic(topicInfo).onSuccess {
                dismissDialog()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TranslateButton(content: TopicContent, topicInfo: TopicDetail) {
    val toasterState = LocalToaster.current
    var showSheet by remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState()
    ButtonNav(
        MaterialSymbolsOutlined.Translate,
        "Translate",
    ) {
        if (content is TopicContent.Plain) {
            showSheet = true
        } else {
            toasterState.showMessage("can't translate")
        }
    }
    TopicTranslateSheet(showSheet, sheetState, topicInfo) {
        showSheet = false
    }
}

suspend fun AppGlobalDialogController.pinOrUnpinTopic(topicInfo: TopicDetail): CustomResult<Unit, TopicActionError> =
    useResult {
        if (topicInfo.isPin) {
            request { unpinTopic(topicInfo.id) }
        } else {
            request { pinTopic(topicInfo.id) }
        }
    }.onSuccess {
        context.emitEvent(
            com.storyteller_f.alphaheart.app.common.OnTopicPinChanged(it.id, it.parentId, it.rootType, it.isPin),
        )
    }.map { Unit }.mapError(TopicActionError::fromError)

@OptIn(ExperimentalMaterial3Api::class, InternalComposeUiApi::class)
@Composable
fun TopicTranslateSheet(showSheet: Boolean, sheetState: SheetState, topicInfo: TopicDetail, hideSheet: () -> Unit) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        SheetContainer {
            val currentModel by rememberStringPreference("gpt_model", "")
            val content = topicInfo.content
            Box(
                modifier =
                Modifier.height(
                    200.dp,
                ).padding(
                    horizontal = 20.dp,
                ).padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ).fillMaxWidth(),
            ) {
                if (content is TopicContent.Plain) {
                    // TODO 转成ViewModel
                    val result by produceState<CustomResult<Flow<GPTOutput>, ModelGenerationError>?>(
                        null,
                        content,
                        currentModel,
                    ) {
                        val prompt =
                            buildTranslatePrompt(
                                content.plain,
                                getCurrentLanguage(),
                            )
                        Napier.i(tag = "gpt") {
                            "prompt $prompt"
                        }
                        value = buildGPT().generate(currentModel, prompt)
                    }
                    TopicTranslateSheetInternal(result, topicInfo, content)
                } else {
                    Text("can't process", modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TopicTranslateSheetInternal(
    result: CustomResult<Flow<GPTOutput>, ModelGenerationError>?,
    topicInfo: TopicDetail,
    content: TopicContent.Plain,
) {
    when {
        result == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

        result.isSuccess -> {
            var output by remember {
                mutableStateOf("")
            }
            val scope = rememberCoroutineScope()
            val outputFlow = result.getOrThrow()
            DisposableEffect(outputFlow) {
                val job =
                    scope.launch {
                        outputFlow.onCompletion {
                            Napier.i(tag = "gpt") {
                                "complete"
                            }
                        }.collect {
                            output += it.text
                        }
                    }
                onDispose {
                    Napier.i(tag = "gpt") {
                        "dispose"
                    }
                    job.cancel()
                }
            }
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState).fillMaxSize()) {
                AppTopicContentView(
                    topicInfo.copy(topicInfo = topicInfo.topicInfo.copy(content = content.copy(plain = output))),
                )
            }
        }

        else ->
            result.exceptionOrNull()?.let {
                ExceptionView(it, modifier = Modifier.align(Alignment.Center))
            }
    }
}
