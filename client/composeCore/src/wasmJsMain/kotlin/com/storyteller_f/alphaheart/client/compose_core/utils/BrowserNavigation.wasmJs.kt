/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.js.JsAny

private val browserNavigationControllers = mutableMapOf<String, BrowserNavigationController>()

internal actual fun getBrowserNavigation(scope: String): BrowserNavigationController? =
    browserNavigationControllers.getOrPut(scope) { BrowserNavigationController(scope, WindowBrowserHistory) }

private object WindowBrowserHistory : BrowserHistory {
    private val bridge = createNavigationHistoryBridge()

    override fun read(): String? = bridge.read()
    override fun replace(value: String) = bridge.write(value, false)
    override fun push(value: String) = bridge.write(value, true)
    override fun go(delta: Int) = window.history.go(delta)

    override fun listen(onPop: () -> Unit): () -> Unit {
        val listener: (Event) -> Unit = { onPop() }
        window.addEventListener("popstate", listener)
        return { window.removeEventListener("popstate", listener) }
    }
}

private external interface NavigationHistoryBridge : JsAny {
    fun read(): String?
    fun write(value: String, push: Boolean)
}

// Preserve other document owners' state and the exact URL, including existing query parameters.
private fun createNavigationHistoryBridge(): NavigationHistoryBridge =
    js(
        """({
            read() {
                const state = window.history.state;
                return typeof state?.aNavigation === 'string' ? state.aNavigation : null;
            },
            write(value, push) {
                const state = {...window.history.state, aNavigation: value};
                if (push) window.history.pushState(state, '');
                else window.history.replaceState(state, '');
            }
        })""",
    )
