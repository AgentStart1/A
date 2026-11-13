/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.Test

@RunWith(Parameterized::class)
class PanelBrowserNavigationTest(private val browserType: WasmBrowser) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun browsers() = WasmBrowser.entries
    }

    @Test
    fun `browser and app navigation round trip`() =
        testBrowserNavigation(browserType, distribution(), panel = true, scenario = BrowserNavigationCase.ROUND_TRIP)

    @Test
    fun `browser navigation survives refresh`() =
        testBrowserNavigation(browserType, distribution(), panel = true, scenario = BrowserNavigationCase.RELOAD)

    @Test
    fun `modal dismissal preserves browser navigation`() =
        testBrowserNavigation(
            browserType,
            distribution(),
            panel = true,
            scenario = BrowserNavigationCase.MODAL_DISMISSAL,
        )

    private fun distribution(): File =
        File(System.getProperty("e2e.coverage.wasmDistribution", "build/wasmDistribution"))
}
