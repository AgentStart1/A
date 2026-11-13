/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.MemberPolicy
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.now
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DatabaseTest {
    init {
        SnowflakeFactory.setMachine(0)
    }

    private suspend fun createTestUser(db: CombinedDatabase): User {
        val id = SnowflakeFactory.nextId()
        val notificationId = SnowflakeFactory.nextId()
        val shortId = (id % 100_000).toString()
        val user =
            User(
                aid = "u$shortId",
                encryptionPublicKey = null,
                publicKey = "pk-$id",
                address = "addr-$id",
                icon = null,
                nickname = "User$shortId",
                id = id,
                createdTime = now(),
                acgAmount = 0,
                passType = PassType.RAW,
                algoType = AlgoType.P256,
                notificationId = notificationId,
            )
        db.user.createUser(user).getOrThrow()
        return user
    }

    private val createAndGetUser: suspend (CombinedDatabase) -> Unit = { db ->
        val user = createTestUser(db)

        val rawUser = db.user.getRawUser(ObjectFetch.IdFetch(user.id)).getOrThrow()
        assertNotNull(rawUser)
        assertEquals(user.id, rawUser.user.id)
        assertEquals(user.nickname, rawUser.user.nickname)
        assertEquals(user.address, rawUser.user.address)
    }

    private val userNotExistsReturnsNull: suspend (CombinedDatabase) -> Unit = { db ->
        val rawUser = db.user.getRawUser(ObjectFetch.IdFetch(999_999)).getOrThrow()
        assertNull(rawUser)
    }

    private val createCommunityWithMember: suspend (CombinedDatabase) -> Unit = { db ->
        val user = createTestUser(db)
        val communityId = SnowflakeFactory.nextId()
        val memberId = SnowflakeFactory.nextId()
        val community =
            Community(
                id = communityId,
                createdTime = now(),
                aid = "c${communityId % 100_000}",
                name = "TestCommunity",
                owner = user.id,
                memberPolicy = MemberPolicy.OPEN,
            )
        db.community.createCommunity(community, memberId).getOrThrow()

        val rawCommunity = db.community.getRawCommunity(ObjectFetch.IdFetch(communityId)).getOrThrow()
        assertNotNull(rawCommunity)
        assertEquals("TestCommunity", rawCommunity.community.name)
    }

    private val createRoom: suspend (CombinedDatabase) -> Unit = { db ->
        val user = createTestUser(db)
        val roomId = SnowflakeFactory.nextId()
        val memberId = SnowflakeFactory.nextId()
        val room =
            Room(
                id = roomId,
                createdTime = now(),
                aid = "r${roomId % 100_000}",
                name = "TestRoom",
                creator = user.id,
            )
        val member =
            Member(
                id = memberId,
                uid = user.id,
                objectId = roomId,
                objectType = ObjectType.ROOM,
                createdTime = now(),
                status = MemberStatus.JOINED,
                joinedTime = now(),
            )
        db.room.createRoom(room, listOf(member)).getOrThrow()

        val rawRoom = db.room.getRawRoom(ObjectFetch.IdFetch(roomId)).getOrThrow()
        assertNotNull(rawRoom)
        assertEquals("TestRoom", rawRoom.room.name)
    }

    private val databaseInitAndClean: suspend (CombinedDatabase) -> Unit = { db ->
        val user = createTestUser(db)
        val rawUser = db.user.getRawUser(ObjectFetch.IdFetch(user.id)).getOrThrow()
        assertNotNull(rawUser)

        db.clean()
        db.init()

        assertNull(db.user.getRawUser(ObjectFetch.IdFetch(user.id)).getOrThrow())
    }

    @Test
    fun `test create and get user memory`() = testDatabaseMemory(createAndGetUser)

    @Test
    fun `test create and get user container`() = testDatabaseContainer(createAndGetUser)

    @Test
    fun `test user not exists returns null memory`() = testDatabaseMemory(userNotExistsReturnsNull)

    @Test
    fun `test user not exists returns null container`() = testDatabaseContainer(userNotExistsReturnsNull)

    @Test
    fun `test create community with member memory`() = testDatabaseMemory(createCommunityWithMember)

    @Test
    fun `test create community with member container`() = testDatabaseContainer(createCommunityWithMember)

    @Test
    fun `test create room memory`() = testDatabaseMemory(createRoom)

    @Test
    fun `test create room container`() = testDatabaseContainer(createRoom)

    @Test
    fun `test database init and clean memory`() = testDatabaseMemory(databaseInitAndClean)

    @Test
    fun `test database init and clean container`() = testDatabaseContainer(databaseInitAndClean)
}
