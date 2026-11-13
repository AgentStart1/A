/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
