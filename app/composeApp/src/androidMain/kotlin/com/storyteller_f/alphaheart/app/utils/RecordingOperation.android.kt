/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

internal actual suspend fun <T> runRecordingOperation(block: suspend () -> T): T = block()
