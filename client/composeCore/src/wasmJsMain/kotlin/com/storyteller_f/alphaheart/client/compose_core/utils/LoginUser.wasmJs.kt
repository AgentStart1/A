/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.serialization.json.Json

actual fun readInjectedSessionFromPrivateStorageOrNull(): ConvertedRawUserPassInfo? {
    val serializedSession = localStorage.getItem(APPIUM_INJECTED_SESSION_KEY) ?: return null
    val query = window.location.search
    if (!query.contains("appium=true")) return null
    return Json.decodeFromString(ConvertedRawUserPassInfo.serializer(), serializedSession)
}

private const val APPIUM_INJECTED_SESSION_KEY = "appium.injected_session"
