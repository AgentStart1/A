/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getCommunityInfoByAid
import com.storyteller_f.alphaheart.client.core.getTopicTopics
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.MemberPolicy
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testCommunityCreation() =
    withSignedInUser { scope, user, _ ->
        val driver = scope.driver as WasmAppTestDriver
        val uid = requireNotNull(user.sessionManager.model.uid)
        val name = "UI Community"
        val aid = "uicommunity"
        driver.clickByDescription("avatar")
        driver.clickByDescription("create")
        driver.clickByText("create community")
        driver.inputTextByDescription("community name", name)
        driver.inputTextByDescription("community aid", aid)
        driver.clickByDescription("submit")
        // Creation must finish and leave the form before navigating away from its owning scope.
        driver.awaitDescriptionRemoved("community aid")
        driver.clickByText("Communities")
        driver.assertVisibleByText(name)
        val community = user.sessionManager.getCommunityInfoByAid(aid).getOrThrow()
        assertEquals(name, community.name)
        assertEquals(uid, community.owner)
        assertEquals(MemberPolicy.OPEN, community.memberPolicy)
        assertEquals(1L, community.memberCount)
        assertTrue(community.isJoined)
        scope.restart()
        val restarted = scope.driver as WasmAppTestDriver
        restarted.awaitAuthenticatedSession()
        restarted.clickByText("Communities")
        restarted.assertVisibleByText(name)
    }

internal fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.withSignedInUser(
    scenario: suspend (AppiumTestScope, AuthenticatedSession, AppiumPorts) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runClients(testName, targetHelper, setup = { ports ->
        val user = targetHelper.createAuthenticatedSession(ports)
        WasmClientsSetup(user to ports, mapOf("user" to null)) { user.sessionManager.client.close() }
    }) { clients, (user, ports) ->
        val scope = clients.getValue("user")
        scenarioSignIn(scope.driver, user.session.pemPrivateKey)
        scope.driver.dismissDialog()
        scenario(scope, user, ports)
    }
}

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testCommunityMembershipByHelper() =
    testPreparedCommunityRoomScenarioByHelper { driver, data ->
        val session = data.viewer.sessionManager
        scenarioOpenCommunityProfile(driver, data.communityName)
        driver.clickByText("Exit community")
        driver.assertVisibleByText("Join community")
        assertEquals(false, session.getCommunityInfo(data.communityId).getOrThrow().isJoined)
        driver.clickByText("Join community")
        driver.assertVisibleByText("Exit community")
        assertTrue(session.getCommunityInfo(data.communityId).getOrThrow().isJoined)
        driver.dismissDialog()
        driver.navigateBack()
        scenarioOpenCommunity(driver, data.communityName)
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testTopicCommentByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(testName, targetHelper, setup = { ports ->
            val data = prepareFavoriteTopicScenario { targetHelper.createAuthenticatedSession(ports) }
            AppiumTestSetup(data, data.authenticated.session)
        }) { scope, data ->
            val session = data.authenticated.sessionManager
            try {
                val driver = scope.driver
                val comment = "ui-comment-${data.topicId}"
                driver.clickByDescription("avatar")
                driver.clickByTextContaining(data.authenticated.session.address)
                driver.clickByText(data.topicContent)
                driver.assertVisibleByDescription("Send")
                driver.inputText(comment)
                driver.clickByDescription("Send")
                driver.assertVisibleByText(comment)
                withTimeout(30_000) {
                    while (true) {
                        val replies =
                            session.getTopicTopics(
                                data.topicId,
                                TopicPinSearch.UNSPECIFIED,
                                PaginationQuery(),
                            ).getOrThrow().data
                        val reply = replies.singleOrNull { (it.content as? TopicContent.Plain)?.plain == comment }
                        if (reply != null) {
                            assertEquals(session.model.uid, reply.author)
                            assertEquals(data.topicId, reply.parentId)
                            assertEquals(ObjectType.TOPIC, reply.parentType)
                            break
                        }
                        delay(100)
                    }
                }
                driver.dismissKeyboard()
                driver.navigateBack()
                driver.clickByText(data.topicContent)
                driver.assertVisibleByText(comment)
            } finally {
                session.client.close()
            }
        }
    }
