/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.backend.core.types.ReactionRecord
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabase
import com.storyteller_f.alphaheart.backend.exposed.tables.Reactions
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.deleteReaction
import com.storyteller_f.alphaheart.client.core.getReactionRecords
import com.storyteller_f.alphaheart.client.core.getReactions
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.now
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ReactionPrimaryKeyTest {
    @Test
    fun `reaction records include each readable topic once per page`() =
        test {
            attachSession {
                val community = createCommunity(NewCommunity("Reaction previews", "reaction-previews")).getOrThrow()
                val topic = createTopic(ObjectType.COMMUNITY, community.id, "First paragraph.\n\nSecond.").getOrThrow()
                addReaction(topic.id, "👍").getOrThrow()
                addReaction(topic.id, "😀").getOrThrow()

                val response = getReactionRecords(PaginationQuery(size = 2)).getOrThrow()
                assertEquals(2, response.data.size)
                assertEquals(listOf(topic.id), response.topics.map { it.id })
                val reactions = response.topics.single().reactions
                assertEquals(setOf("👍", "😀"), reactions.map { it.emoji }.toSet())
            }
        }

    @Test
    fun `same emoji on two topics has independent counts and deletion`() =
        test {
            val owner =
                attachSession {
                    val community = createCommunity(NewCommunity("Reaction scope", "reaction-scope")).getOrThrow()
                    val first = createTopic(ObjectType.COMMUNITY, community.id, "First topic").getOrThrow()
                    val second = createTopic(ObjectType.COMMUNITY, community.id, "Second topic").getOrThrow()
                    val emoji = "👍"
                    assertEquals(1, addReaction(first.id, emoji).getOrThrow().count)
                    assertEquals(1, addReaction(second.id, emoji).getOrThrow().count)
                    assertEquals(1, addReaction(first.id, emoji).getOrThrow().count)
                    assertEquals(0, deleteReaction(emoji, first.id).getOrThrow().count)
                    assertEquals(1, getReactions(second.id, 10).getOrThrow().data.single().count)
                    assertEquals(emptyList(), getReactions(first.id, 10).getOrThrow().data)
                    assertEquals(1, addReaction(first.id, emoji).getOrThrow().count)
                    assertEquals(0, deleteReaction(emoji, second.id).getOrThrow().count)
                    assertEquals(1, getReactions(first.id, 10).getOrThrow().data.single().count)
                    Triple(community.id, first.id, second.id)
                }
            attachSession {
                val (communityId, firstId, secondId) = owner.custom
                joinCommunity(communityId).getOrThrow()
                assertEquals(2, addReaction(firstId, "👍").getOrThrow().count)
                assertEquals(1, addReaction(secondId, "👍").getOrThrow().count)
                assertEquals(1, deleteReaction("👍", firstId).getOrThrow().count)
                assertEquals(1, getReactions(secondId, 10).getOrThrow().data.single().count)
                assertEquals(2, addReaction(firstId, "👍").getOrThrow().count)
                assertEquals(0, deleteReaction("👍", secondId).getOrThrow().count)
                assertEquals(2, getReactions(firstId, 10).getOrThrow().data.single().count)
            }
        }

    @Test
    fun `legacy emoji primary key migrates without losing records and migration is idempotent`() =
        test {
            val database = workerBackend.database as ExposedDatabase
            val reactions = database.reaction
            val emoji = "👍"
            val expectedFirst = prepareDamagedLegacyStats(database)
            val beforeRecords = snapshotReactionRecords(database)
            val connection = database.databaseSession.database
            restoreLegacyEmojiKey(connection)
            assertEquals(listOf("emoji"), reactionKeyColumns(connection))
            assertEquals(1, assertNotNull(reactions.getReactionInfo(11, 101, emoji).getOrThrow()).count)
            assertNull(reactions.getReactionInfo(11, 202, emoji).getOrThrow())
            database.migration()
            assertEquals(listOf("object_id", "emoji"), reactionKeyColumns(connection))
            assertEquals(expectedFirst, reactions.getReactionInfo(11, 101, emoji).getOrThrow())
            assertEquals(beforeRecords, snapshotReactionRecords(database))
            assertEquals(1, assertNotNull(reactions.getReactionInfo(11, 202, emoji).getOrThrow()).count)
            assertNull(reactions.getReactionInfo(11, 303, "🔥").getOrThrow())
            // An already-correct key must not trigger another full stats rebuild on each startup.
            reactions.insertReaction(ReactionRecord(99, 404, ObjectType.TOPIC, emoji, 1004, now())).getOrThrow()
            database.migration()
            assertNull(reactions.getReactionInfo(99, 404, emoji).getOrThrow())
            assertNotNull(reactions.getReactionRecordInfo(99, emoji, 404).getOrThrow())
            assertEquals(listOf("object_id", "emoji"), reactionKeyColumns(connection))
            assertEquals(beforeRecords, snapshotReactionRecords(database))
            assertEquals(expectedFirst, reactions.getReactionInfo(11, 101, emoji).getOrThrow())
            reactions.deleteReaction(11, emoji, 202).getOrThrow()
            reactions.statsReactionRecord(202, emoji, ObjectType.TOPIC).getOrThrow()
            assertNull(reactions.getReactionInfo(11, 202, emoji).getOrThrow())
            assertEquals(expectedFirst, reactions.getReactionInfo(11, 101, emoji).getOrThrow())
            reactions.deleteReaction(11, emoji, 101).getOrThrow()
            reactions.statsReactionRecord(101, emoji, ObjectType.TOPIC).getOrThrow()
            assertEquals(1, assertNotNull(reactions.getReactionInfo(12, 101, emoji).getOrThrow()).count)
        }
}

private suspend fun prepareDamagedLegacyStats(database: ExposedDatabase): ReactionInfo {
    val reactions = database.reaction
    val firstRecord = ReactionRecord(11, 101, ObjectType.TOPIC, "👍", 1001, now())
    reactions.insertReaction(firstRecord).getOrThrow()
    reactions.insertReaction(firstRecord.copy(uid = 12, id = 1002)).getOrThrow()
    reactions.statsReactionRecord(101, "👍", ObjectType.TOPIC).getOrThrow()
    val correct = assertNotNull(reactions.getReactionInfo(11, 101, "👍").getOrThrow())
    assertEquals(2, correct.count)
    // The old global emoji key rejected this object's stats after the record had already been committed.
    reactions.insertReaction(firstRecord.copy(objectId = 202, id = 1003)).getOrThrow()
    suspendTransaction(database.databaseSession.database) {
        Reactions.update({ Reactions.objectId eq 101 }) { it[Reactions.count] = 1 }
        Reactions.insert {
            it[Reactions.objectId] = 303
            it[Reactions.objectType] = ObjectType.TOPIC
            it[Reactions.emoji] = "🔥"
            it[Reactions.count] = 7
            it[Reactions.lastReactionId] = 9999
        }
    }
    return correct
}

private suspend fun snapshotReactionRecords(database: ExposedDatabase) =
    listOf(11L to 101L, 12L to 101L, 11L to 202L).map { (uid, objectId) ->
        assertNotNull(database.reaction.getReactionRecordInfo(uid, "👍", objectId).getOrThrow())
    }

private suspend fun restoreLegacyEmojiKey(database: R2dbcDatabase) {
    suspendTransaction(database) {
        val key = requireNotNull(db.dialectMetadata.existingPrimaryKeys(Reactions)[Reactions])
        val name = db.identifierManager.quoteIdentifierWhenWrongCaseOrNecessary(key.name)
        exec(
            "ALTER TABLE ${identity(Reactions)} DROP CONSTRAINT $name, " +
                "ADD CONSTRAINT \"legacy-reactions-emoji-key\" PRIMARY KEY (${identity(Reactions.emoji)})",
        )
        db.dialectMetadata.resetCaches()
    }
}

private suspend fun reactionKeyColumns(database: R2dbcDatabase): List<String> =
    suspendTransaction(database) {
        // Exposed's PostgreSQL metadata orders by column name, not the primary key's declared position.
        val query =
            """
            SELECT attribute.attname AS column_name
            FROM pg_catalog.pg_constraint AS primary_key
            CROSS JOIN LATERAL unnest(primary_key.conkey) WITH ORDINALITY AS key_column(attnum, position)
            JOIN pg_catalog.pg_attribute AS attribute
              ON attribute.attrelid = primary_key.conrelid AND attribute.attnum = key_column.attnum
            WHERE primary_key.contype = 'p' AND primary_key.conrelid = 'reactions'::regclass
            ORDER BY key_column.position
            """.trimIndent()
        val result = exec(query) { row -> requireNotNull(row.get("column_name", String::class.java)) }
        val orderedColumns = requireNotNull(result).map { requireNotNull(it) }.toList()
        val metadata = requireNotNull(db.dialectMetadata.existingPrimaryKeys(Reactions)[Reactions])
        assertEquals(metadata.columnNames.map { it.lowercase() }.toSet(), orderedColumns.toSet())
        orderedColumns
    }
