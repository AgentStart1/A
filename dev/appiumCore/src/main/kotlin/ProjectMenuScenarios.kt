/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testProjectLibraries() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { AppiumTestSetup(Unit) }) { scope, _ ->
            openProjectMenu(scope.driver)
            scope.driver.assertNotVisibleByText("Download latest app")
            scope.driver.clickByText("Open Source Libraries")
            scope.driver.assertVisibleByTextContaining(" libraries")
            scope.driver.saveSnapshot("open-source-library-list")
        }
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testProjectRepository() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { AppiumTestSetup(Unit) }) { scope, _ ->
            val driver = scope.driver as WasmAppTestDriver
            driver.observeExternalUris()
            openProjectMenu(driver)
            driver.assertNotVisibleByText("Design Spec")
            driver.assertNotVisibleByText("Download latest app")
            driver.clickExternalLink("Source repository")
            driver.assertExternalUri("https://github.com/storytellerF/A")
        }
    }

private suspend fun openProjectMenu(driver: AppTestDriver) {
    driver.clickByDescription("about project")
}
