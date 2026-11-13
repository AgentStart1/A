/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import java.io.File
import java.nio.file.Path
import java.util.zip.GZIPInputStream

private const val VIEW_DIRECTORY = "artifact-views"
private const val PREVIEW_BYTE_LIMIT = 2 * 1024 * 1024
private const val ARCHIVE_ENTRY_LIMIT = 100
private const val ARCHIVE_BYTE_LIMIT = 8 * 1024 * 1024

/** Static HTML previews work with ordinary file servers and do not execute collected markup. */
fun renderTestArtifacts(directory: File): String {
    val files =
        directory.walkTopDown().onEnter { it.name != VIEW_DIRECTORY }
            .filter { it.isFile && it.name != "index.html" }.sortedBy { it.path }.toList()
    return files.joinToString("\n") { file ->
        val relative = file.relativeTo(directory)
        val view = directory.resolve(VIEW_DIRECTORY).resolve("${relative.path}.html")
        view.parentFile.mkdirs()
        writeArtifactView(file, view)
        val link = encodeArtifactPath(view.relativeTo(directory).toPath())
        "<li><a href=\"$link\">${relative.invariantSeparatorsPath.escapeHtml()}</a></li>"
    }
}

private fun writeArtifactView(file: File, view: File) {
    val rawLink = encodeArtifactPath(file.relativeTo(view.parentFile).toPath())
    val content =
        try {
            renderArtifactContent(file, rawLink)
        } catch (failure: Exception) {
            "<p>Unable to preview this artifact.</p><pre>${failure.stackTraceToString().escapeHtml()}</pre>"
        }
    view.writeText(
        """
        <!doctype html><html lang="en"><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${file.name.escapeHtml()}</title>
        <style>body{font:16px system-ui;margin:2rem}pre{white-space:pre-wrap;overflow-wrap:anywhere}
        img,video{max-width:100%;max-height:85vh}summary{cursor:pointer;margin:1rem 0}</style>
        <h1>${file.name.escapeHtml()}</h1>
        <p><a href="$rawLink" download>Download original</a> (${file.length()} bytes)</p>
        $content</html>
        """.trimIndent(),
    )
}

private fun renderArtifactContent(file: File, rawLink: String): String =
    when {
        file.extension.lowercase() in setOf("png", "jpg", "jpeg", "gif", "webp") ->
            """<img src="$rawLink" alt="${file.name.escapeHtml()}">"""

        file.extension.lowercase() in setOf("mp4", "webm", "ogg") ->
            """<video controls preload="metadata" src="$rawLink"></video>"""

        file.extension.lowercase() == "tar" ->
            TarArchiveInputStream(file.inputStream().buffered()).use(::renderArchive)

        file.extension.lowercase() == "zip" ->
            ZipArchiveInputStream(file.inputStream().buffered()).use(::renderArchive)

        file.name.endsWith(".tar.gz") || file.extension.lowercase() == "tgz" ->
            TarArchiveInputStream(GZIPInputStream(file.inputStream().buffered())).use(::renderArchive)

        else -> file.inputStream().buffered().use { renderText(it.readNBytes(PREVIEW_BYTE_LIMIT + 1)) }
    }

private fun renderArchive(archive: ArchiveInputStream): String =
    buildString {
        append("<h2>Archive contents</h2>")
        var remaining = ARCHIVE_BYTE_LIMIT
        repeat(ARCHIVE_ENTRY_LIMIT) {
            val entry = archive.nextEntry ?: return@buildString
            append("<details open><summary>${entry.name.escapeHtml()} (${entry.size} bytes)</summary>")
            if (!entry.isDirectory && archive.canReadEntryData(entry)) {
                val bytes = archive.readNBytes(minOf(PREVIEW_BYTE_LIMIT + 1, remaining))
                remaining -= bytes.size
                if (bytes.isEmpty() && entry.size > 0) {
                    append("<p>Archive text preview limit reached.</p>")
                } else {
                    append(renderText(bytes))
                }
            }
            append("</details>")
        }
        append("<p>Only the first $ARCHIVE_ENTRY_LIMIT archive entries are shown.</p>")
    }

private fun renderText(bytes: ByteArray): String {
    if (bytes.any { it == 0.toByte() }) return "<p>Binary data has no text preview.</p>"
    val content = bytes.take(PREVIEW_BYTE_LIMIT).toByteArray().toString(Charsets.UTF_8).escapeHtml()
    val notice =
        if (bytes.size >
            PREVIEW_BYTE_LIMIT
        ) {
            "<p>Text preview limited to $PREVIEW_BYTE_LIMIT bytes.</p>"
        } else {
            ""
        }
    return "$notice<pre>$content</pre>"
}

internal fun encodeArtifactPath(path: Path): String =
    path.joinToString("/") { segment ->
        java.net.URLEncoder.encode(segment.toString(), Charsets.UTF_8).replace("+", "%20")
    }
