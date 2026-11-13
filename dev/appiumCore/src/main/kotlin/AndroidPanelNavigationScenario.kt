/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

fun AppiumTestBase<PanelAppiumHelper, AndroidAppiumHelper>.testAndroidPanelDrawerBack() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val session = targetHelper.createPreRegisteredSession(ports)
                AppiumTestSetup(data = Unit, injectedSession = session)
            },
        ) { scope, _ ->
            val driver = scope.driver
            scenarioOpenAllUsersFromOverview(driver, expandedNavigation = false)
            driver.clickByDescription("Menu")
            driver.assertVisibleByDescription("Close navigation menu")
            driver.navigateBack()
            driver.assertNotVisibleByDescription("Close navigation menu")
            driver.assertVisibleByText("All users")
            driver.saveSnapshot("drawer-back-preserves-users")
            driver.navigateBack()
            driver.assertNotVisibleByText("All users")
            driver.assertVisibleByDescription("Menu")
        }
    }

fun AppiumTestBase<PanelAppiumHelper, AndroidAppiumHelper>.testAndroidPanelPredictiveCancel(fromRight: Boolean) =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val session = targetHelper.createPreRegisteredSession(ports)
                AppiumTestSetup(data = Unit, injectedSession = session)
            },
        ) { scope, _ ->
            val driver = scope.driver as AndroidAppTestDriver
            driver.clickByDescription("Menu")
            driver.clickByText("All communities")
            driver.assertVisibleByText("All communities")
            driver.assertPredictiveBackCancellation(fromRight)
            driver.assertVisibleByText("All communities")
            driver.navigateBack()
            driver.assertNotVisibleByText("All communities")
            driver.assertVisibleByDescription("Menu")
        }
    }
