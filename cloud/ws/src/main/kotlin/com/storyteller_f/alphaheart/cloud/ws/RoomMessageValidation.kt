/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws

import com.storyteller_f.shared.model.TopicContent

private const val MAX_ROOM_MESSAGE_LENGTH = 1000

/** A rejected message must never continue into persistence or notification delivery. */
internal suspend fun processValidatedRoomMessage(
    content: TopicContent,
    reject: suspend (String) -> Unit,
    process: suspend () -> Unit,
) {
    val error =
        when (content) {
            is TopicContent.Plain ->
                when {
                    content.plain.isBlank() -> "plain is empty"
                    content.plain.length > MAX_ROOM_MESSAGE_LENGTH -> "plain is too long"
                    else -> null
                }

            is TopicContent.Encrypted ->
                when {
                    content.encrypted.isBlank() -> "message is empty"
                    content.encrypted.length > MAX_ROOM_MESSAGE_LENGTH -> "message is too long"
                    else -> null
                }

            else -> "not support message type"
        }
    if (error != null) {
        reject(error)
        return
    }
    process()
}
