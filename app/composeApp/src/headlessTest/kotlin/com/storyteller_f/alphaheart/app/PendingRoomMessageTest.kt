/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.room.PendingRoomMessage
import com.storyteller_f.alphaheart.app.room.RoomMessagePhase
import com.storyteller_f.alphaheart.app.room.prepareRoomMessage
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.trimMarkdownUnusedContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingRoomMessageTest {
    private val pending =
        PendingRoomMessage("Draft", ObjectTuple(20, ObjectType.TOPIC), 7, phase = RoomMessagePhase.SUBMITTED)
    private val persisted =
        TopicInfo.EMPTY.copy(
            id = 30,
            author = 7,
            parentId = 20,
            parentType = ObjectType.TOPIC,
            content = TopicContent.Plain("Draft"),
        )

    @Test
    fun persistedEchoMatchesTheSubmittedDraft() {
        assertTrue(pending.matches(TopicCreated(persisted, null)))
    }

    @Test
    fun otherMessagesCannotCompleteTheDraft() {
        listOf(
            persisted.copy(author = 8),
            persisted.copy(parentId = 21),
            persisted.copy(parentType = ObjectType.ROOM),
            persisted.copy(content = TopicContent.Plain("Different draft")),
            persisted.copy(content = TopicContent.DecryptFailed("Unavailable")),
        ).forEach { assertFalse(pending.matches(TopicCreated(it, null))) }
    }

    @Test
    fun canonicalPublicContentCompletesItsOriginalDraft() {
        val draft =
            """
            |```csa
            |  Programming  
            |```
            |
            | 
            """.trimMargin()
        val canonical = trimMarkdownUnusedContent(draft)
        val receipt = pending.copy(content = draft, expectedContent = canonical)
        assertTrue(receipt.matches(TopicCreated(persisted.copy(content = TopicContent.Plain(canonical)), null)))
        assertTrue(receipt.content == draft)
    }

    @Test
    fun encryptedDraftRequiresItsExactDecryptedContent() {
        val draft = "  Private draft\n\n"
        val receipt = pending.copy(content = draft, expectedContent = draft)
        assertTrue(receipt.matches(TopicCreated(persisted.copy(content = TopicContent.Plain(draft)), null)))
        assertFalse(receipt.matches(TopicCreated(persisted.copy(content = TopicContent.Plain(draft.trim())), null)))
    }

    @Test
    fun preparingCannotAcknowledgeEvenAnExactPersistedEcho() {
        assertFalse(pending.copy(phase = RoomMessagePhase.PREPARING).matches(TopicCreated(persisted, null)))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun invalidatedPreparationCannotSubmitAfterReconnect() =
        runTest {
            val message = pending.copy(phase = RoomMessagePhase.PREPARING)
            var current: PendingRoomMessage? = message
            var connected = true
            val normalization = CompletableDeferred<String>()
            var sends = 0
            val operation =
                async {
                    prepareRoomMessage(message, { current }, { connected }) { normalization.await() }?.let { sends++ }
                }
            runCurrent()
            connected = false
            current = null
            connected = true
            normalization.complete("Draft")
            operation.await()
            assertEquals(0, sends)
            assertNull(current)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun replacedOwnerAndConnectionCannotResumeAnOldPreparation() =
        runTest {
            val message = pending.copy(phase = RoomMessagePhase.PREPARING)
            var current: PendingRoomMessage? = message
            val normalization = CompletableDeferred<String>()
            val operation =
                async {
                    prepareRoomMessage(message, { current }, { true }) { normalization.await() }
                }
            runCurrent()
            val replacement = PendingRoomMessage("Draft", message.parent, message.author)
            current = replacement
            normalization.complete("Draft")
            assertNull(operation.await())
            assertTrue(current === replacement)
            assertNull(prepareRoomMessage(replacement, { current }, { false }) { "Draft" })
        }

    @Test
    fun currentConnectedPreparationTransitionsToSubmittedAndRetainsDraft() =
        runTest {
            val message = pending.copy(content = " Original draft ", phase = RoomMessagePhase.PREPARING)
            val submitted = prepareRoomMessage(message, { message }, { true }) { "Draft" }
            assertEquals(RoomMessagePhase.SUBMITTED, submitted?.phase)
            assertEquals(" Original draft ", submitted?.content)
            assertTrue(message.owns(submitted))
            assertTrue(assertNotNull(submitted).matches(TopicCreated(persisted, null)))
        }
}
