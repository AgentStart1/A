/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import kotlinx.datetime.LocalDateTime
import org.openpdf.text.pdf.PdfReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignedSnapshotTest {
    @Test
    fun `signed snapshots pass cryptographic verification`() {
        val bytes = generateSignedSnapshot("signature-valid", generationSpec()).readBytes()
        assertTrue(verifySignature(bytes), "The complete generated PDF must have a valid signature")
    }

    @Test
    fun `changed signed bytes fail verification`() {
        val bytes = generateSignedSnapshot("signature-tampering", generationSpec()).readBytes()
        assertTrue(verifySignature(bytes), "The original signature must be valid before tampering")
        val producer = bytes.toString(Charsets.ISO_8859_1).indexOf("/Producer")
        assertTrue(producer >= 0, "The fixture must contain the expected metadata dictionary key")
        // ISO-8859-1 preserves byte offsets. Changing key case keeps PDF syntax and ByteRange intact.
        bytes[producer + 1] = 'p'.code.toByte()
        assertFalse(verifySignature(bytes), "A changed byte inside the signed range must invalidate the signature")
    }

    private fun verifySignature(bytes: ByteArray): Boolean {
        val reader = PdfReader(bytes)
        return try {
            val fields = reader.acroFields
            assertEquals(listOf("sig"), fields.signedFieldNames)
            assertTrue(fields.signatureCoversWholeDocument("sig"), "ByteRange must cover the complete PDF")
            fields.verifySignature("sig").verify()
        } finally {
            reader.close()
        }
    }

    private fun generationSpec(): PdfGenerationSpec =
        PdfGenerationSpec(
            LocalDateTime.parse("2026-01-01T10:00:00.987654321"),
            LocalDateTime.parse("2026-01-02T12:30:45.123456789"),
        )
}
