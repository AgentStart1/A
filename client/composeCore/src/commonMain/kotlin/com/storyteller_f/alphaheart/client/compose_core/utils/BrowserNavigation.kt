/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * Mirrors a navigation stack into document history on browsers. Nested stacks use a slash-separated
 * id below their parent (for example `app/signin`); native platforms keep their own back handling.
 * History stores route keys only, never editor drafts, credentials or account state.
 */
@Composable
fun BindBrowserNavigation(backStack: NavBackStack<NavKey>, serializersModule: SerializersModule, id: String) {
    val scope = id.substringBefore('/')
    val controller = remember(scope) { getBrowserNavigation(scope) } ?: return
    val json = remember(backStack) { createBrowserNavigationJson(serializersModule) }
    val binding =
        remember(backStack) {
            BrowserNavigationBinding(
                read = { backStack.map { json.encodeToString(PolymorphicSerializer(NavKey::class), it) } },
                restore = { keys ->
                    val routes = keys.map { json.decodeFromString(PolymorphicSerializer(NavKey::class), it) }
                    if (routes.isNotEmpty() && routes != backStack.toList()) {
                        backStack.clear()
                        backStack.addAll(routes)
                    }
                },
            )
        }
    DisposableEffect(controller, binding, id) {
        controller.register(id, binding)
        onDispose { controller.unregister(id, binding) }
    }
    LaunchedEffect(controller, binding, id) {
        snapshotFlow { binding.read() }.collect { controller.update(id, binding, it) }
    }
}

internal expect fun getBrowserNavigation(scope: String): BrowserNavigationController?

internal data class BrowserNavigationBinding(val read: () -> List<String>, val restore: (List<String>) -> Unit)

// Route payloads can themselves contain a `type` field (for example panel task records).
internal fun createBrowserNavigationJson(serializersModule: SerializersModule): Json =
    Json {
        useArrayPolymorphism = true
        this.serializersModule = serializersModule
    }
