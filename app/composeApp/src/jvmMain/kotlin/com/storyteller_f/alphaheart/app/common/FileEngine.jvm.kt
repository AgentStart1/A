/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.Dispatchers
import kotlinx.io.Sink
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

internal actual fun createFileEngine(): FileEngine = DiskFileEngine(Dispatchers.IO, ::createExclusiveArchiveSink)

/** CREATE_NEW atomically refuses existing filesystem targets, including aliases and links. */
internal fun createExclusiveArchiveSink(path: Path): Sink =
    Files.newOutputStream(Paths.get(path.toString()), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        .buffered().asSink().buffered()
