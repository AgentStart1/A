/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.addChildAccount
import com.storyteller_f.result.getOrThrow
import kotlin.test.assertEquals

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidProtectedStorageFailure() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(targetHelper.createAuthenticatedSession(ports))
        }) { scope, user ->
            try {
                platformHelper.withProtectedWritesBlocked(targetHelper.androidApp.packageName) {
                    scope.driver.enterClientPrivateKey(user.session.pemPrivateKey)
                    scope.driver.clickByText("Start sign in")
                    scope.driver.assertVisibleByTextContaining("Protected storage write failed")
                    scope.driver.assertVisibleByText("Close")
                    scope.driver.dismissDialog()
                    scope.driver.assertVisibleByText("Start sign in")
                    scope.restart()
                    scope.driver.assertGuestAccount()
                }
            } finally {
                user.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, AndroidAppiumHelper>.testAndroidChildStorageFailure() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(targetHelper.createAuthenticatedSession(ports))
        }) { scope, user ->
            try {
                val child = user.sessionManager.addChildAccount().getOrThrow()
                scenarioSignIn(scope.driver, user.session.pemPrivateKey)
                val packageName = targetHelper.androidApp.packageName
                val original = platformHelper.protectedPreferencesDigest(packageName)
                platformHelper.withProtectedWritesBlocked(packageName) {
                    scope.driver.clickByText("Switch Account")
                    scope.driver.clickByTextContaining(child.userInfo.address)
                    scope.driver.clickByDescription("avatar")
                    scope.driver.assertVisibleByTextContaining(child.userInfo.address)
                }
                assertEquals(original, platformHelper.protectedPreferencesDigest(packageName))
                scope.restart()
                scenarioVerifyInjectedSessionLoaded(scope.driver, user.session.address)
            } finally {
                user.sessionManager.client.close()
            }
        }
    }
