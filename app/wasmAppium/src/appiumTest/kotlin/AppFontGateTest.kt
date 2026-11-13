/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import kotlin.test.Test

@RunWith(Parameterized::class)
class AppFontGateTest(private val browserType: WasmBrowser) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun browsers() = WasmBrowser.entries
    }

    @Test
    fun `font choice blocks business content`() =
        testWasmFonts(browserType, distribution(), panel = false, scenario = WasmFontCase.FIRST_CHOICE)

    @Test
    fun `font opt out survives refresh and restart`() =
        testWasmFonts(browserType, distribution(), panel = false, scenario = WasmFontCase.PERSISTENCE)

    @Test
    fun `stored font choice requires permission`() =
        testWasmFonts(browserType, distribution(), panel = false, scenario = WasmFontCase.PENDING_PERMISSION)

    @Test
    fun `denied font access keeps content blocked`() =
        testWasmFonts(browserType, distribution(), panel = false, scenario = WasmFontCase.DENIED_PERMISSION)

    @Test
    fun `granted font access loads system fonts`() =
        testWasmFonts(browserType, distribution(), panel = false, scenario = WasmFontCase.GRANTED_PERMISSION)

    @Test
    fun `revoked font access hides content`() =
        testWasmFonts(browserType, distribution(), panel = false, scenario = WasmFontCase.REVOKED_PERMISSION)

    private fun distribution(): File =
        File(System.getProperty("e2e.coverage.wasmDistribution", "build/wasmDistribution"))
}
