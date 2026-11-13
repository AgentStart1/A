/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import org.openqa.selenium.edge.EdgeDriver
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.json.Json
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val EDGE_STARTUP_TIMEOUT_SECONDS = 10L

/** Creates a blank target without waiting for Edge's external new-tab navigation. */
internal fun createBlankEdgeDriver(options: EdgeOptions): EdgeDriver {
    val driver = EdgeDriver(options)
    return try {
        val target = createBlankEdgeTarget(driver)
        val windows = driver.windowHandles
        check(target in windows) { "Blank Edge target is missing from WebDriver windows" }
        windows.filter { it != target }.forEach { window ->
            driver.switchTo().window(window)
            driver.close()
        }
        driver.switchTo().window(target)
        driver
    } catch (failure: Throwable) {
        captureFailure(
            ArtifactCollectionError::CollectionFailed,
        ) { driver.quit() }.exceptionOrNull()?.let(failure::addSuppressed)
        throw failure
    }
}

private fun createBlankEdgeTarget(driver: EdgeDriver): String {
    val options = driver.capabilities.getCapability(EdgeOptions.CAPABILITY) as? Map<*, *>
    val address = requireNotNull(options?.get("debuggerAddress") as? String) { "Edge debugger address is missing" }
    val timeout = Duration.ofSeconds(EDGE_STARTUP_TIMEOUT_SECONDS)
    // Page-scoped WebDriver commands wait for MSN; the browser endpoint does not.
    val request =
        HttpRequest.newBuilder(URI.create("http://$address/json/new?about:blank"))
            .timeout(timeout).PUT(HttpRequest.BodyPublishers.noBody()).build()
    return HttpClient.newBuilder().connectTimeout(timeout).build().use { client ->
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == HttpURLConnection.HTTP_OK) { "Unable to create blank Edge target" }
        val target = Json().toType<Map<String, Any?>>(response.body(), Map::class.java)
        requireNotNull(target["id"] as? String) { "Blank Edge target ID is missing" }
    }
}
