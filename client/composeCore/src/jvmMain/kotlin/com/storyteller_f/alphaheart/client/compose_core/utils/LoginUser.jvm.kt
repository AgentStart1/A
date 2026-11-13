/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import java.io.File

actual fun readInjectedSessionFromPrivateStorageOrNull(): ConvertedRawUserPassInfo? {
    val sessionFilePath =
        System.getProperty("appium.session.file") ?: run {
            Napier.d { "Injected desktop session path is not configured" }
            return null
        }
    val file = File(sessionFilePath)
    if (!file.exists()) {
        Napier.d { "Injected session file does not exist" }
        return null
    }
    return try {
        val injected = Json.decodeFromString<ConvertedRawUserPassInfo>(file.readText())
        Napier.d { "Injected desktop session loaded" }
        injected
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (expected: Exception) {
        throw IllegalStateException(
            "Injected session file exists but cannot be loaded: $sessionFilePath",
            expected,
        )
    }
}
