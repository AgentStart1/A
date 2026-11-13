/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
