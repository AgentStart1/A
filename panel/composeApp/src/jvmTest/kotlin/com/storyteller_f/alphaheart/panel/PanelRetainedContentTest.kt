/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class PanelRetainedContentTest {
    @Test
    fun switchingDrawerBranchesRetainsOpenModalDraftSelectionAndDestinationOwner() =
        runComposeUiTest {
            val compact = mutableStateOf(false)
            val revision = mutableStateOf("initial")
            val lifetime = ContentLifetime()
            setContent {
                MaterialTheme { DrawerBranchFixture(compact.value, revision.value, lifetime) }
            }
            onNodeWithText("Open editor").performClick()
            onNodeWithTag("draft").performTextInput("unsaved user")
            onNodeWithText("Select second").performClick()
            onNodeWithText("Selected: second").assertExists()
            onNodeWithText("Context: wide initial").assertExists()
            runOnIdle {
                compact.value = true
                revision.value = "updated"
            }
            onNodeWithTag("draft").assertTextContains("unsaved user")
            onNodeWithText("Selected: second").assertExists()
            onNodeWithText("Context: narrow updated").assertExists()
            runOnIdle { compact.value = false }
            onNodeWithTag("draft").assertTextContains("unsaved user")
            onNodeWithText("Selected: second").assertExists()
            onNodeWithText("Context: wide updated").assertExists()
            runOnIdle {
                assertEquals(1, lifetime.mounts)
                assertEquals(0, lifetime.unmounts)
            }
        }
}

@Composable
private fun DrawerBranchFixture(compact: Boolean, revision: String, lifetime: ContentLifetime) {
    val wideOwner = rememberNavigationEventDispatcherOwner(parent = null)
    val narrowOwner = rememberNavigationEventDispatcherOwner(parent = null)
    val retainedContent = rememberPanelMainContent { StatefulPanelEditor(revision, wideOwner, lifetime) }
    if (compact) {
        Column(Modifier.width(360.dp)) {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides narrowOwner) { retainedContent() }
        }
    } else {
        Box(Modifier.width(1000.dp)) {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides wideOwner) { retainedContent() }
        }
    }
}

@Composable
private fun StatefulPanelEditor(
    revision: String,
    wideOwner: NavigationEventDispatcherOwner,
    lifetime: ContentLifetime,
) {
    var open by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf("first") }
    val owner = LocalNavigationEventDispatcherOwner.current
    val context = if (owner === wideOwner) "wide" else "narrow"
    DisposableEffect(Unit) {
        lifetime.mounts++
        onDispose { lifetime.unmounts++ }
    }
    TextButton(onClick = { open = true }) { Text("Open editor") }
    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            DialogContainer {
                Text("Editor")
                TextField(draft, { draft = it }, Modifier.testTag("draft"), label = { Text("Draft") })
                TextButton(onClick = { selected = "second" }) { Text("Select second") }
                Text("Selected: $selected")
                Text("Context: $context $revision")
            }
        }
    }
}

private class ContentLifetime {
    var mounts = 0
    var unmounts = 0
}
