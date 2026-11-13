/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testInvalidKeyByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { AppiumTestSetup(Unit) }) { scope, _ ->
            scope.driver.enterClientPrivateKey("not-a-private-key")
            scope.driver.assertVisibleByText("Invalid private key. Check the PEM format.")
            scope.restart()
            scope.driver.assertGuestAccount()
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testUnknownKeyByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = {
            loadCryptoLibIfNeed()
            val algorithm = getAlgo(AlgoType.P256)
            val key = algorithm.generatePemKeyPair().getOrThrow().first
            val publicKey = algorithm.getDerPublicKeyFromPrivateKey(key).getOrThrow()
            AppiumTestSetup(key to algorithm.calcAddress(publicKey).getOrThrow())
        }) { scope, (key, address) ->
            scope.driver.enterClientPrivateKey(key)
            scope.driver.assertVisibleByTextContaining(address)
            scope.driver.clickByText("Start sign in")
            scope.driver.assertVisibleByText("400 Bad Request user not found")
            scope.driver.clickByText("Close")
            scope.driver.assertVisibleByText("Start sign in")
            scope.restart()
            scope.driver.assertGuestAccount()
        }
    }

internal suspend fun AppTestDriver.enterClientPrivateKey(key: String) {
    clickByDescription("avatar")
    clickByText("Sign in")
    clickByText("Private Key")
    clickByDescription("Edit Private Key")
    inputText(key)
    clickByText("Confirm")
}

internal suspend fun AppTestDriver.assertGuestAccount() {
    clickByDescription("avatar")
    assertVisibleByText("Sign in")
    assertNotVisibleByText("Sign out")
}
