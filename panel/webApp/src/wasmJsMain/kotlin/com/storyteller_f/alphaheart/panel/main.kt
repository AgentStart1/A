/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.storyteller_f.alphaheart.client.compose_core.utils.WasmFontGate
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class)
fun main() {
    val resourceVersion = document.querySelector("meta[name='compose-resource-version']")?.getAttribute("content")
    if (!resourceVersion.isNullOrEmpty()) {
        configureWebResources {
            resourcePathMapping { path -> "$path?v=$resourceVersion" }
        }
    }
    ComposeViewport {
        WasmFontGate {
            val appScope = rememberCoroutineScope()
            val httpUrl = wasmQueryParameter("appiumHttpUrl") ?: getWasmPanelServerUrl()
            val panelUiViewModel = remember(httpUrl) { PanelUIViewModel(appScope, httpUrl) }
            LaunchedEffect(panelUiViewModel) {
                if (window.location.search.contains("appium=true")) {
                    panelUiViewModel.instance.collectLatest { instance ->
                        localStorage.setItem(
                            "appium.session_ready",
                            (instance is IPanelAccountInstance.Regular).toString(),
                        )
                    }
                }
            }
            CompositionLocalProvider(LocalPanelUiViewModel provides panelUiViewModel) {
                App()
            }
        }
    }
}

private fun wasmQueryParameter(name: String): String? =
    window.location.search
        .removePrefix("?")
        .split('&')
        .firstOrNull { it.substringBefore('=') == name }
        ?.substringAfter('=', missingDelimiterValue = "")
        ?.takeIf(String::isNotEmpty)
