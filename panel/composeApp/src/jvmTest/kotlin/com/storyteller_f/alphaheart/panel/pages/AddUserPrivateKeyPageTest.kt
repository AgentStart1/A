/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.panel.common.AddUserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class AddUserPrivateKeyPageTest {
    @Test
    fun importsOneFileIntoTheChosenFieldAtEitherWidth() {
        val locale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        Dispatchers.setMain(Dispatchers.Swing)
        try {
            runComposeUiTest {
                val width = mutableStateOf(720.dp)
                val model = AddUserViewModel()
                var file: String? = null
                setContent {
                    MaterialTheme {
                        Box(Modifier.requiredWidth(width.value)) {
                            AddUserPrivateKeyPage(model, readKeyFile = { file }, goBack = {})
                        }
                    }
                }
                onNodeWithContentDescription("Import private key").assertIsDisplayed()
                onNodeWithContentDescription("Import encryption private key").assertDoesNotExist()
                onNodeWithText("ML-DSA").performClick()
                val signing = onNodeWithContentDescription("Import signing private key")
                val encryption = onNodeWithContentDescription("Import encryption private key")
                runOnIdle { file = "sample signing key" }
                signing.performClick()
                waitUntil { model.privateKey.value == "sample signing key" }
                assertEquals("", model.encryptionPrivateKey.value)
                runOnIdle {
                    width.value = 390.dp
                    file = "sample encryption key"
                }
                encryption.performClick()
                waitUntil { model.encryptionPrivateKey.value == "sample encryption key" }
                assertEquals("sample signing key", model.privateKey.value)
                runOnIdle { file = null }
                signing.performClick()
                runOnIdle { assertEquals("sample signing key", model.privateKey.value) }
                onNodeWithText("P256").performClick()
                encryption.assertDoesNotExist()
                assertEquals("", model.encryptionPrivateKey.value)
            }
        } finally {
            Dispatchers.resetMain()
            Locale.setDefault(locale)
        }
    }
}
