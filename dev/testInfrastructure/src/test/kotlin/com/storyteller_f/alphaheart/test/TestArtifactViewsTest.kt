/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestArtifactViewsTest {
    @Test
    fun previewTextWithoutExecutingCollectedMarkup() =
        withDirectory { directory ->
            directory.resolve("application.log").writeText("<script>alert('log')</script> & text")
            directory.resolve("page-source.xml").writeText("<hierarchy><node/></hierarchy>")
            val links = renderTestArtifacts(directory)
            assertTrue(links.contains("href=\"artifact-views/application.log.html\""))
            val log = directory.resolve("artifact-views/application.log.html").readText()
            assertTrue(log.contains("&lt;script&gt;alert(&#39;log&#39;)&lt;/script&gt; &amp; text"))
            assertFalse(log.contains("<script>"))
            assertTrue(log.contains("href=\"../application.log\" download"))
            val source = directory.resolve("artifact-views/page-source.xml.html").readText()
            assertTrue(source.contains("&lt;hierarchy&gt;&lt;node/&gt;&lt;/hierarchy&gt;"))
            assertEquals(links, renderTestArtifacts(directory))
        }

    @Test
    fun inspectArchiveEntriesWithoutExtractingPaths() =
        withDirectory { directory ->
            val text = "service <event>".toByteArray()
            TarArchiveOutputStream(directory.resolve("server.tar").outputStream().buffered()).use { archive ->
                archive.putArchiveEntry(TarArchiveEntry("../../outside.log").also { it.size = text.size.toLong() })
                archive.write(text)
                archive.closeArchiveEntry()
            }
            ZipOutputStream(directory.resolve("diagnostics.zip").outputStream().buffered()).use { archive ->
                archive.putNextEntry(ZipEntry("<entry>.log"))
                archive.write(text)
                archive.closeEntry()
            }
            renderTestArtifacts(directory)
            val tar = directory.resolve("artifact-views/server.tar.html").readText()
            assertTrue(tar.contains("../../outside.log"))
            assertTrue(tar.contains("service &lt;event&gt;"))
            val zip = directory.resolve("artifact-views/diagnostics.zip.html").readText()
            assertTrue(zip.contains("&lt;entry&gt;.log"))
            assertTrue(zip.contains("service &lt;event&gt;"))
            assertFalse(directory.parentFile.resolve("outside.log").exists())
        }

    @Test
    fun embedMediaAndHandleBrokenArchives() =
        withDirectory { directory ->
            directory.resolve("screenshot.png").writeBytes(byteArrayOf(1))
            directory.resolve("recording # &.mp4").writeBytes(byteArrayOf(2))
            directory.resolve("broken.zip").writeText("broken archive")
            val links = renderTestArtifacts(directory)
            assertTrue(links.contains("recording%20%23%20%26.mp4.html"))
            val image = directory.resolve("artifact-views/screenshot.png.html").readText()
            assertTrue(image.contains("""<img src="../screenshot.png""""))
            val video = directory.resolve("artifact-views/recording # &.mp4.html").readText()
            assertTrue(video.contains("""<video controls preload="metadata" src="../recording%20%23%20%26.mp4""""))
            assertTrue(directory.resolve("artifact-views/broken.zip.html").exists())
        }

    private fun withDirectory(block: (File) -> Unit) {
        val parent = File("build/test/artifact-views").also { it.mkdirs() }
        val directory = Files.createTempDirectory(parent.toPath(), "case-").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
