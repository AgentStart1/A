/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.storyteller_f.alphaheart.app.App
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.Downloader
import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import com.storyteller_f.alphaheart.app.common.UploaderImpl
import com.storyteller_f.alphaheart.app.getWasmServerUrl
import com.storyteller_f.alphaheart.app.getWasmWsServerUrl
import com.storyteller_f.alphaheart.client.compose_core.components.ConstPlayItem
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.components.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.MediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
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
    ComposeViewport(document.body!!) {
        WasmFontGate {
            val appScope = rememberCoroutineScope()
            val httpUrl = wasmQueryParameter("appiumHttpUrl") ?: getWasmServerUrl()
            val wsUrl = wasmQueryParameter("appiumWsUrl") ?: getWasmWsServerUrl()
            val uiViewModel = remember(httpUrl, wsUrl) { UIViewModel(appScope, wsUrl, httpUrl) }
            val account by uiViewModel.instance.collectAsState()
            val fileProvider =
                remember(uiViewModel, account) {
                    val register = SimpleTaskRegister(account.accountScope)
                    val downloader = uiViewModel.downloader
                    val uploader =
                        UploaderImpl(
                            uiViewModel,
                            register,
                            retainSelectedFiles = true,
                            // Fetch consumes the request body before transmission; report acknowledged chunks instead.
                            persistTransportProgress = false,
                        )
                    object : ClientFileProvider {
                        override suspend fun getDownloader(): Downloader = downloader
                        override suspend fun getUploader() = uploader
                    }
                }
            LaunchedEffect(uiViewModel) {
                if (window.location.search.contains("appium=true")) {
                    publishAppiumSessionReady(uiViewModel)
                }
            }
            CompositionLocalProvider(
                LocalUiViewModel provides uiViewModel,
                LocalClientFileProvider provides fileProvider,
                LocalMediaPlayerService provides WasmMediaPlayerService,
            ) {
                App()
            }
        }
    }
}

private suspend fun publishAppiumSessionReady(uiViewModel: UIViewModel) {
    uiViewModel.instance.collectLatest { instance ->
        localStorage.setItem(APPIUM_SESSION_READY_KEY, "false")
        if (instance !is IAccountInstance.None) {
            instance.sessionManager.model.userHandler.data.collectLatest { user ->
                localStorage.setItem(APPIUM_SESSION_READY_KEY, (user != null).toString())
            }
        }
    }
}

private const val APPIUM_SESSION_READY_KEY = "appium.session_ready"

private object WasmMediaPlayerService : MediaPlayerService() {
    override val enablePip = false

    override fun fullscreen(remoteMediaItem: RemoteMediaItem) = Unit

    override suspend fun start(
        remoteMediaItem: RemoteMediaItem,
        localMediaPlaySession: LocalMediaPlaySession,
        playList: List<ConstPlayItem>,
    ) = Unit
}

private fun wasmQueryParameter(name: String): String? =
    window.location.search
        .removePrefix("?")
        .split('&')
        .firstOrNull { it.substringBefore('=') == name }
        ?.substringAfter('=', missingDelimiterValue = "")
        ?.takeIf(String::isNotEmpty)
