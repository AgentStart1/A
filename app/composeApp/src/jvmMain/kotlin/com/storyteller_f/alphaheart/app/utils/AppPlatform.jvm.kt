/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.storyteller_f.alphaheart.app.AppConfig
import dev.jordond.connectivity.Connectivity
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.utils.toFile
import io.ktor.http.ContentType
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemTemporaryDirectory
import okio.Path.Companion.toOkioPath
import org.apache.tika.Tika
import java.io.File

val tika = Tika()

actual val appPlatform: AppPlatform
    get() = AppPlatform(false, debug = true)

actual fun initEnvironment(context: Any) = Unit

actual fun createConnectivity(): Connectivity =
    Connectivity {
        autoStart = true
        urls("cloudflare.com")
        port = 80
        pollingIntervalMs = 10.minutes
        timeoutMs = 5.seconds
    }

actual fun getClientFile(path: String): ClientFile? {
    val file = File(path)
    return RegularClientFile(file)
}

class RegularClientFile(val file: File) : ClientFile {
    override val name: String = file.name
    override val contentType: ContentType = ContentType.parse(tika.detect(file))
    override val size: Long = file.length()
    override val path: String = file.path

    override fun source(): Source = file.inputStream().asSource().buffered()
}

private val appPreferencesDataStore by lazy {
    PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            val pb = Path(SystemTemporaryDirectory, "com.storyteller_f.alphaheart.app.main.preferences_pb")
            val file = pb.toFile()
            if (!file.exists() && !file.createNewFile()) {
                Napier.e {
                    "$file create failed"
                }
            }
            file.toOkioPath()
        },
    )
}

internal actual fun createAppPreferencesDataStore(): DataStore<Preferences> = appPreferencesDataStore

actual fun unregisterPushService() = Unit

actual fun getDeepLinkHost(): String = AppConfig.DEEP_LINK_HOST

actual fun getDeepLinkScheme(): String = AppConfig.DEEP_LINK_SCHEME_PREFIX
