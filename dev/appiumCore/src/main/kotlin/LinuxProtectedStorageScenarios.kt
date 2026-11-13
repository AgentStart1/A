/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.addChildAccount
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testLinuxProtectedStorageFailure() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(targetHelper.createAuthenticatedSession(ports))
        }) { scope, user ->
            try {
                scope.driver.enterClientPrivateKey(user.session.pemPrivateKey)
                scope.driver.clickByText("Start sign in")
                scope.driver.assertVisibleByTextContaining("missing-system-secret-store")
                scope.driver.assertVisibleByText("Close")
                scope.driver.dismissDialog()
                scope.driver.assertVisibleByText("Start sign in")
                scope.restart()
                scope.driver.assertGuestAccount()
            } finally {
                user.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testLinuxChildStorageFailure() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            val user = targetHelper.createAuthenticatedSession(ports)
            AppiumTestSetup(user, user.session)
        }) { scope, user ->
            try {
                val child = user.sessionManager.addChildAccount().getOrThrow()
                scenarioVerifyInjectedSessionLoaded(scope.driver, user.session.address)
                scope.driver.clickByText("Switch Account")
                scope.driver.clickByTextContaining(child.userInfo.address)
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByTextContaining(child.userInfo.address)
                scope.restart()
                scope.driver.assertGuestAccount()
            } finally {
                user.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, DesktopAppiumHelper>.testLinuxLargeProtectedValue() =
    runAppiumBlockingTest {
        check(DesktopTestPlatform.current() == DesktopTestPlatform.Linux) { "Requires a Linux desktop" }
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            val user = targetHelper.createAuthenticatedSession(ports)
            // PEM permits preamble lines. Keep the real key at the end so truncated input cannot parse.
            val key = "Synthetic account padding: " + "密钥".repeat(5000) + "\n" + user.session.pemPrivateKey
            val algorithm = getAlgo(AlgoType.P256)
            val publicKey = algorithm.getDerPublicKeyFromPrivateKey(key).getOrThrow()
            assertEquals(user.session.address, algorithm.calcAddress(publicKey).getOrThrow())
            assertTrue(key.encodeToByteArray().size > 16_384)
            AppiumTestSetup(user to key)
        }) { scope, (user, key) ->
            try {
                scenarioSignIn(scope.driver, key)
                scope.driver.assertVisibleByTextContaining(user.session.address)
                scope.driver.dismissDialog()
                scope.restart()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByTextContaining(user.session.address)
                scope.driver.assertNotVisibleByText("Sign in")
            } finally {
                user.sessionManager.client.close()
            }
        }
    }
