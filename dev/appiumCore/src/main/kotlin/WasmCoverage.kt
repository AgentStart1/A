/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.createTestArtifactDirectory
import com.storyteller_f.alphaheart.test.testArtifactRoot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import java.io.File

/** Standalone browser assertions still own case-scoped coverage artifacts. */
fun createWasmCoverageDirectory(testClass: String, testName: String, browser: WasmBrowser): File =
    createTestArtifactDirectory(testArtifactRoot(), testClass, testName, "wasm-${browser.name.lowercase()}")
        .resolve("coverage/wasm")

/** HTTP callbacks own document state on HttpServer's single executor thread. */
internal class WasmCoverage(manifestFile: File, private val output: File) {
    private val manifest = Json.parseToJsonElement(manifestFile.readText()).jsonObject
    val buildId: String = manifest.getValue("buildId").jsonPrimitive.content
    val lineCount: Int = manifest.getValue("lines").jsonArray.size
    private val documents = mutableMapOf<String, JsonElement>()

    init {
        check(buildId.matches(Regex("[0-9a-f]{64}")) && lineCount > 0) { "Invalid Wasm coverage manifest" }
        output.mkdirs()
        manifestFile.copyTo(output.resolve("manifest.json"), overwrite = true)
        output.resolve("status").writeText("incomplete")
    }

    fun record(kind: String, body: String) {
        val snapshot = Json.parseToJsonElement(body).jsonObject
        check(
            snapshot.getValue("buildId").jsonPrimitive.content == buildId,
        ) { "Wasm snapshot belongs to another build" }
        val document = snapshot.getValue("documentKey").jsonPrimitive.content
        check(document.isNotBlank()) { "Missing browser document identity" }
        if (kind == "begin") {
            documents.putIfAbsent(document, JsonNull)
        } else {
            val instances = snapshot.getValue("instances").jsonArray
            check(instances.isNotEmpty()) { "No instrumented Wasm instance was collected" }
            instances.forEach { instance ->
                instance.jsonObject.getValue("covered").jsonArray.forEach { id ->
                    check(id.jsonPrimitive.int in 0 until lineCount) { "Unknown Wasm coverage location" }
                }
            }
            documents[document] = snapshot
        }
        val report = JsonObject(mapOf("buildId" to JsonPrimitive(buildId), "documents" to JsonObject(documents)))
        output.resolve("snapshots.json").writeText(report.toString())
        output.resolve("status").writeText(if (documents.values.none { it == JsonNull }) "complete" else "incomplete")
    }

    fun collect(browser: WebDriver) {
        try {
            val result =
                (browser as JavascriptExecutor).executeAsyncScript(
                    """
                    const done = arguments[arguments.length - 1];
                    const coverage = globalThis.__e2eCoverage;
                    if (!coverage) { done('Coverage runtime is missing'); return; }
                    coverage.collect().then(() => done(null), error => done(String(error)));
                    """.trimIndent(),
                )
            check(result == null) { result ?: "Wasm coverage export failed" }
        } catch (failure: Exception) {
            invalidate(failure)
            throw failure
        }
    }

    fun invalidate(failure: Exception) {
        output.resolve("collection-error.txt").writeText(failure.stackTraceToString())
    }
}
