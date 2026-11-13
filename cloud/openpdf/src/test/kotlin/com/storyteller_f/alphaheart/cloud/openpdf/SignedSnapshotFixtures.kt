/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.alphaheart.cloud.pdf.SnapshotGeneration
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.CryptoJvm
import com.storyteller_f.shared.model.UserInfo
import org.apache.pdfbox.multipdf.PDFCloneUtility
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.Base64

internal fun generateSignedSnapshot(name: String, spec: PdfGenerationSpec): File {
    val directory = File("build/test-output/layout").apply { mkdirs() }
    val keyStore = File.createTempFile("snapshot-", ".p12", directory)
    val signed = File(directory, "$name.pdf")
    val password = "synthetic-test-password"
    try {
        CryptoJvm.createKeystore(password.toCharArray(), keyStore.path)
        OpenPdf().generateSignedSnapshot(
            UserInfo.EMPTY,
            UserInfo.EMPTY,
            "# Signed topic\n\nSigned content remains readable.",
            emptyMap(),
            SnapshotGeneration.KeyStoreGeneration(
                Base64.getEncoder().encodeToString(keyStore.readBytes()),
                password,
                File(directory, "$name-unsigned.pdf"),
                signed,
            ),
            spec,
        ).getOrThrow()
        return signed
    } finally {
        keyStore.delete()
    }
}

/** Signature text lives in a widget appearance, which the ordinary page text extractor ignores. */
internal fun readSignatureAppearance(pdf: PDDocument): String =
    PDDocument().use { appearanceDocument ->
        val widget = pdf.getPage(0).annotations.filterIsInstance<PDAnnotationWidget>().single()
        val appearance = checkNotNull(widget.normalAppearanceStream)
        val clone = object : PDFCloneUtility(appearanceDocument) {}.cloneForNewDocument(appearance.cosObject)
        val page = PDPage(PDRectangle(appearance.bBox.width, appearance.bBox.height))
        appearanceDocument.addPage(page)
        PDPageContentStream(appearanceDocument, page).use { stream ->
            stream.drawForm(PDFormXObject(clone))
        }
        PDFTextStripper().getText(appearanceDocument)
    }
