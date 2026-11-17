/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.storyteller_f.alphaheart.app.ProvideFontIcon
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.import_action
import com.storyteller_f.alphaheart.app.translate_model
import com.storyteller_f.alphaheart.app.translation_platform_unavailable
import com.storyteller_f.alphaheart.app.try_button
import org.jetbrains.compose.resources.getString
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class BrowserTranslationPreferenceTest {
    @Test
    fun browserPreferenceUsesItsRealUnsupportedModelBoundaryWithoutNodeFilesystem() =
        runComposeUiTest {
            val title = getString(Res.string.translate_model)
            val unavailable = getString(Res.string.translation_platform_unavailable)
            val importAction = getString(Res.string.import_action)
            val tryAction = getString(Res.string.try_button)
            setContent { MaterialTheme { ProvideFontIcon { TranslateModelPreferenceItem() } } }
            onNodeWithText(title).assertIsDisplayed()
            onNodeWithText(unavailable).assertIsDisplayed()
            onNodeWithText(importAction).assertDoesNotExist()
            onNodeWithText(tryAction).assertDoesNotExist()
        }
}
