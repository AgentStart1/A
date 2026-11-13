/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.type.PrimaryKey

data class ChildAccount(
    val uid: PrimaryKey,
    val encryptedPrivateKey: String,
    val encryptedAesKey: String,
    val hostId: PrimaryKey,
    val remark: String?,
    val encryptedEncryptionPrivateKey: String? = null,
) {
    companion object
}

data class RawChildAccount(val childAccount: ChildAccount, val rawUser: RawUser)
