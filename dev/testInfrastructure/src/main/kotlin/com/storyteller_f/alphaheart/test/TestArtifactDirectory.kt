/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import java.io.File
import java.nio.file.Files

/** Creates a named case directory; the Gradle Test task clears raw artifacts before execution. */
fun createTestArtifactDirectory(root: File, vararg names: String): File {
    require(names.isNotEmpty()) { "A test report needs a case name" }
    val directory = names.fold(root) { parent, name -> parent.resolve(reportPathSegment(name)) }
    val reportPath = directory.canonicalFile.toPath()
    val rootPath = root.canonicalFile.toPath()
    check(reportPath != rootPath && reportPath.startsWith(rootPath)) { "Report must remain within its root" }
    return Files.createDirectories(directory.toPath()).toFile()
}

private fun reportPathSegment(name: String): String {
    if (name.isEmpty()) return "%00"
    val reserved = name.substringBefore('.').uppercase() in WINDOWS_RESERVED_NAMES
    return name.mapIndexed { index, char ->
        val invalidCharacter = char in "<>:\"/\\|?*%" || char.code < 32
        val trailingCharacter = index == name.lastIndex && char in ". "
        val reservedPrefix = reserved && index == 0
        if (invalidCharacter || trailingCharacter || reservedPrefix) {
            "%${char.code.toString(16).uppercase().padStart(2, '0')}"
        } else {
            char.toString()
        }
    }.joinToString("")
}

private val WINDOWS_RESERVED_NAMES =
    setOf("CON", "PRN", "AUX", "NUL") + (1..9).flatMap { listOf("COM$it", "LPT$it") }
