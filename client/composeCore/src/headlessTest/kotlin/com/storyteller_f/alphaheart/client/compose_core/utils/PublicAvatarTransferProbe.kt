/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.DataSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.toUri
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal fun verifyPublicAvatarTransfers(context: PlatformContext) =
    runBlocking(Dispatchers.Default) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val gets = AtomicInteger()
        val payload = byteArrayOf(1, 2, 3, 4)
        server.createContext("/avatar.jpg") { exchange ->
            exchange.use {
                check(exchange.requestHeaders.getFirst("X-Request-ID") == null)
                gets.incrementAndGet()
                Thread.sleep(120)
                exchange.responseHeaders.set("Content-Type", "image/jpeg")
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.buffered().use { it.write(payload) }
            }
        }
        server.start()
        val loader = ImageLoader.Builder(context).build()
        try {
            assertNotNull(loader.diskCache)
            val uri = "http://127.0.0.1:${server.address.port}/avatar.jpg?probe=${UUID.randomUUID()}".toUri()
            suspend fun load(): DataSource {
                val fetcher = assertNotNull(publicAvatarFetcher.create(uri, Options(context), loader))
                val result = assertIs<SourceFetchResult>(fetcher.fetch())
                result.source.use { assertTrue(it.source().readByteArray().contentEquals(payload)) }
                return result.dataSource
            }

            val simultaneous = List(5) { async { load() } }.awaitAll()
            assertEquals(1, simultaneous.count { it == DataSource.NETWORK })
            assertEquals(4, simultaneous.count { it == DataSource.DISK })
            assertEquals(DataSource.DISK, load())
            assertEquals(1, gets.get())
        } finally {
            loader.shutdown()
            server.stop(0)
        }
    }
