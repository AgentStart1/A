/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.E2eCoverage
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.openqa.selenium.WebDriver
import java.io.File
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class WasmDistributionServer(
    private val root: File,
    backendUrl: String? = null,
    coverageDirectory: File? = null,
    measureCoverage: Boolean = true,
) : AutoCloseable {
    private val coverage =
        if (E2eCoverage.enabled && measureCoverage) {
            WasmCoverage(root.resolve("e2e-coverage-manifest.json"), requireNotNull(coverageDirectory))
        } else {
            null
        }

    @Volatile private var backendUri = backendUrl?.let { URI.create("${it.removeSuffix("/")}/") }
    private val httpClient = HttpClient.newHttpClient()
    val requestedPaths = mutableListOf<String>()
    private val server =
        HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange -> serve(exchange) }
            start()
        }

    val url: String = "http://127.0.0.1:${server.address.port}/"
    val bootstrapUrl: String = "${url}appium-bootstrap.html"

    /** Set once before browser requests; HttpServer handlers read it on their own threads. */
    fun setBackendUrl(url: String) {
        backendUri = URI.create("${url.removeSuffix("/")}/")
    }

    override fun close() {
        server.stop(0)
    }

    fun collectCoverage(browser: WebDriver) {
        coverage?.collect(browser)
    }

    private fun serve(exchange: HttpExchange) {
        val relativePath = exchange.requestURI.path.removePrefix("/").ifBlank { "index.html" }
        if (relativePath.startsWith("__e2e-coverage/")) {
            serveCoverage(exchange, relativePath.substringAfter('/'))
            return
        }
        requestedPaths += relativePath
        if (relativePath == "appium-bootstrap.html") {
            val content = "<!doctype html><title>Appium bootstrap</title>".toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "text/html")
            exchange.sendResponseHeaders(200, content.size.toLong())
            exchange.responseBody.use { it.write(content) }
            return
        }
        val file = root.resolve(relativePath).canonicalFile
        if (!file.path.startsWith(root.canonicalPath) || !file.isFile) {
            if (backendUri != null) {
                proxyToBackend(exchange)
            } else {
                exchange.sendResponseHeaders(404, -1)
            }
            return
        }
        val content =
            if (relativePath == "index.html") {
                val probe = checkNotNull(javaClass.getResource("/wasm-socket-probe.js")).readText()
                val capture =
                    coverage?.let {
                        "<script src='/__e2e-coverage/runtime.js' data-build='${it.buildId}' data-lines='${it.lineCount}'></script>"
                    }.orEmpty()
                file.readText().replace(
                    "<head>",
                    "<head>$capture<script>$probe</script>",
                ).toByteArray(StandardCharsets.UTF_8)
            } else {
                Files.readAllBytes(file.toPath())
            }
        exchange.responseHeaders.add("Content-Type", contentTypeFor(file.extension))
        exchange.responseHeaders.add("Cross-Origin-Opener-Policy", "same-origin")
        exchange.responseHeaders.add("Cross-Origin-Embedder-Policy", "require-corp")
        exchange.sendResponseHeaders(200, content.size.toLong())
        exchange.responseBody.use { it.write(content) }
    }

    private fun serveCoverage(exchange: HttpExchange, operation: String) {
        val collector = coverage
        if (collector == null) {
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
            return
        }
        if (operation == "runtime.js" && exchange.requestMethod == "GET") {
            val script = checkNotNull(javaClass.getResource("/wasm-coverage.js")).readBytes()
            exchange.responseHeaders.add("Content-Type", "text/javascript")
            exchange.sendResponseHeaders(200, script.size.toLong())
            exchange.responseBody.use { it.write(script) }
            return
        }
        try {
            check(
                operation in setOf("begin", "snapshot") && exchange.requestMethod == "POST",
            ) { "Invalid coverage operation" }
            val body = exchange.requestBody.buffered().use { it.readNBytes(MAX_COVERAGE_BODY_BYTES + 1) }
            check(body.size <= MAX_COVERAGE_BODY_BYTES) { "Coverage snapshot is too large" }
            collector.record(operation, body.toString(StandardCharsets.UTF_8))
            exchange.sendResponseHeaders(204, -1)
        } catch (failure: Exception) {
            collector.invalidate(failure)
            exchange.sendResponseHeaders(400, -1)
        } finally {
            exchange.close()
        }
    }

    private fun proxyToBackend(exchange: HttpExchange) {
        val targetUri =
            checkNotNull(backendUri) { "Backend URI is required for proxy requests" }
                .resolve(exchange.requestURI.toString().removePrefix("/"))
        val body = exchange.requestBody.use { it.readAllBytes() }
        val request =
            HttpRequest.newBuilder(targetUri)
                .method(
                    exchange.requestMethod,
                    if (body.isEmpty()) {
                        HttpRequest.BodyPublishers.noBody()
                    } else {
                        HttpRequest.BodyPublishers.ofByteArray(
                            body,
                        )
                    },
                )
                .apply {
                    exchange.requestHeaders.forEach { (name, values) ->
                        if (name.lowercase() !in REQUEST_HEADERS_NOT_FORWARDED) {
                            values.forEach { value -> header(name, value) }
                        }
                    }
                }
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        response.headers().map().forEach { (name, values) ->
            if (name.lowercase() !in RESPONSE_HEADERS_NOT_FORWARDED) {
                values.forEach { value -> exchange.responseHeaders.add(name, value) }
            }
        }
        exchange.sendResponseHeaders(response.statusCode(), response.body().size.toLong())
        exchange.responseBody.use { it.write(response.body()) }
    }

    private fun contentTypeFor(extension: String): String =
        when (extension) {
            "html" -> "text/html"
            "js", "mjs" -> "text/javascript"
            "wasm" -> "application/wasm"
            "css" -> "text/css"
            else -> "application/octet-stream"
        }

    private companion object {
        const val MAX_COVERAGE_BODY_BYTES = 4 * 1024 * 1024
        val REQUEST_HEADERS_NOT_FORWARDED = setOf("connection", "content-length", "host", "upgrade")
        val RESPONSE_HEADERS_NOT_FORWARDED = setOf("connection", "content-length", "transfer-encoding", "upgrade")
    }
}
