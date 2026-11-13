/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

data class CustomKeyStore(val base64: String, val pass: String) {
    override fun toString(): String = "CustomKeyStore(<redacted>)"
}

data class CustomConfig(
    val buildType: String,
    val flavor: String,
    val snapshotKeyStore: CustomKeyStore?,
    val enableSignUp: Boolean = true,
)
