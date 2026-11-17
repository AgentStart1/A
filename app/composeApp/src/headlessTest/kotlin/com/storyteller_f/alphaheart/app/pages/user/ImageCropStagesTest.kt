/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import com.storyteller_f.alphaheart.app.error.AvatarUpdateError
import com.storyteller_f.alphaheart.client.compose_core.components.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogState
import com.storyteller_f.result.CustomResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImageCropStagesTest {
    @Test
    fun loadingEndsBeforeInteractiveCropAndUploadOwnsItsIndependentStage() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val download = CompletableDeferred<Unit>()
            val interaction = CompletableDeferred<Unit>()
            val calls = mutableListOf<String>()
            val operation =
                async {
                    controller.runImageCropStages(
                        load = {
                            calls += "download"
                            assertIs<GlobalDialogState.Loading>(controller.state.value.single())
                            download.await()
                            CustomResult.success("original")
                        },
                        crop = {
                            assertEquals("original", it)
                            calls += "crop"
                            assertTrue(controller.state.value.isEmpty())
                            interaction.await()
                            "cropped"
                        },
                        save = {
                            assertEquals("cropped", it)
                            calls += "save"
                            assertIs<GlobalDialogState.Loading>(controller.state.value.single())
                            CustomResult.success("path")
                        },
                        upload = {
                            assertEquals("path", it)
                            assertTrue(controller.state.value.isEmpty())
                            controller.useResult<String?, AvatarUpdateError> {
                                calls += "upload"
                                assertIs<GlobalDialogState.Loading>(controller.state.value.single())
                                CustomResult.success("uploaded")
                            }.let { result ->
                                when (result) {
                                    is CustomResult.Success -> result

                                    is CustomResult.Failure ->
                                        CustomResult.Failure(AvatarUpdateError.fromError(result.error))
                                }
                            }
                        },
                    )
                }
            runCurrent()
            assertEquals(listOf("download"), calls)
            download.complete(Unit)
            runCurrent()
            assertEquals(listOf("download", "crop"), calls)
            assertTrue(controller.state.value.isEmpty())
            interaction.complete(Unit)
            runCurrent()
            assertTrue(operation.isCompleted)
            assertEquals("uploaded", assertIs<CustomResult.Success<String?>>(operation.await()).value)
            assertEquals(listOf("download", "crop", "save", "upload"), calls)
            assertTrue(controller.state.value.isEmpty())
        }

    @Test
    fun cropCancellationNeverSavesOrUploadsAndLeavesNoGlobalLoading() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val result =
                controller.runImageCropStages<Unit, String, String, String, String>(
                    load = { CustomResult.success("image") },
                    crop = { null },
                    save = { error("cancelled image must not be saved") },
                    upload = { error("cancelled image must not be uploaded") },
                )
            assertEquals(null, assertIs<CustomResult.Success<String?>>(result).value)
            assertTrue(controller.state.value.isEmpty())
            val interaction = CompletableDeferred<Unit>()
            val operation =
                async {
                    controller.runImageCropStages<Unit, String, String, String, String>(
                        load = { CustomResult.success("image") },
                        crop = {
                            interaction.await()
                            "cropped"
                        },
                        save = { error("cancelled coroutine must not save") },
                        upload = { error("cancelled coroutine must not upload") },
                    )
                }
            runCurrent()
            operation.cancel(CancellationException("close settings"))
            runCurrent()
            assertTrue(operation.isCancelled)
            assertTrue(controller.state.value.isEmpty())
        }

    @Test
    fun cropFailureShowsOriginalDiagnosticWithoutSavingOrUploading() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val cause = IllegalStateException("synthetic crop failure")
            val result =
                controller.runImageCropStages<Unit, String, String, String, String>(
                    load = { CustomResult.success("image") },
                    crop = { throw cause },
                    save = { error("failed crop must not save") },
                    upload = { error("failed crop must not upload") },
                )
            assertIs<CustomResult.Failure<AvatarUpdateError>>(result)
            assertSame(cause, assertIs<GlobalDialogState.Error>(controller.state.value.single()).throwable)
        }
}
