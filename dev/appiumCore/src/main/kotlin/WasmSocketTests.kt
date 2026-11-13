/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.signOutAll
import com.storyteller_f.result.getOrThrow
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class BrowserSessionExpiry(val authenticated: AuthenticatedSession, val expire: () -> Unit)

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketServerExpiry() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(
                BrowserSessionExpiry(
                    targetHelper.createAuthenticatedSession(ports),
                    checkNotNull(ports.expireUserSessions),
                ),
            )
        }) { scope, data ->
            try {
                scenarioSignIn(scope.driver, data.authenticated.session.pemPrivateKey)
                scope.driver.dismissDialog()
                val probe = (scope.driver as WasmAppTestDriver).socketProbe()
                probe.awaitOpen()
                data.expire()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
                probe.assertNoFurtherAttempts()
                probe.assertRevokedCookieRejected()
                scope.restart()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
            } finally {
                data.authenticated.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketReconnect() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(prepareAuthenticatedRoomScenario { targetHelper.createAuthenticatedSession(ports) })
        }) { scope, data ->
            try {
                scenarioSignIn(scope.driver, data.authenticated.session.pemPrivateKey)
                scope.driver.dismissDialog()
                val probe = (scope.driver as WasmAppTestDriver).socketProbe()
                probe.awaitOpen()
                scenarioPublishTopicInRoom(scope.driver, data.communityName, data.roomName)
                assertTrue(probe.count("sent") > 0)
                assertTrue(probe.count("received") > 0)
                val opened = probe.count("opened")
                val sent = probe.count("sent")
                probe.disconnect()
                probe.awaitOpen(opened)
                assertEquals(sent, probe.count("sent"), "Reconnection must not replay the published topic")
                val content = "socket-after-reconnect-${System.currentTimeMillis()}"
                scope.driver.inputText(content)
                scope.driver.clickByDescription("Send")
                scope.driver.assertVisibleByText(content)
                scope.restart()
                val restarted = (scope.driver as WasmAppTestDriver).socketProbe()
                restarted.awaitOpen()
                scenarioVerifyInjectedSessionLoaded(scope.driver, data.authenticated.session.address)
                scope.driver.clickByText("Sign out")
                scope.driver.clickByText("Yes")
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
                restarted.assertNoFurtherAttempts()
            } finally {
                data.authenticated.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketCookieLoss() =
    testBrowserSocketInvalidCookie(expired = false)

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketCookieExpiry() =
    testBrowserSocketInvalidCookie(expired = true)

private fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketInvalidCookie(expired: Boolean) =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(targetHelper.createAuthenticatedSession(ports))
        }) { scope, data ->
            try {
                scenarioSignIn(scope.driver, data.session.pemPrivateKey)
                scope.driver.dismissDialog()
                val probe = (scope.driver as WasmAppTestDriver).socketProbe()
                probe.awaitOpen()
                if (expired) probe.expireCookies() else probe.dropCookies()
                probe.disconnect()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
                probe.assertNoFurtherAttempts()
                scope.restart()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
            } finally {
                data.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketOrigin() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(targetHelper.createAuthenticatedSession(ports))
        }) { scope, data ->
            try {
                scenarioSignIn(scope.driver, data.session.pemPrivateKey)
                scope.driver.dismissDialog()
                val probe = (scope.driver as WasmAppTestDriver).socketProbe()
                probe.awaitOpen()
                probe.assertForeignOriginRejected()
            } finally {
                data.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testBrowserSocketRevocation() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            AppiumTestSetup(targetHelper.createAuthenticatedSession(ports))
        }) { scope, data ->
            try {
                scenarioSignIn(scope.driver, data.session.pemPrivateKey)
                scope.driver.dismissDialog()
                val probe = (scope.driver as WasmAppTestDriver).socketProbe()
                probe.awaitOpen()
                data.sessionManager.signOutAll().getOrThrow()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
                probe.assertNoFurtherAttempts()
                probe.assertRevokedCookieRejected()
                scope.restart()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
            } finally {
                data.sessionManager.client.close()
            }
        }
    }
