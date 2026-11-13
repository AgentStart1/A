/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.shared.CryptoJvm
import java.io.File
import java.nio.file.Files
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class SnapshotKeyStoreTest {
    @Test
    fun loadsBase64WithoutUsingConfiguredPath() =
        withKeyStore { file, password ->
            val encoded = Base64.getEncoder().encodeToString(file.readBytes())
            val unusedPath = File(file.parentFile, "unused/keystore.p12")
            val config =
                requireNotNull(
                    loadConfiguredKeyStore(
                        mapOf(
                            "SNAPSHOT_KEYSTORE_BASE64" to encoded,
                            "SNAPSHOT_KEYSTORE_PATH" to unusedPath.path,
                            "SNAPSHOT_KEYSTORE_PASS" to password,
                        ),
                    ),
                )
            assertContentEquals(file.readBytes(), Base64.getDecoder().decode(config.base64))
            assertEquals(password, config.pass)
            assertEquals(false, unusedPath.exists())
            assertEquals("CustomKeyStore(<redacted>)", config.toString())
        }

    @Test
    fun retainsExistingFileConfiguration() =
        withKeyStore { file, password ->
            val config =
                requireNotNull(
                    loadConfiguredKeyStore(
                        mapOf(
                            "SNAPSHOT_KEYSTORE_PATH" to file.path,
                            "SNAPSHOT_KEYSTORE_PASS" to password,
                        ),
                    ),
                )
            assertContentEquals(file.readBytes(), Base64.getDecoder().decode(config.base64))
        }

    @Test
    fun rejectsInvalidBase64WithoutFileFallback() =
        withKeyStore { file, password ->
            val original = file.readBytes()
            assertFails {
                loadConfiguredKeyStore(
                    mapOf(
                        "SNAPSHOT_KEYSTORE_BASE64" to "invalid!",
                        "SNAPSHOT_KEYSTORE_PATH" to file.path,
                        "SNAPSHOT_KEYSTORE_PASS" to password,
                    ),
                )
            }
            assertContentEquals(original, file.readBytes())
        }

    @Test
    fun rejectsIncorrectOrMissingPassword() =
        withKeyStore { file, password ->
            val encoded = Base64.getEncoder().encodeToString(file.readBytes())
            assertFails {
                loadConfiguredKeyStore(mapOf("SNAPSHOT_KEYSTORE_BASE64" to encoded))
            }
            assertFails {
                loadConfiguredKeyStore(
                    mapOf(
                        "SNAPSHOT_KEYSTORE_BASE64" to encoded,
                        "SNAPSHOT_KEYSTORE_PASS" to "$password-wrong",
                    ),
                )
            }
        }

    @Test
    fun disablesSigningWithoutConfiguredKeyStore() {
        assertNull(loadConfiguredKeyStore(emptyMap()))
    }

    private fun loadConfiguredKeyStore(env: Map<String, String>) = loadSnapshotKeyStore(MergedEnv(listOf(env)))

    private fun withKeyStore(block: (File, String) -> Unit) {
        val directory = Files.createTempDirectory(File("build").toPath(), "snapshot-keystore-").toFile()
        try {
            val file = File(directory, "fixture.p12")
            val password = "fixture-password"
            CryptoJvm.createKeystore(password.toCharArray(), file.path)
            block(file, password)
        } finally {
            directory.deleteRecursively()
        }
    }
}
