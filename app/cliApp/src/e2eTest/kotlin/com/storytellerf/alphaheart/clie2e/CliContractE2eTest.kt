/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.clie2e

import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getReactions
import com.storyteller_f.alphaheart.client.core.getRoomInfo
import com.storyteller_f.alphaheart.client.core.getTopicInfo
import com.storyteller_f.alphaheart.client.core.getTopicTopics
import com.storyteller_f.alphaheart.client.core.searchAllMembers
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class CliContractE2eTest : CliScenarioTest() {
    override val suiteName = "CliContractE2eTest"

    @Test
    fun `login multiline key selects the expected identity`() =
        withUser { terminal, user ->
            val members = user.session.searchAllMembers(null, 10, "").getOrThrow().data
            val index = members.indexOfFirst { it.id == user.id }
            assertTrue(index >= 0)
            terminal.sendLine("6")
            terminal.awaitText("=== Member List ===")
            terminal.sendLine(index.toString())
            terminal.awaitText("=== User Detail ===")
            terminal.awaitText("ID: ${user.id}")
            val model = user.session.model
            terminal.awaitText("Address: ${requireNotNull(model.userHandler.data.value).address}")
        }

    @Test
    fun `recommendations render the persisted topic`() =
        withTopic { terminal, _, topic ->
            terminal.sendLine("3")
            terminal.awaitText("Topic/Message List:")
            terminal.awaitText("${topic.id}: ${topic.text}")
        }

    @Test
    fun `joined communities render membership`() =
        withRoom { terminal, _, data ->
            terminal.sendLine("4")
            terminal.awaitText("Joined Communities:")
            terminal.awaitText("${data.community.id}: ${data.community.name}")
        }

    @Test
    fun `joined rooms render membership`() =
        withRoom { terminal, _, data ->
            terminal.sendLine("5")
            terminal.awaitText("Joined Rooms:")
            terminal.awaitText("${data.room.id}: ${data.room.name}")
        }

    @Test
    fun `favorites render the selected topic`() =
        withFixture({ _, user ->
            val topic = user.session.createCliTopic()
            user.session.addFavorite(NewFavorite(ObjectType.TOPIC, topic.id)).getOrThrow()
            topic
        }) { terminal, _, topic ->
            terminal.sendLine("7")
            terminal.awaitText("=== Favorites ===")
            terminal.awaitText("TOPIC ${topic.id}")
        }

    @Test
    fun `subscriptions render the selected topic`() =
        withFixture({ _, user ->
            val topic = user.session.createCliTopic()
            user.session.addSubscription(NewSubscription(topic.id, ObjectType.TOPIC)).getOrThrow()
            topic
        }) { terminal, _, topic ->
            terminal.sendLine("8")
            terminal.awaitText("=== Subscriptions ===")
            terminal.awaitText("TOPIC ${topic.id}")
        }

    @Test
    fun `topic favorite persists through CLI`() =
        withTopic { terminal, user, topic ->
            terminal.openTopic(user, topic)
            terminal.sendLine("1")
            terminal.awaitText("Favorited successfully")
            assertNotNull(user.session.getTopicInfo(topic.id).getOrThrow().favoriteId)
        }

    @Test
    fun `topic subscription persists through CLI`() =
        withTopic { terminal, user, topic ->
            terminal.openTopic(user, topic)
            terminal.sendLine("2")
            terminal.awaitText("Subscribed successfully")
            assertNotNull(user.session.getTopicInfo(topic.id).getOrThrow().subscriptionId)
        }

    @Test
    fun `topic comment persists with correct author`() =
        withTopic { terminal, user, topic ->
            terminal.openTopic(user, topic)
            terminal.sendLine("3")
            terminal.awaitText("=== Add Comment ===")
            val comment = "cli-comment-persisted"
            terminal.sendLine(comment)
            terminal.awaitText("Comment added successfully")
            val created =
                user.session.getTopicTopics(
                    topic.id,
                    TopicPinSearch.UNSPECIFIED,
                    PaginationQuery(),
                ).getOrThrow().data
            val reply = created.single { (it.content as? TopicContent.Plain)?.plain == comment }
            assertEquals(user.id, reply.author)
        }

    @Test
    fun `topic reaction code points persist through CLI`() =
        withTopic { terminal, user, topic ->
            terminal.openTopic(user, topic)
            terminal.sendLine("4")
            terminal.awaitText("=== Add Reaction ===")
            terminal.sendLine("U+2764 U+FE0F")
            terminal.awaitText("Reaction added successfully")
            val reactions = user.session.getReactions(topic.id, 10).getOrThrow().data
            val reaction = reactions.single { it.emoji == "❤️" }
            assertEquals(1L, reaction.count)
            assertTrue(reaction.hasReacted)
        }

    @Test
    fun `room message is persisted by WebSocket`() =
        withRoom { terminal, user, data ->
            terminal.awaitText("[ws] done")
            terminal.openRoom(data)
            terminal.sendLine("5")
            terminal.awaitText("=== Send Message ===")
            val text = "cli-room-message-persisted"
            terminal.sendLine(text)
            terminal.awaitText("Message Sent")
            awaitCliMessage(user, data.room.id, text)
        }

    @Test
    fun `room messages are read through CLI`() =
        withFixture({ ports, user ->
            val room = createCliRoomForTest(ports, user)
            seedCliMessage(user, room.room.id, "cli-room-history")
            room
        }) { terminal, _, room ->
            terminal.openRoom(room)
            terminal.sendLine("1")
            terminal.awaitText("Topic/Message List:")
            terminal.awaitText("cli-room-history")
        }

    @Test
    fun `community exit and rejoin persist membership`() =
        withRoom { terminal, user, data ->
            terminal.openCommunity(data)
            terminal.sendLine("5")
            terminal.awaitText("Exited Community successfully")
            assertEquals(false, user.session.getCommunityInfo(data.community.id).getOrThrow().isJoined)
            terminal.sendLine("4")
            terminal.awaitText("Joined Community successfully")
            assertTrue(user.session.getCommunityInfo(data.community.id).getOrThrow().isJoined)
        }

    @Test
    fun `room exit and rejoin persist membership`() =
        withRoom { terminal, user, data ->
            terminal.openRoom(data)
            terminal.sendLine("4")
            terminal.awaitText("Exited Room successfully")
            assertEquals(false, user.session.getRoomInfo(data.room.id).getOrThrow().isJoined)
            terminal.sendLine("3")
            terminal.awaitText("Joined Room successfully")
            assertTrue(user.session.getRoomInfo(data.room.id).getOrThrow().isJoined)
        }

    @Test
    fun `community favorite persists through CLI`() =
        withRoom { terminal, user, data ->
            terminal.openCommunity(data)
            terminal.sendLine("6")
            terminal.awaitText("Favorited successfully")
            assertNotNull(user.session.getCommunityInfo(data.community.id).getOrThrow().favoriteId)
        }

    @Test
    fun `community subscription persists through CLI`() =
        withRoom { terminal, user, data ->
            terminal.openCommunity(data)
            terminal.sendLine("7")
            terminal.awaitText("Subscribed successfully")
            assertNotNull(user.session.getCommunityInfo(data.community.id).getOrThrow().subscriptionId)
        }

    @Test
    fun `room favorite persists through CLI`() =
        withRoom { terminal, user, data ->
            terminal.openRoom(data)
            terminal.sendLine("6")
            terminal.awaitText("Favorited successfully")
            assertNotNull(user.session.getRoomInfo(data.room.id).getOrThrow().favoriteId)
        }

    @Test
    fun `room subscription persists through CLI`() =
        withRoom { terminal, user, data ->
            terminal.openRoom(data)
            terminal.sendLine("7")
            terminal.awaitText("Subscribed successfully")
            assertNotNull(user.session.getRoomInfo(data.room.id).getOrThrow().subscriptionId)
        }

    @Test
    fun `invalid private key leaves CLI unauthenticated`() =
        withGuest { _, terminal ->
            terminal.sendLine("2")
            terminal.awaitText("Enter Private Key:")
            terminal.sendLine("not-a-private-key")
            terminal.awaitText("Error")
            terminal.sendLine("4")
            terminal.awaitText("Failed")
            terminal.sendLine("0")
            terminal.awaitExit()
        }

    @Test
    fun `guest cannot read joined rooms`() =
        withGuest { _, terminal ->
            terminal.sendLine("5")
            terminal.awaitText("Failed")
            terminal.sendLine("0")
            terminal.awaitExit()
        }

    @Test
    fun `invalid menu choice preserves terminal usability`() =
        withGuest { _, terminal ->
            terminal.sendLine("unknown-command")
            terminal.awaitText("Invalid Choice")
            terminal.sendLine("0")
            terminal.awaitExit()
        }

    @Test
    fun `community rooms render persisted room`() =
        withRoom { terminal, _, data ->
            terminal.openCommunity(data)
            terminal.sendLine("2")
            terminal.awaitText("Joined Rooms:")
            terminal.awaitText("${data.room.id}: ${data.room.name}")
        }

    @Test
    fun `community members render authenticated member`() =
        withRoom { terminal, user, data ->
            terminal.openCommunity(data)
            terminal.sendLine("3")
            terminal.awaitText("User ID: ${user.id}")
        }

    @Test
    fun `room members render authenticated member`() =
        withRoom { terminal, user, data ->
            terminal.openRoom(data)
            terminal.sendLine("2")
            terminal.awaitText("User ID: ${user.id}")
        }

    @Test
    fun `community topics render persisted content`() =
        withFixture({ ports, user ->
            val room = createCliRoomForTest(ports, user)
            val text = "cli-community-topic"
            val topic = user.session.createTopic(ObjectType.COMMUNITY, room.community.id, text).getOrThrow()
            room to CliTopic(topic.id, text)
        }) { terminal, _, data ->
            terminal.openCommunity(data.first)
            terminal.sendLine("1")
            terminal.awaitText("Topic/Message List:")
            terminal.awaitText("${data.second.id}: ${data.second.text}")
        }
}

private suspend fun UserSessionManager.createCliTopic(): CliTopic {
    val text = "cli-list-topic"
    val topic = createTopic(ObjectType.USER, requireNotNull(model.uid), text).getOrThrow()
    return CliTopic(topic.id, text)
}
