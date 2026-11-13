/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.LoadedFont
import com.storyteller_f.shared.commonJson
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.impl.use
import kotlin.io.encoding.Base64
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsException

private val LocalPlatformFont = staticCompositionLocalOf<FontFamily?> { null }

@Composable
actual fun rememberPlatformDefaultFontFamily(): FontFamily? = LocalPlatformFont.current

/** Does not compose business content until the user opts out or an authorized font is preloaded. */
@Composable
@OptIn(ExperimentalWasmJsInterop::class, ExperimentalTextApi::class)
fun WasmFontGate(content: @Composable () -> Unit) {
    var state by remember { mutableStateOf<FontGateState>(FontGateState.Blocked) }
    val resolver = LocalFontFamilyResolver.current
    DisposableEffect(Unit) {
        startFontGate { json ->
            state =
                when (val snapshot = commonJson.decodeFromString<FontGateSnapshot>(json.toString())) {
                    FontGateSnapshot.Blocked -> FontGateState.Blocked
                    FontGateSnapshot.Default -> FontGateState.Ready(null)
                    is FontGateSnapshot.Font -> FontGateState.Pending(snapshot)
                }
        }
        onDispose { stopFontGate() }
    }
    LaunchedEffect(state) {
        val pending = state as? FontGateState.Pending ?: return@LaunchedEffect
        try {
            val font = pending.snapshot
            val bytes = Base64.decode(font.base64)
            val family = preloadBrowserFont(font.identity, bytes, resolver)
            currentCoroutineContext().ensureActive()
            if (confirmFont(font.requestId)) state = FontGateState.Ready(family)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Napier.w(failure) { "Unable to preload the selected browser-local font." }
            rejectFont(pending.snapshot.requestId)
        } catch (failure: JsException) {
            Napier.w { "Browser failed to preload the selected local font: ${failure.message}" }
            rejectFont(pending.snapshot.requestId)
        }
    }
    val ready = state as? FontGateState.Ready
    if (ready != null) {
        CompositionLocalProvider(LocalPlatformFont provides ready.family, content = content)
    }
}

internal suspend fun preloadBrowserFont(
    identity: String,
    bytes: ByteArray,
    resolver: FontFamily.Resolver,
): FontFamily {
    // Compose can silently replace a failed blocking LoadedFont with its default typeface.
    // Validate with the same Skia decoder before treating resolver preloading as acceptance.
    Data.makeFromBytes(bytes).use { data ->
        requireNotNull(FontMgr.default.makeFromData(data)) { "Unable to decode browser-local font bytes." }.close()
    }
    val family = FontFamily(LoadedFont(identity, { bytes }, FontWeight.Normal, FontStyle.Normal))
    resolver.preload(family)
    return family
}

private sealed interface FontGateState {
    data object Blocked : FontGateState
    data class Pending(val snapshot: FontGateSnapshot.Font) : FontGateState
    data class Ready(val family: FontFamily?) : FontGateState
}

@Serializable
private sealed interface FontGateSnapshot {
    @Serializable
    @SerialName("blocked")
    data object Blocked : FontGateSnapshot

    @Serializable
    @SerialName("default")
    data object Default : FontGateSnapshot

    @Serializable
    @SerialName("font")
    data class Font(val requestId: Int, val identity: String, val base64: String) : FontGateSnapshot
}
