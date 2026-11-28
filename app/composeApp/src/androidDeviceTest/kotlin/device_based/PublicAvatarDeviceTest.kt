/*
 * This is a private project. All rights reserved.
 */

package device_based

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.storyteller_f.alphaheart.client.compose_core.common.LocalClient
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.core.getClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PublicAvatarDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun concurrentAndRepeatAvatarLoadsSendOneGet() {
        val gets = AtomicInteger()
        val traced = AtomicInteger()
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        val bytes =
            ByteArrayOutputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.toByteArray()
            }
        bitmap.recycle()
        ServerSocket(0, 8, InetAddress.getByName("127.0.0.1")).use { listener ->
            val server = startAvatarServer(listener, bytes, gets, traced)
            val client = getClient {}
            val loader = ImageLoader.Builder(compose.activity).build()
            try {
                val url = "http://127.0.0.1:${listener.localPort}/avatar-${System.nanoTime()}.png"
                lateinit var request: ImageRequest
                compose.setContent {
                    CompositionLocalProvider(LocalClient provides client) {
                        val avatar = globalLoader(url, publicAvatar = true)
                        SideEffect { request = avatar.newBuilder().memoryCachePolicy(CachePolicy.DISABLED).build() }
                    }
                }
                compose.waitForIdle()
                val sources =
                    runBlocking {
                        List(5) {
                            async { assertIs<SuccessResult>(loader.execute(request)).dataSource }
                        }.awaitAll() + assertIs<SuccessResult>(loader.execute(request)).dataSource
                    }
                assertEquals(1, gets.get())
                assertEquals(0, traced.get())
                assertEquals(1, sources.count { it == DataSource.NETWORK })
                assertEquals(5, sources.count { it == DataSource.DISK })
            } finally {
                loader.shutdown()
                client.close()
                listener.close()
                server.join(3000)
            }
        }
    }
}

private fun startAvatarServer(listener: ServerSocket, bytes: ByteArray, gets: AtomicInteger, traced: AtomicInteger) =
    thread(isDaemon = true) {
        while (!listener.isClosed) {
            val socket = runCatching { listener.accept() }.getOrNull() ?: break
            socket.use {
                val input = socket.getInputStream().bufferedReader()
                if (input.readLine()?.startsWith("GET ") == true) gets.incrementAndGet()
                for (line in generateSequence { input.readLine() }.takeWhile(String::isNotEmpty)) {
                    if (line.startsWith("X-Request-ID:", ignoreCase = true)) traced.incrementAndGet()
                }
                val response =
                    listOf(
                        "HTTP/1.1 200 OK",
                        "Content-Type: image/png",
                        "Content-Length: ${bytes.size}",
                        "Connection: close",
                        "",
                        "",
                    ).joinToString("\r\n")
                socket.getOutputStream().buffered().use { output ->
                    output.write(response.encodeToByteArray())
                    output.write(bytes)
                }
            }
        }
    }
