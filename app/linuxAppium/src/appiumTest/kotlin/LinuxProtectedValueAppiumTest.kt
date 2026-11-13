/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test

class LinuxProtectedValueAppiumTest :
    AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>(AppAppiumHelper(), DesktopAppiumHelper()) {
    @Test
    fun `test large unicode private key history survives restart`() = testLinuxLargeProtectedValue()
}
