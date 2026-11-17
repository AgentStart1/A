/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.storyteller_f.alphaheart.app.ProvideFontIcon
import com.storyteller_f.alphaheart.app.utils.ProvideAppPreferences
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
internal class HomeStartupPreferenceUiTest {
    @Test
    fun delayedPersistedStartupIsAppliedOnceWithoutOverwritingMountedNavigation() {
        for (initial in listOf(HOME_START_DESTINATION_COMMUNITIES, HOME_START_DESTINATION_ROOMS)) {
            runComposeUiTest {
                val values = MutableSharedFlow<String>(replay = 1)
                val width = mutableStateOf(800.dp)
                var captured: PagerState? = null
                setContent {
                    MaterialTheme {
                        val state = rememberStartupHomePager(values)
                        if (state == null) {
                            Text("Loading preference")
                        } else {
                            captured = state
                            val scope = rememberCoroutineScope()
                            Column(Modifier.width(width.value)) {
                                Text("Selected ${state.currentPage}")
                                HorizontalPager(state, Modifier.height(300.dp)) { Text("Section $it") }
                                ReadingButton({ scope.launch { state.scrollToPage(0) } }) { Text("Read home") }
                            }
                        }
                    }
                }
                onNodeWithText("Loading preference").assertIsDisplayed()
                runOnIdle { values.tryEmit(initial) }
                onNodeWithText("Selected ${homePageFromPreference(initial)}").assertIsDisplayed()
                val mounted = runOnIdle { checkNotNull(captured) }
                onNodeWithText("Read home").performClick()
                onNodeWithText("Selected 0").assertIsDisplayed()
                runOnIdle { values.tryEmit(HOME_START_DESTINATION_ROOMS) }
                for (nextWidth in listOf(390.dp, 800.dp)) {
                    runOnIdle { width.value = nextWidth }
                    onNodeWithText("Selected 0").assertIsDisplayed()
                    runOnIdle { assertSame(mounted, captured) }
                }
            }
        }
    }

    @Test
    fun startupOptionsHaveMatchingSelectionSummaryAndPersistedValues() {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
        try {
            runComposeUiTest {
                val store = MemoryPreferences()
                val key = stringPreferencesKey(HOME_START_DESTINATION_PREFERENCE_KEY)
                setContent {
                    MaterialTheme {
                        ReadingInteractions {
                            ProvideFontIcon {
                                ProvideAppPreferences(store) { HomeStartDestinationPreferenceItem() }
                            }
                        }
                    }
                }
                var selected = "世界"
                for ((label, value) in listOf(
                    "社区" to HOME_START_DESTINATION_COMMUNITIES,
                    "房间" to HOME_START_DESTINATION_ROOMS,
                    "世界" to HOME_START_DESTINATION_WORLD,
                )) {
                    onNodeWithText(selected).performClick()
                    onNodeWithText(label).performClick()
                    waitUntil { store.data.value[key] == value }
                    onNodeWithText(label).assertIsDisplayed()
                    runOnIdle { assertEquals(value, store.data.value[key]) }
                    selected = label
                }
                onNodeWithText("世界").assertIsDisplayed()
                onNodeWithText("原来的首页").assertDoesNotExist()
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}

private class MemoryPreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}
