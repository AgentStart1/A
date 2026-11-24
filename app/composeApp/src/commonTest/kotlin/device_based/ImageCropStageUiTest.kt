/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.storyteller_f.alphaheart.app.media.runImageCropStages
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import com.storyteller_f.result.CustomResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class ImageCropStageUiTest {
    @Test
    fun cropInteractionCanCancelAfterDownloadWithoutACompetingLoadingModal() =
        runComposeUiTest {
            val downloaded = CompletableDeferred<Unit>()
            val decision = CompletableDeferred<String?>()
            val cropVisible = mutableStateOf(false)
            val finished = mutableStateOf(false)
            lateinit var controller: CustomGlobalDialogController<Unit>
            setContent {
                val scope = rememberCoroutineScope()
                controller = remember(scope) { CustomGlobalDialogController(scope, Unit) }
                MaterialTheme {
                    Column {
                        Text("Original avatar")
                        ReadingButton({
                            scope.launch {
                                controller.runImageCropStages<Unit, String, String, String, String>(
                                    load = {
                                        downloaded.await()
                                        CustomResult.success("image")
                                    },
                                    crop = {
                                        cropVisible.value = true
                                        try {
                                            decision.await()
                                        } finally {
                                            cropVisible.value = false
                                        }
                                    },
                                    save = { error("cancel must not save") },
                                    upload = { error("cancel must not upload") },
                                )
                                finished.value = true
                            }
                        }) { Text("Edit avatar") }
                    }
                    if (cropVisible.value) {
                        BasicAlertDialog({ decision.complete(null) }) {
                            DialogContainer(onDismissRequest = { decision.complete(null) }) {
                                Text("Crop selected image")
                                ReadingButton({ decision.complete(null) }) { Text("Cancel crop") }
                            }
                        }
                    }
                    GlobalDialog(controller)
                }
            }
            onNodeWithText("Edit avatar").performClick()
            waitUntil { controller.state.value.singleOrNull() is GlobalDialogState.Loading }
            runOnIdle { downloaded.complete(Unit) }
            waitUntil { cropVisible.value }
            onNodeWithText("Crop selected image").assertIsDisplayed()
            runOnIdle { assertTrue(controller.state.value.isEmpty()) }
            onNodeWithText("Cancel crop").performClick()
            waitUntil { finished.value }
            onNodeWithText("Crop selected image").assertDoesNotExist()
            onNodeWithText("Original avatar").assertIsDisplayed()
            runOnIdle { assertTrue(controller.state.value.isEmpty()) }
        }
}
