/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.getOrThrow
import java.io.File
import java.security.MessageDigest
import java.util.Properties

/** TrueType/mono integration uses installed fonts; original CFF2 inputs are pinned test resources. */
internal object PdfFontFixtures {
    private val repository = FontInstanceRepository()
    private val variable by lazy {
        requireNotNull(
            InstalledPdfFonts.installedFaces().firstOrNull {
                it.variable && !it.italic && it.source.file.extension.equals("ttf", ignoreCase = true) && it.family in
                    listOf(
                        "Noto Serif SC",
                        "Noto Serif CJK SC",
                        "Source Han Serif SC",
                    )
            },
        ) {
            "Font tests require an installed standalone variable Noto Serif SC/CJK SC or Source Han Serif SC font"
        }.source.readBytes()
    }
    private val cff2 by lazy { readCff2Fixture("cff2-variable.otf", "pdf.test.cff2.font") }
    private val multiAxis by lazy { readCff2Fixture("cff2-multiaxis.otf", "pdf.test.cff2.multiaxis.font") }
    private val collectionFace by lazy { readCff2Fixture("cff2-collection.otf", "pdf.test.cff2.collection.font") }
    private val mono by lazy { PdfFontFamily.load("mono") }

    private val bundledFixtures by lazy {
        Properties().apply {
            checkNotNull(PdfFontFixtures::class.java.getResourceAsStream("/fonts/fixtures.properties"))
                .buffered().use(::load)
        }
    }

    private fun readCff2Fixture(name: String, property: String): ByteArray {
        val configured = System.getProperty(property)
        val bytes = if (configured == null) readBundledFont(name) else FontFileSource(File(configured)).readBytes()
        require(SfntFont(bytes, 0).hasTable("CFF2")) { "$name must contain CFF2 outlines" }
        return bytes
    }

    private fun readBundledFont(name: String): ByteArray {
        val path = checkNotNull(bundledFixtures.getProperty("$name.resource"))
        val bytes =
            checkNotNull(javaClass.getResourceAsStream("/$path")) { "Missing font fixture: $name" }
                .buffered().use { it.readBytes() }
        val digest =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
        check(digest == bundledFixtures.getProperty("$name.sha256")) { "Font fixture checksum mismatch: $name" }
        return bytes
    }

    fun read(name: String): ByteArray =
        when (name) {
            "variable.ttf" -> variable.copyOf()

            "cff2-variable.otf" -> cff2.copyOf()

            "cff2-multiaxis.otf" -> multiAxis.copyOf()

            "cff2-collection.otf" -> collectionFace.copyOf()

            "mono-regular.ttf" -> mono.getInstance(repository, 400).getOrThrow().copyBytes()

            "mono-bold.ttf" -> mono.getInstance(repository, 700).getOrThrow().copyBytes()

            "serif-regular.ttf" ->
                repository.getInstance(variable, axes = mapOf("wght" to 400f)).getOrThrow().copyBytes()

            "serif-bold.ttf" -> repository.getInstance(variable, axes = mapOf("wght" to 700f)).getOrThrow().copyBytes()

            else -> error("Unknown installed font fixture $name")
        }
}

internal fun fontBytes(name: String): ByteArray = PdfFontFixtures.read(name)
