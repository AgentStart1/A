/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.type.PrimaryKey

data class EncryptedKey(val topicId: PrimaryKey, val uid: PrimaryKey, val encryptedAes: ByteArray) {
    companion object
}
