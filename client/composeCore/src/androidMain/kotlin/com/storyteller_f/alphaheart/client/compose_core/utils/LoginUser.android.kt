/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.storyteller_f.shared.getAppContextRefValue
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream

private val injectedSessionJson = Json { ignoreUnknownKeys = true }

actual fun readInjectedSessionFromPrivateStorageOrNull(): ConvertedRawUserPassInfo? {
    val context = getAppContextRefValue() ?: return null
    val injectedFile =
        context.filesDir
            .resolve("appium-session")
            .resolve("session.json")
    if (!injectedFile.exists()) {
        Napier.d("Injected session file does not exist: ${injectedFile.absolutePath}")
        return null
    }

    return try {
        val content = BufferedInputStream(injectedFile.inputStream()).use { it.readBytes().decodeToString() }
        injectedSessionJson.decodeFromString<ConvertedRawUserPassInfo>(content)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (expected: Exception) {
        throw IllegalStateException(
            "Injected session file exists but cannot be loaded: ${injectedFile.absolutePath}",
            expected,
        )
    }
}
