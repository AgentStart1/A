/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.WebLocalStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import com.storyteller_f.alphaheart.app.AppConfig
import dev.jordond.connectivity.Connectivity
import dev.jordond.connectivity.ConnectivityProvider
import kotlinx.coroutines.flow.flowOf

actual val appPlatform: AppPlatform
    get() =
        AppPlatform(
            hasNativeBack = false,
            debug = false,
        )

actual fun initEnvironment(context: Any) = Unit

actual fun createConnectivity(): Connectivity =
    Connectivity(
        ConnectivityProvider(flowOf(Connectivity.Status.Connected(metered = false))),
    )

actual fun getClientFile(path: String): ClientFile? = null

private const val APP_PREFERENCES_STORE_NAME = "main.preferences_pb"

private val appPreferencesDataStore by lazy {
    DataStoreFactory.create(
        storage =
        WebLocalStorage(
            serializer = PreferencesSerializer,
            name = APP_PREFERENCES_STORE_NAME,
        ),
    )
}

internal actual fun createAppPreferencesDataStore(): DataStore<Preferences> = appPreferencesDataStore

actual fun unregisterPushService() = Unit

actual fun getDeepLinkHost(): String = AppConfig.DEEP_LINK_HOST

actual fun getDeepLinkScheme(): String = AppConfig.DEEP_LINK_SCHEME_PREFIX
