/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws

import com.storyteller_f.shared.model.TopicContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomMessageValidationTest {
    @Test
    fun overlongPlainMessageIsRejectedWithoutPersistence() =
        runTest {
            assertRejected(TopicContent.Plain("x".repeat(1001)), "plain is too long")
        }

    @Test
    fun overlongEncryptedMessageIsRejectedWithoutPersistence() =
        runTest {
            assertRejected(TopicContent.Encrypted("x".repeat(1001), emptyMap()), "message is too long")
        }

    @Test
    fun emptyPlainMessageIsRejectedWithoutPersistence() =
        runTest {
            assertRejected(TopicContent.Plain("  "), "plain is empty")
        }

    @Test
    fun emptyEncryptedMessageIsRejectedWithoutPersistence() =
        runTest {
            assertRejected(TopicContent.Encrypted("  ", emptyMap()), "message is empty")
        }

    @Test
    fun unsupportedContentIsRejectedWithoutPersistence() =
        runTest {
            assertRejected(TopicContent.DecryptFailed("Unavailable"), "not support message type")
        }

    @Test
    fun maximumPlainMessageLengthReachesPersistenceOnce() =
        runTest {
            assertAccepted(TopicContent.Plain("x".repeat(1000)))
        }

    @Test
    fun maximumEncryptedMessageLengthReachesPersistenceOnce() =
        runTest {
            assertAccepted(TopicContent.Encrypted("x".repeat(1000), emptyMap()))
        }

    private suspend fun assertRejected(content: TopicContent, expected: String) {
        val errors = mutableListOf<String>()
        var persistenceCalls = 0
        processValidatedRoomMessage(content, reject = { errors += it }) { persistenceCalls += 1 }
        assertEquals(listOf(expected), errors)
        assertEquals(0, persistenceCalls)
    }

    private suspend fun assertAccepted(content: TopicContent) {
        val errors = mutableListOf<String>()
        var persistenceCalls = 0
        processValidatedRoomMessage(content, reject = { errors += it }) { persistenceCalls += 1 }
        assertEquals(emptyList(), errors)
        assertEquals(1, persistenceCalls)
    }
}
