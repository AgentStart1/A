/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test

class PanelDesktopAppiumTest :
    AppiumTestBase<PanelAppiumHelper, DesktopAppiumHelper>(
        PanelAppiumHelper(),
        DesktopAppiumHelper(),
    ) {
    @Test
    fun `test explicit sign out survives restart`() = testPanelSignoutPersistenceByHelper()

    @Test
    fun `test sign in survives restart and revocation requires explicit login`() = testPanelSigninLifecycleByHelper()

    @Test
    fun `test panel sign in by injected private session`() = testPanelInjectedSessionByHelper()
}
