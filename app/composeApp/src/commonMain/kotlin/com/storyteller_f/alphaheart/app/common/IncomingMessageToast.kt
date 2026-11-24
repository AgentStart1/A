/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

internal data class IncomingMessageToast(
    val nickname: String?,
    val avatarUrl: String?,
    val roomName: String?,
    val text: String,
) {
    override fun toString() = "IncomingMessageToast"
}
