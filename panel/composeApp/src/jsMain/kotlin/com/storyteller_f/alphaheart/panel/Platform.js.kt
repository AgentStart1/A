/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()
