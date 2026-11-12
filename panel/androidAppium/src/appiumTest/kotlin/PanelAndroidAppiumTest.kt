/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.dev.appium

import kotlin.test.Test

class PanelAndroidAppiumTest :
    AppiumTestBase<PanelAppiumHelper, AndroidAppiumHelper>(
        PanelAppiumHelper(),
        AndroidAppiumHelper(),
    ) {
    @Test
    fun `test explicit sign out survives restart`() = testPanelSignoutPersistenceByHelper()

    @Test
    fun `test sign in survives restart and revocation requires explicit login`() = testPanelSigninLifecycleByHelper()

    @Test
    fun `test panel sign in by injected private session`() = testPanelInjectedSessionByHelper()

    @Test
    fun `test left predictive back animates page and cancels without popping`() =
        testAndroidPanelPredictiveCancel(false)

    @Test
    fun `test right predictive back animates page and cancels without popping`() =
        testAndroidPanelPredictiveCancel(true)

    @Test
    fun `test drawer back closes overlay before navigating`() = testAndroidPanelDrawerBack()

    @Test
    fun `test panel uploaded video preview renders and returns`() = testAndroidPanelVideoPreview()
}
