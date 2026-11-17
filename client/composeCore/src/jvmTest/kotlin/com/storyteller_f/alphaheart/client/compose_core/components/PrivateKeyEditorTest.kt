/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.storyteller_f.shared.model.AlgoType
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class PrivateKeyEditorTest {
    private val originalLocale = Locale.getDefault()

    @BeforeTest
    fun setEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun cancellingAlgorithmAndKeyEditsLeavesTheOwnerUnchanged() =
        runComposeUiTest {
            val owner = PrivateKeyEditorOwner(AlgoType.P256)
            setContent { PrivateKeyEditorFixture(owner) }
            editPostQuantumDraft()
            runOnIdle {
                assertEquals(AlgoType.P256, owner.algo)
                assertTrue(owner.events.isEmpty())
            }
            onNodeWithText("Cancel").performClick()
            runOnIdle {
                assertTrue(owner.cancelled)
                assertEquals(AlgoType.P256, owner.algo)
                assertEquals("original-signing", owner.signingKey)
                assertEquals("original-encryption", owner.encryptionKey)
                assertTrue(owner.events.isEmpty())
            }
        }

    @Test
    fun confirmingCommitsTheDraftAlgorithmAndBothKeysTogether() =
        runComposeUiTest {
            val owner = PrivateKeyEditorOwner(AlgoType.P256)
            setContent { PrivateKeyEditorFixture(owner) }
            editPostQuantumDraft()
            runOnIdle { assertTrue(owner.events.isEmpty()) }
            onNodeWithText("Confirm").performClick()
            runOnIdle {
                assertEquals(AlgoType.DILITHIUM, owner.algo)
                assertEquals("draft-signing", owner.signingKey)
                assertEquals("draft-encryption", owner.encryptionKey)
                assertEquals(
                    listOf("algo:DILITHIUM", "signing:draft-signing", "encryption:draft-encryption"),
                    owner.events,
                )
            }
        }

    @Test
    fun confirmingP256ClearsThePostQuantumEncryptionDraft() =
        runComposeUiTest {
            val owner = PrivateKeyEditorOwner(AlgoType.DILITHIUM)
            setContent { PrivateKeyEditorFixture(owner) }
            onNodeWithText("P256").performClick()
            runOnIdle {
                assertEquals(AlgoType.DILITHIUM, owner.algo)
                assertTrue(owner.events.isEmpty())
            }
            onNodeWithText("Confirm").performClick()
            runOnIdle {
                assertEquals(AlgoType.P256, owner.algo)
                assertEquals("original-signing", owner.signingKey)
                assertEquals("", owner.encryptionKey)
                assertEquals(
                    listOf("algo:P256", "signing:original-signing", "encryption:"),
                    owner.events,
                )
            }
        }

    private fun ComposeUiTest.editPostQuantumDraft() {
        onNodeWithText("DILITHIUM").performClick()
        onNode(hasSetTextAction() and hasText("Private Key")).performTextReplacement("draft-signing")
        onNode(hasSetTextAction() and hasText("Encryption Private Key")).performTextReplacement("draft-encryption")
    }
}

private class PrivateKeyEditorOwner(var algo: AlgoType) {
    var signingKey = "original-signing"
    var encryptionKey = "original-encryption"
    var cancelled = false
    val visible = mutableStateOf(true)
    val events = mutableListOf<String>()

    fun cancel() {
        cancelled = true
        visible.value = false
    }
}

@Composable
private fun PrivateKeyEditorFixture(owner: PrivateKeyEditorOwner) {
    if (owner.visible.value) {
        MaterialTheme {
            ReadingInteractions {
                PrivateKeyEditor(
                    privateKey = owner.signingKey,
                    encryptionPrivateKey = owner.encryptionKey,
                    enableRandom = false,
                    algo = owner.algo,
                    onAlgoChange = {
                        owner.algo = it
                        owner.events += "algo:${it.name}"
                    },
                    onConfirmPrivateKey = {
                        owner.signingKey = it
                        owner.events += "signing:$it"
                    },
                    onConfirmEncryptionPrivateKey = {
                        owner.encryptionKey = it
                        owner.events += "encryption:$it"
                        owner.visible.value = false
                    },
                    onCancel = owner::cancel,
                )
            }
        }
    }
}
