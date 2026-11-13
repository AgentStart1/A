/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.mikepenz.aboutlibraries.Libs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AboutLibrariesHostTest {
    @Test
    fun resourceFailureCanBeRetried() =
        runTest {
            var fail = true
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                AboutLibrariesHost(backgroundScope, dispatcher, dispatcher) {
                    check(!fail) { "Resource unavailable" }
                    libraryFixture()
                }
            runCurrent()
            assertEquals("Resource unavailable", assertIs<LibrariesState.Failed>(host.state.value).cause.message)
            fail = false
            host.refresh()
            runCurrent()
            assertEquals(
                "Synthetic library",
                assertIs<LibrariesState.Ready>(host.state.value).libraries.libraries.single().name,
            )
            host.close()
        }

    @Test
    fun emptyDefinitionsProduceAnErrorInsteadOfBlankList() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host = AboutLibrariesHost(backgroundScope, dispatcher, dispatcher) { Libs(emptyList(), emptySet()) }
            runCurrent()
            assertIs<LibrariesState.Failed>(host.state.value)
            host.close()
        }

    @Test
    fun closingCancelsLoadingWithoutPublishingFailure() =
        runTest {
            var cancelled = false
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                AboutLibrariesHost(backgroundScope, dispatcher, dispatcher) {
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled = true
                    }
                }
            runCurrent()
            host.close()
            runCurrent()
            assertTrue(cancelled)
            assertIs<LibrariesState.Loading>(host.state.value)
        }

    private fun libraryFixture() =
        Libs.Builder().withJson(
            """{"libraries":[{"uniqueId":"sample:library","name":"Synthetic library"}],"licenses":{}}""",
        ).build()
}
