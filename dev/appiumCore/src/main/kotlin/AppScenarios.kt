/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType

suspend fun scenarioSignUp(driver: AppTestDriver): String {
    val algo = getAlgo(AlgoType.P256)
    val privateKey = algo.generatePemKeyPair().getOrThrow().first
    val publicKey = algo.getDerPublicKeyFromPrivateKey(privateKey).getOrThrow()
    val address = algo.calcAddress(publicKey).getOrThrow()
    driver.clickByDescription("avatar")
    driver.clickByText("Sign in")
    driver.clickByText("Go to sign up")
    driver.assertVisibleByText("Sign up")
    driver.clickByText("Private Key")
    driver.clickByDescription("Edit Private Key")
    driver.inputText(privateKey)
    driver.clickByText("Confirm")
    driver.assertVisibleByText("Start sign up")
    driver.clickByText("Start sign up")
    scenarioVerifyInjectedSessionLoaded(driver, address)
    return address
}

suspend fun scenarioSignIn(driver: AppTestDriver, privateKey: String) {
    driver.clickByDescription("avatar")
    driver.clickByText("Sign in")
    driver.clickByText("Private Key")
    driver.clickByDescription("Edit Private Key")
    driver.inputText(privateKey)
    driver.clickByText("Confirm")
    driver.assertVisibleByText("Start sign in")
    driver.clickByText("Start sign in")
    driver.clickByDescription("avatar")
    driver.assertNotVisibleByText("Sign in")
}

suspend fun scenarioSignInAsSystemUser(driver: AppTestDriver, privateKey: String) {
    scenarioSignIn(driver, privateKey)
    driver.assertNotVisibleByText("Sign in")
}

suspend fun scenarioVerifyInjectedSessionLoaded(driver: AppTestDriver, address: String) {
    driver.clickByDescription("avatar")
    driver.assertVisibleByTextContaining(address)
    driver.assertNotVisibleByText("Sign in")
}

suspend fun scenarioPublishTopicInUserSpace(driver: AppTestDriver, address: String) {
    val topicContent = "appium-user-space-topic-${System.currentTimeMillis()}"
    driver.clickByDescription("avatar")
    driver.clickByTextContaining(address)
    driver.clickByDescription("create")
    driver.clickByText("Raw")
    driver.inputText(topicContent)
    driver.clickByDescription("submit")
    driver.assertVisibleByText(topicContent)
}

suspend fun scenarioFavoriteTopic(driver: AppTestDriver, address: String, topicContent: String) {
    driver.clickByDescription("avatar")
    driver.clickByTextContaining(address)
    driver.assertVisibleByText(topicContent)
    driver.clickByText(topicContent)
    driver.clickByDescription("topic")
    driver.clickByText("Favorite")
}

suspend fun scenarioOpenAsciidocPreview(driver: AppTestDriver, address: String, topicMarker: String) {
    driver.clickByDescription("avatar")
    driver.clickByTextContaining(address)
    driver.assertVisibleByText(topicMarker)
    driver.clickByText(topicMarker)
    driver.assertVisibleByTextContaining("AsciiDoc preview")
    driver.clickByDescription("open")
}

suspend fun scenarioFavoritePreparedTopic(driver: AppTestDriver, data: FavoriteTopicScenario) {
    scenarioFavoriteTopic(driver, data.authenticated.session.address, data.topicContent)
    waitUntilTopicFavorited(data.authenticated.sessionManager, data.topicId)
    driver.dismissDialog()
    driver.assertVisibleByDescription("topic")
}

suspend fun scenarioOpenCommunity(driver: AppTestDriver, communityName: String) {
    driver.clickByText("Communities")
    driver.clickByText(communityName)
}

suspend fun scenarioSubscribeTopic(driver: AppTestDriver, communityName: String, topicContent: String) {
    scenarioOpenCommunity(driver, communityName)
    driver.clickByText(topicContent)
    driver.clickByDescription("topic")
    driver.clickByText("Subscription")
}

suspend fun scenarioSubscribePreparedTopic(driver: AppTestDriver, data: SubscriptionTopicScenario) {
    scenarioSubscribeTopic(driver, data.communityName, data.topicContent)
    waitUntilTopicSubscribed(data.authenticated.sessionManager, data.topicId)
    driver.dismissDialog()
    driver.assertVisibleByDescription("topic")
}

suspend fun scenarioOpenCommunityProfile(driver: AppTestDriver, communityName: String) {
    scenarioOpenCommunity(driver, communityName)
    driver.clickByText(communityName.first().toString())
}

suspend fun scenarioOpenCommunityMember(driver: AppTestDriver, data: PreparedCommunityRoomScenario) {
    scenarioOpenCommunityProfile(driver, data.communityName)
    driver.clickByTextContaining("All members")
    driver.clickByTextContaining(data.ownerSession.address)
    driver.assertVisibleByText(data.ownerTopic)
}

suspend fun scenarioPublishTopicInCommunity(driver: AppTestDriver, communityName: String) {
    val topicContent = "appium-community-topic-${System.currentTimeMillis()}"
    scenarioOpenCommunity(driver, communityName)
    driver.clickByText(communityName.first().toString())
    driver.clickByText("Add")
    driver.saveSnapshot("community-after-add")
    driver.clickByText("Raw")
    driver.inputText(topicContent)
    driver.clickByDescription("submit")
    driver.assertVisibleByText(topicContent)
}

suspend fun scenarioPublishTopicInRoom(driver: AppTestDriver, communityName: String, roomName: String) {
    val topicContent = "appium-room-topic-${System.currentTimeMillis()}"
    scenarioOpenCommunity(driver, communityName)
    driver.clickByText("Rooms")
    driver.clickByTextContaining(roomName)
    // The room composer must replace the outgoing room-list editor before text input.
    driver.assertVisibleByDescription("Send")
    driver.inputText(topicContent)
    driver.clickByDescription("Send")
    driver.assertVisibleByText(topicContent)
}
