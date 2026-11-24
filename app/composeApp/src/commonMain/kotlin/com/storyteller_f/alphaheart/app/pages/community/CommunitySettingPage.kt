/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnCommunityUpdated
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.community.resetCommunityMedia
import com.storyteller_f.alphaheart.app.community.updateCommunity
import com.storyteller_f.alphaheart.app.components.CommunityPoster
import com.storyteller_f.alphaheart.app.components.ReadingSettingsLayout
import com.storyteller_f.alphaheart.app.components.ReadingSettingsSection
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingValueRow
import com.storyteller_f.alphaheart.app.field_name
import com.storyteller_f.alphaheart.app.font_settings
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.user.ObjectSettingDialog
import com.storyteller_f.alphaheart.app.settings_aid
import com.storyteller_f.alphaheart.app.settings_basic
import com.storyteller_f.alphaheart.app.settings_community
import com.storyteller_f.alphaheart.app.settings_community_icon
import com.storyteller_f.alphaheart.app.settings_community_images
import com.storyteller_f.alphaheart.app.settings_community_poster
import com.storyteller_f.alphaheart.app.settings_font_summary
import com.storyteller_f.alphaheart.app.user.SettingOption
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
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
    val globalDialogController = LocalGlobalDialog.current
    val closeDialog = {
        currentOption = null
    }
    Scaffold(
        topBar = {
            ReadingPageToolbar(
                stringResource(Res.string.settings_community),
                subtitle = communityInfo?.name,
                leading = { CommunityIconWithSettingsMenu(communityInfo) },
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
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    val appNavFactory = LocalAppNavFactory.current
    ReadingSettingsLayout(Modifier.padding(values)) {
        ReadingSettingsSection(stringResource(Res.string.settings_basic)) {
            SettingValueRow(stringResource(Res.string.field_name), communityInfo.name, onEdit = {
                showDialog(SettingOption.Name(communityInfo.name))
            })
            SettingValueRow(stringResource(Res.string.settings_aid), communityInfo.aid)
        }
        ReadingSettingsSection(stringResource(Res.string.settings_community_images)) {
            SettingOptionResettableView(
                stringResource(Res.string.settings_community_icon),
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
            SettingOptionResettableView(
                stringResource(Res.string.settings_community_poster),
                communityInfo.hasPoster,
                {
                    if (it) {
                        scope.launch {
                            globalDialogController.resetCommunityMedia(
                                communityInfo.id,
                                UpdateCommunityBody(poster = 0),
                            )
                        }
                    } else {
                        showDialog(SettingOption.Poster(communityInfo.poster?.fullName))
                    }
                },
                {
                    Box(modifier = Modifier.width(90.dp).aspectRatio(3 / 4f)) {
                        CommunityPoster(communityInfo, shape = RoundedCornerShape(5.dp))
                    }
                },
            )
        }
        CommunityFontSettingsEntry { appNavFactory.newAppNav().gotoFontSettingsPage(communityInfo.id) }
    }
}

@Composable
internal fun CommunityIconWithSettingsMenu(info: CommunityInfo?) {
    if (info == null) return
    val menu = rememberCommonDialogController()
    CommunityIconWithDialog(info, menu.shown.value, onClickIcon = menu::update)
}

@Composable
private fun CommunityFontSettingsEntry(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 55.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadingIcon(actionIcon(ActionGlyph.TYPE), null)
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.font_settings), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.settings_font_summary), style = MaterialTheme.typography.bodySmall)
        }
        ReadingIcon(actionIcon(ActionGlyph.CHEVRON_RIGHT), null)
    }
}
