/*
 * This is a private project. All rights reserved.
 */

package fonts

import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.skiko.wasm.onWasmReady
import kotlin.coroutines.resume

class CommunityFontValidationBrowserTest : CommunityFontValidationContract() {
    override suspend fun prepareFontEngine() {
        suspendCancellableCoroutine { continuation -> onWasmReady { continuation.resume(Unit) } }
    }
}
