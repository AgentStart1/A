/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

expect suspend fun saveTextToFile(
    suggestedName: String,
    extension: String,
    extensions: Set<String>,
    content: String,
): Boolean

internal suspend fun saveRecoveryCodes(codes: List<String>): Boolean =
    saveTextToFile("recovery-codes", "txt", setOf("txt"), codes.joinToString("\n"))
