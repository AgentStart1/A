/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.io.RawSink
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

fun Path.safeSink(): RawSink {
    parent?.let {
        if (!SystemFileSystem.exists(it)) {
            SystemFileSystem.createDirectories(it)
        }
    }
    return SystemFileSystem.sink(this)
}
