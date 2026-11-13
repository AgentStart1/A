/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.storyteller_f.alphaheart.backend.core.setLogPath
import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.test.runTest
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AddPresetTest {
    @Test
    fun `parse yaml download config with archive excludes`() {
        setLogPath()
        val tempDir = createTempDirectory(prefix = "addpreset-yaml-").toFile()
        try {
            val configFile = File(tempDir, "sample.download.yaml")
            configFile.writeText(
                """
                name: dataset.zip
                link: https://example.com/dataset.zip
                hash: sha256:abc123
                excludeArchiveEntries:
                  - "**/*.md"
                  - "docs/**"
                """.trimIndent(),
            )

            val config = parseDownloadConfig(configFile)
            assertNotNull(config)
            assertEquals("dataset.zip", config.name)
            assertEquals("https://example.com/dataset.zip", config.link)
            assertEquals("sha256:abc123", config.hash)
            assertEquals(listOf("**/*.md", "docs/**"), config.excludeArchiveEntries)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `fallback to legacy 3-line download config`() {
        setLogPath()
        val tempDir = createTempDirectory(prefix = "addpreset-legacy-").toFile()
        try {
            val configFile = File(tempDir, "legacy.download")
            configFile.writeText(
                """
                archive.zip
                https://example.com/archive.zip
                sha256:deadbeef
                """.trimIndent(),
            )

            val config = parseDownloadConfig(configFile)
            assertNotNull(config)
            assertEquals("archive.zip", config.name)
            assertEquals("https://example.com/archive.zip", config.link)
            assertEquals("sha256:deadbeef", config.hash)
            assertTrue(config.excludeArchiveEntries.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `repack zip excludes matched entries`() {
        setLogPath()
        val tempDir = createTempDirectory(prefix = "addpreset-repack-").toFile()
        try {
            val zipFile = File(tempDir, "bundle.zip")
            writeZip(
                zipFile,
                mapOf(
                    "keep.txt" to "keep",
                    "docs/readme.md" to "remove-md",
                    "nested/secret.txt" to "remove-folder",
                    "images/logo.png" to "keep-image",
                ),
            )

            repackArchiveWithExclusionsAndInclusionsInPlace(
                zipFile,
                listOf("**/*.md", "nested/**"),
                emptyList(),
            )

            ZipFile(zipFile).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toSet()
                assertTrue("keep.txt" in names)
                assertTrue("images/logo.png" in names)
                assertFalse("docs/readme.md" in names)
                assertFalse("nested/secret.txt" in names)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `repack keeps included files after exclusions`() {
        setLogPath()
        val tempDir = createTempDirectory(prefix = "addpreset-include-").toFile()
        try {
            val zipFile = File(tempDir, "bundle.zip")
            writeZip(
                zipFile,
                mapOf(
                    "keep.txt" to "keep",
                    "docs/readme.md" to "remove-md",
                    "nested/secret.txt" to "remove-folder",
                    "images/logo.png" to "keep-image",
                    "images/other.jpg" to "other-image",
                ),
            )

            val exclude = listOf("**/*.md", "nested/**")
            val include = listOf("keep.txt", "images/logo.png")
            repackArchiveWithExclusionsAndInclusionsInPlace(
                zipFile,
                exclude,
                include,
            )

            ZipFile(zipFile).use { zip ->
                val names = zip.entries().asSequence().map { it.name }.toSet()
                assertTrue("keep.txt" in names)
                assertTrue("images/logo.png" in names)
                assertFalse("docs/readme.md" in names)
                assertFalse("nested/secret.txt" in names)
                assertFalse("images/other.jpg" in names)
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `yaml download returns the processed archive`() =
        runTest {
            setLogPath()
            val tempDir = createTempDirectory(prefix = "addpreset-e2e-").toFile()
            val sourceZip = writeDownloadSourceZip(tempDir)
            val hash = sha256File(sourceZip)
            val server = HttpServer.create(InetSocketAddress(0), 0)
            val requestCount = AtomicInteger()
            try {
                server.createBundleContext(sourceZip, requestCount)
                server.start()

                val configFile = writeDownloadConfig(tempDir, server.address.port, hash)

                HttpClient(OkHttp).use { client ->
                    val downloaded = downloadPresetFileIfNeed(configFile.name, tempDir, client)
                    assertNotNull(downloaded)
                    assertEquals(File(tempDir, "download/processed/bundle.zip").canonicalPath, downloaded.canonicalPath)
                    assertTrue(downloaded.exists())
                    assertEquals(1, requestCount.get())

                    val originalDownload = File(tempDir, "download/bundle.zip")
                    assertDownloadedArchives(downloaded, originalDownload, hash)

                    val secondResult = downloadPresetFileIfNeed(configFile.name, tempDir, client)
                    assertNotNull(secondResult)
                    assertEquals(downloaded.canonicalPath, secondResult.canonicalPath)
                    assertEquals(1, requestCount.get())
                }
            } finally {
                server.stop(0)
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `recognize download config path variants`() {
        setLogPath()
        assertTrue(isDownloadConfigPath("a/b/xxx.download"))
        assertTrue(isDownloadConfigPath("a/b/xxx.download.yaml"))
        assertTrue(isDownloadConfigPath("a/b/xxx.download.yml"))
        assertFalse(isDownloadConfigPath("a/b/xxx.txt"))
    }

    @Test
    fun sharedSourceKeepsFilteredFontFamiliesIndependent() =
        runTest {
            setLogPath()
            val parent = File("build/test-font-config").apply { mkdirs() }
            val tempDir = createTempDirectory(parent.toPath(), "shared-source-").toFile()
            val sourceZip = File(tempDir, "source.zip")
            writeZip(
                sourceZip,
                mapOf(
                    "NotoSansNerdFont-Regular.ttf" to "sans",
                    "NotoSansNerdFont-BoldItalic.ttf" to "sans-bold-italic",
                    "NotoSansNerdFont-Medium.ttf" to "medium",
                    "NotoSansCondensedNerdFont-Regular.ttf" to "condensed",
                    "NotoSerifNerdFont-Regular.ttf" to "serif",
                    "NotoSerifNerdFont-Italic.ttf" to "serif-italic",
                ),
            )
            val hash = sha256File(sourceZip)
            val server = HttpServer.create(InetSocketAddress(0), 0)
            val requestCount = AtomicInteger()
            try {
                server.createBundleContext(sourceZip, requestCount)
                server.start()
                val names = listOf("Sans", "Serif")
                names.forEach { family ->
                    writeFamilyDownloadConfig(tempDir, family, server.address.port, hash)
                }
                HttpClient(OkHttp).use { client ->
                    val sans = downloadPresetFileIfNeed("Sans.download.yaml", tempDir, client)
                    val serif = downloadPresetFileIfNeed("Serif.download.yaml", tempDir, client)
                    assertNotNull(sans)
                    assertNotNull(serif)
                    ZipFile(sans).use { zip ->
                        assertEquals(
                            setOf("NotoSansNerdFont-Regular.ttf", "NotoSansNerdFont-BoldItalic.ttf"),
                            zip.entries().asSequence().map { it.name }.toSet(),
                        )
                    }
                    ZipFile(serif).use { zip ->
                        assertEquals(
                            setOf("NotoSerifNerdFont-Regular.ttf", "NotoSerifNerdFont-Italic.ttf"),
                            zip.entries().asSequence().map { it.name }.toSet(),
                        )
                    }
                    assertEquals(1, requestCount.get())
                    assertEquals(hash, sha256File(File(tempDir, "download/Noto.zip")))
                }
            } finally {
                server.stop(0)
                tempDir.deleteRecursively()
            }
        }

    private fun writeFamilyDownloadConfig(tempDir: File, family: String, port: Int, hash: String) {
        File(tempDir, "$family.download.yaml").writeText(
            """
            name: $family.zip
            sourceName: Noto.zip
            link: http://localhost:$port/bundle.zip
            hash: sha256:$hash
            excludeArchiveEntries:
              - "*Condensed*.ttf"
            includeArchiveEntries:
              - "Noto${family}NerdFont-{Regular,Bold,Italic,BoldItalic}.ttf"
            """.trimIndent(),
        )
    }

    private fun writeZip(target: File, contentByPath: Map<String, String>) {
        setLogPath()
        target.parentFile?.mkdirs()
        ZipOutputStream(target.outputStream().buffered()).use { out ->
            contentByPath.forEach { (path, content) ->
                out.putNextEntry(ZipEntry(path))
                out.write(content.toByteArray())
                out.closeEntry()
            }
        }
    }

    private fun writeDownloadSourceZip(tempDir: File): File {
        val sourceZip = File(tempDir, "source.zip")
        writeZip(
            sourceZip,
            mapOf(
                "keep.txt" to "keep",
                "docs/readme.md" to "remove",
                "nested/secret.txt" to "remove-folder",
            ),
        )
        return sourceZip
    }

    private fun HttpServer.createBundleContext(sourceZip: File, requestCount: AtomicInteger) {
        createContext("/bundle.zip") { exchange ->
            requestCount.incrementAndGet()
            val body = sourceZip.readBytes()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { output ->
                output.write(body)
            }
        }
    }

    private fun writeDownloadConfig(tempDir: File, port: Int, hash: String): File {
        val configFile = File(tempDir, "bundle.download.yaml")
        configFile.writeText(
            """
            name: bundle.zip
            link: http://127.0.0.1:$port/bundle.zip
            hash: sha256:$hash
            excludeArchiveEntries:
              - "**/*.md"
              - "nested/**"
            """.trimIndent(),
        )
        return configFile
    }

    private fun assertDownloadedArchives(downloaded: File, originalDownload: File, hash: String) {
        assertTrue(originalDownload.exists())
        assertEquals(hash, sha256File(originalDownload))
        assertZipEntries(downloaded, removed = setOf("docs/readme.md", "nested/secret.txt"))
        assertZipEntries(originalDownload, kept = setOf("docs/readme.md", "nested/secret.txt"))
    }

    private fun assertZipEntries(file: File, kept: Set<String> = emptySet(), removed: Set<String> = emptySet()) {
        ZipFile(file).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toSet()
            assertTrue("keep.txt" in names)
            kept.forEach { assertTrue(it in names) }
            removed.forEach { assertFalse(it in names) }
        }
    }
}
