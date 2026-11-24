/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.components.ReadingSettingsLayout
import com.storyteller_f.alphaheart.app.components.StringListPreference
import com.storyteller_f.alphaheart.app.error.PreferenceError
import com.storyteller_f.alphaheart.app.field_not_set
import com.storyteller_f.alphaheart.app.home_start_destination_communities
import com.storyteller_f.alphaheart.app.home_start_destination_rooms
import com.storyteller_f.alphaheart.app.home_start_destination_world
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_COMMUNITIES
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_PREFERENCE_KEY
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_ROOMS
import com.storyteller_f.alphaheart.app.navigation.HOME_START_DESTINATION_WORLD
import com.storyteller_f.alphaheart.app.pages.topic.TopicTranslateSheet
import com.storyteller_f.alphaheart.app.pages.topic.TranslationPlatformUnavailable
import com.storyteller_f.alphaheart.app.preference
import com.storyteller_f.alphaheart.app.preference_import_config
import com.storyteller_f.alphaheart.app.preference_try_translation
import com.storyteller_f.alphaheart.app.service.GPT
import com.storyteller_f.alphaheart.app.service.NoOpGPT
import com.storyteller_f.alphaheart.app.service.buildGPT
import com.storyteller_f.alphaheart.app.startup_section_label
import com.storyteller_f.alphaheart.app.translate_model
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun PreferencePage() {
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.preference)) }) { padding ->
        ReadingSettingsLayout(Modifier.padding(padding), maxContentWidth = 440.dp) {
            HomeStartDestinationPreferenceItem()
            HorizontalDivider()
            TranslateModelPreferenceItem()
        }
    }
}

@Composable
internal fun HomeStartDestinationPreferenceItem() {
    val preferences = com.storyteller_f.alphaheart.app.utils.LocalAppPreferences.current
    val current by com.storyteller_f.alphaheart.app.utils.rememberStringPreference(
        HOME_START_DESTINATION_PREFERENCE_KEY,
        HOME_START_DESTINATION_WORLD,
    )
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.startup_section_label), style = MaterialTheme.typography.titleMedium)
        val destinations =
            listOf(
                HOME_START_DESTINATION_WORLD to stringResource(Res.string.home_start_destination_world),
                HOME_START_DESTINATION_COMMUNITIES to stringResource(Res.string.home_start_destination_communities),
                HOME_START_DESTINATION_ROOMS to stringResource(Res.string.home_start_destination_rooms),
            )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            destinations.forEach { (value, label) ->
                ReadingTab(
                    selected = current == value,
                    onClick = { scope.launch { preferences.setString(HOME_START_DESTINATION_PREFERENCE_KEY, value) } },
                    modifier = Modifier.weight(1f),
                ) {
                    Box(Modifier.heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TranslateModelPreferenceItem(gpt: GPT = remember { buildGPT() }) {
    if (gpt is NoOpGPT) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.translate_model), style = MaterialTheme.typography.titleMedium)
            TranslationPlatformUnavailable()
        }
        return
    }
    var showSheet by remember {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()
    val toast = LocalToaster.current
    val globalDialogController = LocalGlobalDialog.current
    val models by gpt.models(scope).collectAsState(emptyList())
    StringListPreference(
        key = "gpt_model",
        defaultValue = "",
        title = stringResource(Res.string.translate_model),
        items = mapOf("" to stringResource(Res.string.field_not_set)) + models.associate { it.key to it.value },
        trailingContent = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingTextButton({
                    globalDialogController.launch {
                        globalDialogController.useResult<Unit, PreferenceError> {
                            FileKit.openFilePicker()?.let { file ->
                                gpt.importModel(file).getOrThrow()
                                toast.showSuccess("${file.name} imported")
                                CustomResult.success(Unit)
                            } ?: CustomResult.success(Unit)
                        }
                    }
                }) {
                    PreferenceActionLabel(ActionGlyph.FOLDER_OPEN, stringResource(Res.string.preference_import_config))
                }
                ReadingTextButton({
                    showSheet = true
                }) {
                    PreferenceActionLabel(ActionGlyph.LANGUAGES, stringResource(Res.string.preference_try_translation))
                }
            }
        },
    )

    TopicTranslateSheet(
        showSheet,
        TopicDetail.EMPTY.copy(
            topicInfo =
            TopicDetail.EMPTY.topicInfo.copy(
                content =
                TopicContent.Plain(
                    """Jonas, a 12-year-old boy, lives in
            |a community isolated from all except a few similar towns,
            |where everyone has an assigned role.
            |With the annual Ceremony of Twelve upcoming,
            |he is nervous, for there he will be assigned his life's work.
            |He seeks reassurance from his father,
            |a Nurturer (who cares for the infants in the Community) and his mother,
            |a high-ranking official in the Department of Justice.
            |He is told by his parents that the Elders,
            |who assign the children their careers, are always right.
                    """.trimMargin(),
                ),
            ),
        ),
    ) {
        showSheet = false
    }
}

@Composable
private fun PreferenceActionLabel(glyph: ActionGlyph, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ReadingIcon(actionIcon(glyph), null)
        Text(label)
    }
}
