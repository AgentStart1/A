/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.addChildAccount
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.signOutAll
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import io.ktor.http.HttpStatusCode
import kotlin.test.assertEquals
import kotlin.test.assertIs

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testSignUpByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { AppiumTestSetup(Unit) },
        ) { scope, _ ->
            val address = scenarioSignUp(scope.driver)
            scope.restart()
            scenarioVerifyInjectedSessionLoaded(scope.driver, address)
            scope.driver.dismissDialog()
            scenarioPublishTopicInUserSpace(scope.driver, address)
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testSignInAsSystemUserByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { AppiumTestSetup(Unit) },
        ) { scope, _ ->
            scenarioSignInAsSystemUser(scope.driver, targetHelper.readSystemPrivateKey())
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testInjectedSessionByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val session = targetHelper.createPreRegisteredSession(ports)
                AppiumTestSetup(
                    data = session.address,
                    injectedSession = session,
                )
            },
        ) { scope, address ->
            scenarioVerifyInjectedSessionLoaded(scope.driver, address)
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testPublishTopicInUserSpaceByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val session = targetHelper.createPreRegisteredSession(ports)
                AppiumTestSetup(data = session.address, injectedSession = session)
            },
        ) { scope, address ->
            scenarioPublishTopicInUserSpace(scope.driver, address)
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testFavoriteTopicByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val data = prepareFavoriteTopicScenario { targetHelper.createAuthenticatedSession(ports) }
                AppiumTestSetup(data = data, injectedSession = data.authenticated.session)
            },
        ) { scope, data ->
            try {
                scenarioFavoritePreparedTopic(scope.driver, data)
            } finally {
                data.authenticated.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testOpenAsciidocPreviewByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            captureBrowserOpen = platformHelper.capturesExternalAsciidocPreview,
            setup = { ports ->
                val data = prepareAsciidocPreviewScenario { targetHelper.createAuthenticatedSession(ports) }
                AppiumTestSetup(data = data, injectedSession = data.authenticated.session)
            },
        ) { scope, data ->
            try {
                scenarioOpenAsciidocPreview(
                    scope.driver,
                    data.authenticated.session.address,
                    data.topicMarker,
                )
                scope.assertAsciidocPreviewOpened(data.asciidocSource)
            } finally {
                data.authenticated.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testSubscribeTopicByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val data = prepareSubscriptionTopicScenario { targetHelper.createAuthenticatedSession(ports) }
                AppiumTestSetup(data = data, injectedSession = data.authenticated.session)
            },
        ) { scope, data ->
            try {
                scenarioSubscribePreparedTopic(scope.driver, data)
            } finally {
                data.authenticated.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testCommunityFavoriteByHelper() =
    testPreparedCommunityRoomScenarioByHelper { driver, data ->
        scenarioOpenCommunityProfile(driver, data.communityName)
        driver.clickByText("Favorite")
        waitUntilCommunityAction(data, favorite = true)
        driver.dismissDialog()
        driver.navigateBack()
        scenarioOpenCommunityProfile(driver, data.communityName)
        driver.clickByText("Favorite")
        waitUntilCommunityAction(data, favorite = true, active = false)
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testCommunitySubscriptionByHelper() =
    testPreparedCommunityRoomScenarioByHelper { driver, data ->
        scenarioOpenCommunityProfile(driver, data.communityName)
        driver.clickByText("Subscription")
        waitUntilCommunityAction(data, favorite = false)
        driver.dismissDialog()
        driver.navigateBack()
        scenarioOpenCommunityProfile(driver, data.communityName)
        driver.clickByText("Subscription")
        waitUntilCommunityAction(data, favorite = false, active = false)
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testCommunityMemberByHelper() =
    testPreparedCommunityRoomScenarioByHelper(::scenarioOpenCommunityMember)

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testPublishTopicInCommunityByHelper() =
    testPreparedCommunityRoomScenarioByHelper { driver, data ->
        scenarioPublishTopicInCommunity(driver, data.communityName)
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testPublishTopicInCommunityRoomByHelper() =
    testPreparedCommunityRoomScenarioByHelper { driver, data ->
        scenarioPublishTopicInRoom(driver, data.communityName, data.roomName)
    }

fun AppiumTestBase<PanelAppiumHelper, PlatformAppiumHelper>.testPanelInjectedSessionByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                val session = targetHelper.createPreRegisteredSession(ports)
                AppiumTestSetup(data = Unit, injectedSession = session)
            },
        ) { scope, _ ->
            scenarioOpenAllUsersFromOverview(scope.driver, platformHelper is DesktopAppiumHelper)
        }
    }

internal fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testPreparedCommunityRoomScenarioByHelper(
    block: suspend (AppTestDriver, PreparedCommunityRoomScenario) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runTest(
        testName = testName,
        target = targetHelper,
        setup = { ports ->
            val data = prepareCommunityRoomScenario { targetHelper.createAuthenticatedSession(ports) }
            AppiumTestSetup(data = data, injectedSession = data.viewer.session)
        },
    ) { scope, data ->
        try {
            block(scope.driver, data)
        } finally {
            data.viewer.sessionManager.client.close()
        }
    }
}

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testSigninLifecycleByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                AppiumTestSetup(prepareAuthenticatedRoomScenario { targetHelper.createAuthenticatedSession(ports) })
            },
        ) { scope, data ->
            try {
                scenarioSignIn(scope.driver, data.authenticated.session.pemPrivateKey)
                scope.restart()
                scenarioVerifyInjectedSessionLoaded(scope.driver, data.authenticated.session.address)
                if (platformHelper is WasmAppiumHelper) scope.driver.assertNotVisibleByText("Switch Account")
                scope.driver.dismissDialog()
                scenarioPublishTopicInRoom(scope.driver, data.communityName, data.roomName)
                data.authenticated.sessionManager.signOutAll().getOrThrow()
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    assertIs<ServerErrorException>(
                        data.authenticated.sessionManager.restoreSession().exceptionOrNull(),
                    ).status,
                )
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
                scope.restart()
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByText("Sign in")
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    assertIs<ServerErrorException>(
                        data.authenticated.sessionManager.restoreSession().exceptionOrNull(),
                    ).status,
                )
                scope.driver.dismissDialog()
                scenarioSignIn(scope.driver, data.authenticated.session.pemPrivateKey)
                scope.driver.assertVisibleByTextContaining(data.authenticated.session.address)
                scope.driver.dismissDialog()
                scenarioPublishTopicInRoom(scope.driver, data.communityName, data.roomName)
            } finally {
                data.authenticated.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<PanelAppiumHelper, PlatformAppiumHelper>.testPanelSigninLifecycleByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports -> AppiumTestSetup(targetHelper.createAuthenticatedSession(ports)) },
        ) { scope, data ->
            try {
                scenarioPanelSignin(scope.driver, data.session.pemPrivateKey)
                scope.driver.assertVisibleByDescription("Menu")
                scope.restart()
                scope.driver.assertVisibleByDescription("Menu")
                data.sessionManager.signOutAll().getOrThrow()
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    assertIs<ServerErrorException>(data.sessionManager.restoreSession().exceptionOrNull()).status,
                )
                scope.restart()
                scope.driver.assertVisibleByText("Sign in")
                scope.restart()
                scope.driver.assertVisibleByText("Sign in")
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    assertIs<ServerErrorException>(data.sessionManager.restoreSession().exceptionOrNull()).status,
                )
                scenarioPanelSignin(scope.driver, data.session.pemPrivateKey)
                scenarioOpenAllUsersFromOverview(scope.driver, platformHelper is DesktopAppiumHelper)
            } finally {
                data.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testChildSigninByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports -> AppiumTestSetup(targetHelper.createAuthenticatedSession(ports)) },
        ) { scope, data ->
            try {
                val child = data.sessionManager.addChildAccount().getOrThrow()
                scenarioSignIn(scope.driver, data.session.pemPrivateKey)
                scope.driver.clickByText("Switch Account")
                scope.driver.clickByTextContaining(child.userInfo.address)
                scope.driver.clickByDescription("avatar")
                scope.driver.assertVisibleByTextContaining(child.userInfo.address)
                scope.restart()
                scenarioVerifyInjectedSessionLoaded(scope.driver, data.session.address)
            } finally {
                data.sessionManager.client.close()
            }
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testSignoutPersistenceByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports -> AppiumTestSetup(targetHelper.createPreRegisteredSession(ports)) },
        ) { scope, data ->
            scenarioSignIn(scope.driver, data.pemPrivateKey)
            scope.driver.clickByText("Sign out")
            scope.driver.clickByText("Yes")
            // Restart only after asynchronous sign-out has reached its observable completion state.
            scope.driver.clickByDescription("avatar")
            scope.driver.assertVisibleByText("Sign in")
            scope.restart()
            scope.driver.clickByDescription("avatar")
            scope.driver.assertVisibleByText("Sign in")
        }
    }

fun AppiumTestBase<PanelAppiumHelper, PlatformAppiumHelper>.testPanelSignoutPersistenceByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports -> AppiumTestSetup(targetHelper.createPreRegisteredSession(ports)) },
        ) { scope, data ->
            scenarioPanelSignin(scope.driver, data.pemPrivateKey)
            scope.driver.assertVisibleByDescription("Menu")
            scope.restart()
            scope.driver.assertVisibleByDescription("Menu")
            if (platformHelper !is DesktopAppiumHelper) scope.driver.clickByDescription("Menu")
            scope.driver.clickByText("Sign out")
            scope.driver.clickByText("Yes")
            scope.driver.assertVisibleByText("Sign in")
            scope.restart()
            scope.driver.assertVisibleByText("Sign in")
        }
    }

internal suspend fun scenarioPanelSignin(driver: AppTestDriver, privateKey: String) {
    driver.clickByText("Sign in")
    driver.clickByText("Input")
    driver.clickByDescription("Edit Private Key")
    driver.inputText(privateKey)
    driver.clickByText("Confirm")
    driver.clickByText("Start")
}
