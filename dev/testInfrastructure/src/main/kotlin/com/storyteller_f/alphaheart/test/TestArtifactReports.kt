/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import java.io.File
import java.util.Properties

/** Entry point invoked once by the owning Test task's Gradle finalizer. */
object TestArtifactReportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Expected raw artifact and report directories" }
        generateTestArtifactReports(File(args[0]), File(args[1]))
    }
}

/** Generates a standalone report from completed raw results without modifying those results. */
fun generateTestArtifactReports(raw: File, output: File) {
    val inputPath = raw.canonicalFile.toPath()
    val outputPath = output.canonicalFile.toPath()
    require(!inputPath.startsWith(outputPath) && !outputPath.startsWith(inputPath)) {
        "Raw results and generated reports must use separate directory trees"
    }
    check(!output.exists() || output.deleteRecursively()) { "Unable to replace artifact report" }
    check(output.mkdirs()) { "Unable to create artifact report directory" }
    val results = raw.walkTopDown().filter { it.isFile && it.name == "result.properties" }.sortedBy { it.path }
    val entries =
        results.map { result ->
            val directory = output.resolve(result.parentFile.relativeTo(raw).path)
            check(result.parentFile.copyRecursively(directory)) { "Unable to copy collected artifacts" }
            renderTestCaseReport(directory)
            val link = encodeArtifactPath(directory.resolve("index.html").relativeTo(output).toPath())
            val details = readTestArtifactResult(result)
            val label =
                listOf(
                    details.getProperty("suite"),
                    details.getProperty("testName"),
                    details.getProperty("platform"),
                )
                    .filter(String::isNotBlank).joinToString(" / ")
            "<li><a href=\"$link\">${label.escapeHtml()}</a> — ${details.getProperty("status").escapeHtml()}</li>"
        }.toList()
    output.resolve("index.html").writeText(
        """<!doctype html><html lang="en"><meta charset="utf-8"><title>Test artifacts</title>""" +
            "<h1>Test artifacts</h1><ul>${entries.joinToString("\n")}</ul></html>",
    )
}

private fun renderTestCaseReport(directory: File) {
    val details = readTestArtifactResult(directory.resolve("result.properties"))
    val errors =
        directory.walkTopDown().filter { it.isFile && it.name.endsWith(".error.txt") }
            .joinToString("\n") { "${it.relativeTo(directory)}: ${it.readText()}" }
    val failure = directory.resolve("failure.txt").takeIf(File::isFile)?.readText().orEmpty()
    val collectionErrors = directory.resolve("collection-errors.txt").takeIf(File::isFile)?.readText().orEmpty()
    val artifacts = renderTestArtifacts(directory)
    directory.resolve("index.html").writeText(
        """
        <!doctype html><html lang="en"><meta charset="utf-8">
        <title>${details.getProperty("testName").escapeHtml()} — ${details.getProperty("status").escapeHtml()}</title>
        <h1>${details.getProperty("testName").escapeHtml()}</h1>
        <p>Suite: ${details.getProperty(
            "suite",
        ).escapeHtml()} | Platform: ${details.getProperty("platform").escapeHtml()}</p>
        <p>Result: ${details.getProperty("status").escapeHtml()}</p>
        <p>Started: ${details.getProperty(
            "started",
        ).escapeHtml()} | Finished: ${details.getProperty("finished").escapeHtml()}</p>
        <h2>Failure</h2><pre>${failure.escapeHtml()}</pre>
        <h2>Collection errors</h2><pre>${collectionErrors.escapeHtml()}${errors.escapeHtml()}</pre>
        <h2>Artifacts</h2><ul>$artifacts</ul></html>
        """.trimIndent(),
    )
}

private fun readTestArtifactResult(file: File): Properties =
    Properties().apply { file.inputStream().buffered().use { load(it) } }

internal fun String.escapeHtml(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
