/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.storyteller_f.shared.model.RoomInfo
import dev.jordond.connectivity.Connectivity
import io.ktor.http.ContentType
import kotlinx.io.Source

@Stable
interface ClientFile {
    val name: String
    val contentType: ContentType
    val size: Long
    val path: String

    fun source(): Source
}

data class AppPlatform(val hasNativeBack: Boolean, val isActive: Boolean = true, val debug: Boolean)

interface AppPlatformImpl {
    suspend fun notifyNotification(room: RoomInfo, bitmap: ImageBitmap?)
}

lateinit var appPlatformImpl: AppPlatformImpl

expect val appPlatform: AppPlatform

expect fun initEnvironment(context: Any)

expect fun getClientFile(path: String): ClientFile?

expect fun createConnectivity(): Connectivity

internal expect fun createAppPreferencesDataStore(): DataStore<Preferences>

expect fun unregisterPushService()

expect fun getDeepLinkHost(): String

expect fun getDeepLinkScheme(): String
