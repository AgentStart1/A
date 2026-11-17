/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnCommunityUpdated
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingOptionView
import com.storyteller_f.alphaheart.app.field_alias
import com.storyteller_f.alphaheart.app.field_icon
import com.storyteller_f.alphaheart.app.field_name
import com.storyteller_f.alphaheart.app.field_poster
import com.storyteller_f.alphaheart.app.font_categories
import com.storyteller_f.alphaheart.app.font_settings
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.user.ObjectSettingDialog
import com.storyteller_f.alphaheart.app.pages.user.SettingOption
import com.storyteller_f.alphaheart.app.settings_title
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityPoster
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.core.updateCommunityInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySettingPage(communityId: PrimaryKey) {
    var currentOption by remember {
        mutableStateOf<SettingOption?>(null)
    }
    val communityViewModel = createCommunityViewModel(communityId)
    val communityInfo by communityViewModel.handler.data.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val globalDialogController = LocalGlobalDialog.current
    val closeDialog = {
        currentOption = null
    }
    Scaffold(
        topBar = {
            ReadingPageToolbar(
                stringResource(Res.string.settings_title) + communityInfo?.name?.let { " · $it" }.orEmpty(),
            )
        },
    ) { paddingValues ->
        StateView(communityViewModel.handler, Modifier.padding(paddingValues)) { it1 ->
            CommunitySettingInternal(PaddingValues(0.dp), { opt ->
                currentOption = opt
            }, it1)
        }
        val scope = rememberCoroutineScope()
        ObjectSettingDialog(
            closeDialog,
            currentOption,
            sheetState,
            { media ->
                scope.launch {
                    globalDialogController.useResult {
                        val body =
                            if (currentOption is SettingOption.Poster) {
                                UpdateCommunityBody(poster = media.id)
                            } else {
                                UpdateCommunityBody(icon = media.id)
                            }
                        request { updateCommunityInfo(communityId, body) }
                    }.onSuccess { newInfo ->
                        globalDialogController.emitEvent(OnCommunityUpdated(newInfo))
                        closeDialog()
                    }
                }
            },
            { input ->
                scope.launch {
                    globalDialogController.updateCommunity(communityId, input, currentOption, closeDialog)
                }
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun CommunitySettingInternal(
    values: PaddingValues,
    showDialog: (SettingOption) -> Unit,
    communityInfo: CommunityInfo,
) {
    val toasterState = LocalToaster.current
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    val appNavFactory = LocalAppNavFactory.current
    AdaptiveFormLayout(Modifier.padding(values)) {
        SettingOptionResettableView(
            stringResource(Res.string.field_icon),
            communityInfo.icon != null,
            {
                if (it) {
                    scope.launch {
                        globalDialogController.resetCommunityMedia(communityInfo.id, UpdateCommunityBody(icon = 0))
                    }
                } else {
                    showDialog(SettingOption.Icon(communityInfo.icon?.fullName))
                }
            },
            {
                CommunityIconWithDialog(
                    communityInfo,
                    showDialog = false,
                    setClickEvent = false,
                ) {}
            },
        )
        val globalDialogController = LocalGlobalDialog.current
        SettingOptionResettableView(
            stringResource(Res.string.field_poster),
            communityInfo.hasPoster,
            {
                if (it) {
                    scope.launch {
                        globalDialogController.resetCommunityMedia(communityInfo.id, UpdateCommunityBody(poster = 0))
                    }
                } else {
                    showDialog(SettingOption.Poster(communityInfo.poster?.fullName))
                }
            },
            {
                Box(modifier = Modifier.width(100.dp).aspectRatio(3 / 4f)) {
                    CommunityPoster(communityInfo)
                }
            },
        )
        SettingOptionView(stringResource(Res.string.field_name), {
            showDialog(SettingOption.Name(communityInfo.name))
        }, content = {
            Text(communityInfo.name, textDecoration = TextDecoration.Underline)
        })
        val aid = communityInfo.aid
        SettingOptionView(stringResource(Res.string.field_alias), {
            toasterState.showMessage("forbid")
        }, content = {
            Text(aid)
        })
        SettingOptionView(stringResource(Res.string.font_settings), {
            appNavFactory.newAppNav().gotoFontSettingsPage(communityInfo.id)
        }, showDivider = false) {
            Text(stringResource(Res.string.font_categories), textDecoration = TextDecoration.Underline)
        }
    }
}

private suspend fun AppGlobalDialogController.resetCommunityMedia(communityId: PrimaryKey, body: UpdateCommunityBody) {
    useResult {
        request { updateCommunityInfo(communityId, body) }
    }.onSuccess { newInfo ->
        emitEvent(OnCommunityUpdated(newInfo))
    }
}

private suspend fun AppGlobalDialogController.updateCommunity(
    communityId: PrimaryKey,
    string: String,
    showInputDialog: SettingOption?,
    closeDialog: () -> Unit,
) {
    val body =
        when (showInputDialog) {
            is SettingOption.Name -> {
                UpdateCommunityBody(name = string)
            }

            else -> {
                null
            }
        } ?: return
    useResult {
        request { updateCommunityInfo(communityId, body) }
    }.onSuccess { newInfo ->
        emitEvent(OnCommunityUpdated(newInfo))
        closeDialog()
    }
}
