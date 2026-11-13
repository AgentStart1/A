/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
