/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.backend.core.loadAvif
import com.storyteller_f.alphaheart.backend.core.setLogPath
import com.storyteller_f.alphaheart.cloud.openpdf.OpenPdf
import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.alphaheart.cloud.pdf.SnapshotGeneration
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.shared.CryptoJvm
import com.storyteller_f.shared.model.UserInfo
import de.redsix.pdfcompare.PdfComparator
import kotlinx.datetime.LocalDateTime
import org.apache.pdfbox.pdmodel.encryption.SecurityProvider
import java.io.File
import java.security.Security
import kotlin.test.Test

class SnapshotTest {
    @Test
    fun `test generate signed pdf`() {
        setLogPath()
        val password1 = "123456"
        val path1 = "build/test/keystore2.p12"
        CryptoJvm.createKeystore(password1.toCharArray(), path1)
        Security.addProvider(SecurityProvider.getProvider())
        val pdfFile = File("build/tmp/openpdf.pdf")
        val signedFile = File("build/tmp/openpdf.signed.pdf")
        OpenPdf().generateSignedSnapshot(
            UserInfo.EMPTY,
            UserInfo.EMPTY,
            "hello world",
            emptyMap(),
            SnapshotGeneration.KeyStoreGeneration(
                java.util.Base64.getEncoder().encodeToString(File(path1).readBytes()),
                password1,
                pdfFile,
                signedFile,
            ),
            PdfGenerationSpec(
                LocalDateTime.parse("2023-01-01T00:00:00"),
                LocalDateTime.parse("2023-01-01T00:00:00"),
            ),
        ).getOrThrow()
    }

    @Test
    fun `test generate code fence`() =
        openPdfSnapshot(
            """```kotlin
                |fun main() {
                |    println("hello world")
                |}
                |```
            """.trimMargin(),
        )

    @Test
    fun `test generate code block`() =
        openPdfSnapshot(
            """
                |    fun main() {
                |        println("hello world")
                |    }
            """.trimMargin(),
        )

    @Test
    fun `test generate headings`() =
        openPdfSnapshot(
            """
            # Heading 1
            ## Heading 2
            Normal paragraph

            Heading A
            ===

            Heading B
            ---
            """.trimIndent(),
        )

    @Test
    fun `test generate emphasis and strong`() =
        openPdfSnapshot(
            """
            *italic* and **bold** text with normal content.
            """.trimIndent(),
        )

    @Test
    fun `test generate code span`() =
        openPdfSnapshot(
            """
            Inline `code` span inside a sentence.
            """.trimIndent(),
        )

    @Test
    fun `test generate lists`() =
        openPdfSnapshot(
            """
            - item 1
                - nested item 1.1
            - item 2

            1. first
            2. second
                1. sub first
            """.trimIndent(),
        )

    @Test
    fun `test generate block quote`() =
        openPdfSnapshot(
            """
            > quoted line
            > second line
            """.trimIndent(),
        )

    @Test
    fun `test generate block quote with empty line`() =
        openPdfSnapshot(
            """
            > quoted line
            >
            > second line
            """.trimIndent(),
        )

    @Test
    fun `test generate link`() =
        openPdfSnapshot(
            """
            This is a [link text](https://example.com) in paragraph.
            """.trimIndent(),
        )

    @Test
    fun `test generate horizontal rule`() =
        openPdfSnapshot(
            """
            First paragraph.

            ---

            Second paragraph after horizontal rule.
            """.trimIndent(),
        )

    @Test
    fun `test generate table`() =
        openPdfSnapshot(
            """
            | Header 1 | Header 2 | Header 3 |
            | --- | --- | --- |
            | Cell 1 | Cell 2 | Cell 3 |
            | Cell 4 | Cell 5 | Cell 6 |
            """.trimIndent(),
        )

    @Test
    fun `test generate strikethrough`() =
        openPdfSnapshot(
            """
            This is ~~strikethrough~~ text in a paragraph.
            """.trimIndent(),
        )
}

private fun openPdfSnapshot(content: String, map: Map<String, File> = emptyMap()) {
    setLogPath()
    loadAvif()

    // 从异常堆栈获取当前测试函数名
    val methodName =
        Exception().stackTrace.first {
            it.className.endsWith("SnapshotTest")
        }.methodName

    val pdf = OpenPdf()
    val name = pdf::class.simpleName
    val baseDir = File("build/tmp/${name ?: "<none>"}")

    val snapshotDir = File("src/test/pdf-snapshot/${name ?: "<none>"}").apply { mkdirs() }
    val snapshotFile = File(snapshotDir, "$methodName.pdf")

    val actualFile =
        if (snapshotFile.exists()) {
            File(baseDir, "$methodName.actual.pdf")
        } else {
            snapshotFile
        }
    actualFile.parentFile.mkdirs()
    pdf.generateSignedSnapshot(
        UserInfo.EMPTY,
        UserInfo.EMPTY,
        content,
        map,
        SnapshotGeneration.SimpleGeneration(actualFile),
        PdfGenerationSpec(LocalDateTime.parse("2023-01-01T00:00:00"), LocalDateTime.parse("2023-01-01T00:00:00")),
    ).getOrThrow()
    if (snapshotFile.exists()) {
        val result =
            PdfComparator<de.redsix.pdfcompare.CompareResultImpl>(
                snapshotFile.absolutePath,
                actualFile.absolutePath,
            ).compare()
        val diffFile = baseDir.resolve("$methodName-diff")
        result.writeTo(diffFile.path)

        val updateSnapshots = System.getenv("UPDATE_SNAPSHOTS") == "1"
        if (updateSnapshots && !result.isEqual() && actualFile.canonicalPath != snapshotFile.canonicalPath) {
            actualFile.copyTo(snapshotFile, overwrite = true)
        }
    }
}
