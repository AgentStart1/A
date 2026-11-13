/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.sun.jna.Platform
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class WindowsProtectedStoreTest {
    init {
        require(Platform.isWindows()) { "Run windowsProtectedStoreTest on Windows" }
    }

    @Test
    fun protectedValueUsesRealWindowsEncryption() =
        runBlocking {
            withWindowsStore { directory ->
                val value = "synthetic protected session"
                writeProtectedValue("synthetic-session", value)
                assertEquals(value, readProtectedValue("synthetic-session"))
                val encrypted = Files.readAllBytes(singleProtectedFile(directory))
                val plain = value.encodeToByteArray().toList()
                assertFalse(encrypted.toList().windowed(plain.size).any { it == plain })
            }
        }

    @Test
    fun blockedWritePreservesEncryptedOriginal() =
        runBlocking {
            withWindowsStore { directory ->
                writeProtectedValue("synthetic-session", "original session")
                val target = singleProtectedFile(directory)
                val encrypted = Files.readAllBytes(target)
                val backup = directory.resolve("original.backup")
                AutoCloseable {
                    if (Files.exists(backup)) {
                        Files.deleteIfExists(target.resolve("block"))
                        Files.deleteIfExists(target)
                        Files.move(backup, target)
                    }
                }.use {
                    Files.move(target, backup)
                    Files.createDirectory(target)
                    Files.createFile(target.resolve("block"))
                    assertFailsWith<IOException> { writeProtectedValue("synthetic-session", "replacement session") }
                    assertContentEquals(encrypted, Files.readAllBytes(backup))
                }
                assertEquals("original session", readProtectedValue("synthetic-session"))
            }
        }
}

private fun singleProtectedFile(directory: Path): Path = Files.list(directory).use { files -> files.toList().single() }

private suspend fun withWindowsStore(block: suspend (Path) -> Unit) {
    val root = Path.of("build", "test", "windowsProtectedStoreTest").toAbsolutePath().normalize().createDirectories()
    val directory = Files.createTempDirectory(root, "protected-")
    val previous = System.getProperty("app.protected.storage.dir")
    AutoCloseable {
        if (previous == null) {
            System.clearProperty("app.protected.storage.dir")
        } else {
            System.setProperty("app.protected.storage.dir", previous)
        }
        check(directory.startsWith(root))
        check(directory.toFile().deleteRecursively()) { "Cannot clean protected store fixture" }
    }.use {
        System.setProperty("app.protected.storage.dir", directory.toString())
        block(directory)
    }
}
