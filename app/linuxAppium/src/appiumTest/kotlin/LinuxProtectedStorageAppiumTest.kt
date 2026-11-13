/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test

class LinuxProtectedStorageAppiumTest :
    AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>(
        AppAppiumHelper(),
        DesktopAppiumHelper(protectedStoreLibrary = "/missing-system-secret-store"),
    ) {
    @Test
    fun `test missing credential library prevents persistent login`() = testLinuxProtectedStorageFailure()

    @Test
    fun `test child switch stays process local without credential library`() = testLinuxChildStorageFailure()
}
