/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getTopicInfo
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

data class SubscriptionTopicScenario(
    val authenticated: AuthenticatedSession,
    val topicId: Long,
    val communityName: String,
    val topicContent: String,
)

data class FavoriteTopicScenario(val authenticated: AuthenticatedSession, val topicId: Long, val topicContent: String)

data class AsciidocPreviewScenario(
    val authenticated: AuthenticatedSession,
    val topicMarker: String,
    val asciidocSource: String,
)

data class PreparedCommunityRoomScenario(
    val ownerSession: InjectedSession,
    val viewer: AuthenticatedSession,
    val communityId: Long,
    val roomId: Long,
    val communityName: String,
    val roomName: String,
    val ownerTopic: String,
)

internal data class AuthenticatedRoomScenario(
    val authenticated: AuthenticatedSession,
    val communityName: String,
    val roomName: String,
)

internal suspend fun prepareAuthenticatedRoomScenario(
    createAuthenticatedSession: suspend () -> AuthenticatedSession,
): AuthenticatedRoomScenario {
    val authenticated = createAuthenticatedSession()
    var prepared = false
    return try {
        val suffix = System.currentTimeMillis().toString()
        val communityName = "sc-$suffix"
        val roomName = "sr-$suffix"
        val communityId = createCommunityByApi(authenticated.sessionManager, communityName, "sc$suffix")
        createRoomByApi(authenticated.sessionManager, roomName, "sr$suffix", communityId)
        AuthenticatedRoomScenario(authenticated, communityName, roomName).also { prepared = true }
    } finally {
        if (!prepared) {
            captureFailure(ArtifactCollectionError::CollectionFailed) { authenticated.sessionManager.client.close() }
                .onFailure {
                    org.slf4j.LoggerFactory.getLogger(
                        "appium-setup",
                    ).warn("Failed to close incomplete room scenario", it)
                }
        }
    }
}

suspend fun prepareFavoriteTopicScenario(
    createAuthenticatedSession: suspend () -> AuthenticatedSession,
): FavoriteTopicScenario {
    val topicContent = "appium-favorite-topic-${System.currentTimeMillis()}"
    val authenticated = createAuthenticatedSession()
    try {
        val topicId =
            createTopicByApi(
                authenticated.sessionManager,
                ObjectType.USER,
                authenticated.sessionManager.model.uid ?: error("not login"),
                topicContent,
            )
        return FavoriteTopicScenario(authenticated, topicId, topicContent)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        authenticated.sessionManager.client.close()
        throw throwable
    }
}

suspend fun prepareAsciidocPreviewScenario(
    createAuthenticatedSession: suspend () -> AuthenticatedSession,
): AsciidocPreviewScenario {
    val now = System.currentTimeMillis()
    val topicMarker = "appium-asciidoc-preview-$now"
    val asciidocSource =
        """
        = Appium AsciiDoc Preview

        $topicMarker
        """.trimIndent()
    val topicContent =
        listOf(
            topicMarker,
            "",
            "```asciidoc",
            asciidocSource,
            "```",
        ).joinToString("\n")
    val authenticated = createAuthenticatedSession()
    try {
        createTopicByApi(
            authenticated.sessionManager,
            ObjectType.USER,
            authenticated.sessionManager.model.uid ?: error("not login"),
            topicContent,
        )
        return AsciidocPreviewScenario(authenticated, topicMarker, asciidocSource)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        authenticated.sessionManager.client.close()
        throw throwable
    }
}

suspend fun prepareSubscriptionTopicScenario(
    createAuthenticatedSession: suspend () -> AuthenticatedSession,
): SubscriptionTopicScenario {
    val now = System.currentTimeMillis()
    val topicContent = "appium-subscription-topic-$now"
    val owner = createAuthenticatedSession()
    var viewer: AuthenticatedSession? = null
    try {
        val aidSuffix = (now % 1_000_000).toString().padStart(6, '0')
        val communityName = "community-$aidSuffix"
        val communityId =
            owner.sessionManager.createCommunity(
                NewCommunity(communityName, "sc$aidSuffix"),
            ).getOrThrow().id
        val topicId =
            owner.sessionManager.createTopic(
                ObjectType.COMMUNITY,
                communityId,
                topicContent,
            ).getOrThrow().id
        viewer = createAuthenticatedSession()
        viewer.sessionManager.joinCommunity(communityId).getOrThrow()
        return SubscriptionTopicScenario(viewer, topicId, communityName, topicContent)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        viewer?.sessionManager?.client?.close()
        throw throwable
    } finally {
        owner.sessionManager.client.close()
    }
}

suspend fun prepareCommunityRoomScenario(
    createAuthenticatedSession: suspend () -> AuthenticatedSession,
): PreparedCommunityRoomScenario {
    val now = System.currentTimeMillis()
    val owner = createAuthenticatedSession()
    var viewer: AuthenticatedSession? = null
    var retained = false
    try {
        val aidSuffix = (now % 1_000_000).toString().padStart(6, '0')
        val communityName = "community-$aidSuffix"
        val roomName = "room-$aidSuffix"
        val communityId = createCommunityByApi(owner.sessionManager, communityName, "c$aidSuffix")
        val roomId = createRoomByApi(owner.sessionManager, roomName, "r$aidSuffix", communityId)
        createTopicByApi(owner.sessionManager, ObjectType.COMMUNITY, communityId, "appium-owner-community-topic-$now")
        val ownerTopic = "appium-owner-profile-$now"
        createTopicByApi(
            owner.sessionManager,
            ObjectType.USER,
            requireNotNull(owner.sessionManager.model.uid),
            ownerTopic,
        )
        viewer = createAuthenticatedSession()
        viewer.sessionManager.joinCommunity(communityId).getOrThrow()
        viewer.sessionManager.joinRoom(roomId).getOrThrow()
        val prepared =
            PreparedCommunityRoomScenario(
                owner.session,
                viewer,
                communityId,
                roomId,
                communityName,
                roomName,
                ownerTopic,
            )
        retained = true
        return prepared
    } finally {
        owner.sessionManager.client.close()
        if (!retained) viewer?.sessionManager?.client?.close()
    }
}

suspend fun waitUntilTopicFavorited(
    sessionManager: UserSessionManager,
    topicId: Long,
    timeoutMillis: Long = java.time.Duration.ofSeconds(30).toMillis(),
) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (sessionManager.getTopicInfo(topicId).getOrThrow().favoriteId != null) return
        delay(500.milliseconds)
    }
    error("Topic not marked as favorite: $topicId")
}

suspend fun waitUntilTopicSubscribed(
    sessionManager: UserSessionManager,
    topicId: Long,
    timeoutMillis: Long = java.time.Duration.ofSeconds(30).toMillis(),
) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (sessionManager.getTopicInfo(topicId).getOrThrow().subscriptionId != null) return
        delay(500.milliseconds)
    }
    error("Topic not marked as subscribed: $topicId")
}

private suspend fun createCommunityByApi(manager: UserSessionManager, name: String, aid: String): Long =
    manager.createCommunity(NewCommunity(name, aid)).getOrThrow().id

private suspend fun createRoomByApi(manager: UserSessionManager, name: String, aid: String, communityId: Long): Long =
    manager.createRoom(NewRoom(name, aid, communityId = communityId)).getOrThrow().id

private suspend fun createTopicByApi(
    manager: UserSessionManager,
    parentType: ObjectType,
    parentId: Long,
    content: String,
): Long = manager.createTopic(parentType, parentId, content).getOrThrow().id

internal suspend fun waitUntilCommunityAction(
    data: PreparedCommunityRoomScenario,
    favorite: Boolean,
    active: Boolean = true,
) {
    val deadline = System.currentTimeMillis() + java.time.Duration.ofSeconds(30).toMillis()
    while (System.currentTimeMillis() < deadline) {
        val info = data.viewer.sessionManager.getCommunityInfo(data.communityId).getOrThrow()
        val selected = if (favorite) info.favoriteId != null else info.subscriptionId != null
        if (selected == active) return
        delay(500.milliseconds)
    }
    error("Community action was not persisted")
}
