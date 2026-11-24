/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnCommunityUpdated
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.components.FontView
import com.storyteller_f.alphaheart.app.components.ReadingSettingsLayout
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.field_code_font
import com.storyteller_f.alphaheart.app.field_content_font
import com.storyteller_f.alphaheart.app.field_not_set
import com.storyteller_f.alphaheart.app.font_settings
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.user.ObjectSettingDialog
import com.storyteller_f.alphaheart.app.user.SettingOption
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.updateCommunityInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSettingsPage(communityId: PrimaryKey) {
    var currentOption by remember {
        mutableStateOf<SettingOption?>(null)
    }
    val communityViewModel = createCommunityViewModel(communityId)
    val communityInfo by communityViewModel.handler.data.collectAsState()
    val globalDialogController = LocalGlobalDialog.current
    val closeDialog = {
        currentOption = null
    }

    Scaffold(topBar = {
        ReadingPageToolbar(
            stringResource(Res.string.font_settings),
            subtitle = communityInfo?.name,
            leading = { CommunityIconWithSettingsMenu(communityInfo) },
        )
    }) { paddingValues ->
        StateView(communityViewModel.handler, Modifier.padding(paddingValues)) { info ->
            FontSettingsInternal(
                androidx.compose.foundation.layout.PaddingValues(0.dp),
                info,
                communityId,
            ) { opt -> currentOption = opt }
        }
    }

    val scope = rememberCoroutineScope()
    ObjectSettingDialog(
        closeDialog,
        currentOption,
        { media ->
            scope.launch {
                val option = currentOption ?: return@launch
                val fontSettings = communityInfo?.fontSettings?.settings ?: FontSettings()
                val newFontSettings =
                    when (option) {
                        is SettingOption.ContentFont -> fontSettings.copy(contentFontId = media.id)
                        is SettingOption.CodeFont -> fontSettings.copy(codeFontId = media.id)
                        else -> return@launch
                    }
                val body = UpdateCommunityBody(fontSettings = newFontSettings)
                globalDialogController.useResult {
                    request { updateCommunityInfo(communityId, body) }
                }.onSuccess { newInfo ->
                    globalDialogController.emitEvent(OnCommunityUpdated(newInfo))
                    closeDialog()
                }
            }
        },
        {},
    )
}

@Suppress("LongMethod")
@Composable
private fun FontSettingsInternal(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    communityInfo: com.storyteller_f.shared.model.CommunityInfo,
    communityId: PrimaryKey,
    showDialog: (SettingOption) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    val fontSettings = communityInfo.fontSettings?.settings ?: FontSettings()
    val contentFont = communityInfo.fontSettings?.contentFont
    val codeFont = communityInfo.fontSettings?.codeFont

    ReadingSettingsLayout(Modifier.padding(paddingValues)) {
        Column {
            // Content Font
            FontSettingRow(
                label = stringResource(Res.string.field_content_font),
                fontFile = contentFont,
                onReset = {
                    scope.launch {
                        val newSettings = fontSettings.copy(contentFontId = null)
                        globalDialogController.useResult {
                            request {
                                updateCommunityInfo(communityId, UpdateCommunityBody(fontSettings = newSettings))
                            }
                        }.onSuccess { newInfo ->
                            globalDialogController.emitEvent(OnCommunityUpdated(newInfo))
                        }
                    }
                },
                onSet = { showDialog(SettingOption.ContentFont(contentFont?.fullName)) },
            )

            // Code Font
            FontSettingRow(
                label = stringResource(Res.string.field_code_font),
                fontFile = codeFont,
                onReset = {
                    scope.launch {
                        val newSettings = fontSettings.copy(codeFontId = null)
                        globalDialogController.useResult {
                            request {
                                updateCommunityInfo(communityId, UpdateCommunityBody(fontSettings = newSettings))
                            }
                        }.onSuccess { newInfo ->
                            globalDialogController.emitEvent(OnCommunityUpdated(newInfo))
                        }
                    }
                },
                onSet = { showDialog(SettingOption.CodeFont(codeFont?.fullName)) },
            )
        }
    }
}

@Composable
private fun FontSettingRow(label: String, fontFile: FileInfo?, onReset: () -> Unit, onSet: () -> Unit) {
    SettingOptionResettableView(
        label,
        fontFile != null,
        { reset ->
            if (reset) {
                onReset()
            } else {
                onSet()
            }
        },
        {
            if (fontFile != null) {
                FontView(fontFile)
            } else {
                Text(stringResource(Res.string.field_not_set), style = MaterialTheme.typography.bodyMedium)
            }
        },
    )
}
