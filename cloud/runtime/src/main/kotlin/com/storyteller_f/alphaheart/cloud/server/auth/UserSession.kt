/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface UserSession {
    val data: String

    @Serializable
    @SerialName("pending")
    data class Pending(override val data: String, val remote: String, val label: String) : UserSession

    @Serializable
    @SerialName("two_factor_pending")
    data class TwoFactorPending(override val data: String, val remote: String, val id: PrimaryKey, val label: String) :
        UserSession

    @Serializable
    @SerialName("success")
    data class Success(override val data: String, val remote: String, val id: PrimaryKey, val label: String) :
        UserSession
}
