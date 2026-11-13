/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import java.io.File

/** Source identity includes the collection face; font contents are read only when selected. */
internal data class FontFileSource(val file: File, val faceIndex: Int = 0) {
    fun readBytes(): ByteArray = file.inputStream().buffered().use { it.readBytes() }
}

internal data class InstalledFontFace(
    val source: FontFileSource,
    val family: String,
    val weight: Int,
    val variable: Boolean,
    val italic: Boolean,
)

/** System directory discovery works in headless JVMs without AWT or private JDK font APIs. */
internal object InstalledPdfFonts {
    private val faces by lazy { scan(directories()) }
    private val serifFamilies = listOf("Noto Serif SC", "Noto Serif CJK SC", "Source Han Serif SC")
    private val monoFamilies = listOf("Noto Sans Mono", "DejaVu Sans Mono", "Liberation Mono", "Consolas", "Menlo")

    fun load(name: String): PdfFontFamily = select(name, faces)

    fun installedFaces(): List<InstalledFontFace> = faces

    fun select(name: String, available: List<InstalledFontFace>): PdfFontFamily {
        val families =
            when (name) {
                "serif" -> serifFamilies
                "mono" -> monoFamilies
                else -> error("Unknown PDF font role $name")
            }
        val candidates =
            available.filter { !it.italic && it.family in families }
                .sortedWith(
                    compareBy({
                        families.indexOf(it.family)
                    }, { it.source.file.path }, { it.source.faceIndex }),
                )
        val regular = candidates.firstOrNull { !it.variable && it.weight == 400 }?.source
        val bold = candidates.firstOrNull { !it.variable && it.weight == 700 }?.source
        val variable = candidates.firstOrNull { it.variable }?.source
        require(regular != null || variable != null) { "Install a PDF $name Regular font: ${families.joinToString()}" }
        require(bold != null || variable != null) { "Install a PDF $name Bold font: ${families.joinToString()}" }
        return PdfFontFamily.fromFiles(regular, bold, variable)
    }

    fun scan(roots: List<File>): List<InstalledFontFace> =
        roots.flatMap { root ->
            root.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf", "ttc", "otc") }
                .flatMap { file ->
                    // Unrelated unsupported or inaccessible installed fonts cannot block discovery.
                    captureFontFailure { InstalledFontMetadata.read(file) }.getOrNull().orEmpty().asSequence()
                }.toList()
        }

    private fun directories(): List<File> {
        val home = File(System.getProperty("user.home"))
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.startsWith("windows") ->
                listOf(
                    File(System.getenv("WINDIR") ?: "C:/Windows", "Fonts"),
                    File(
                        System.getenv("LOCALAPPDATA") ?: home.resolve("AppData/Local").path,
                        "Microsoft/Windows/Fonts",
                    ),
                )

            os.contains(
                "mac",
            ) -> listOf(File("/System/Library/Fonts"), File("/Library/Fonts"), home.resolve("Library/Fonts"))

            else ->
                listOf(
                    File("/usr/share/fonts"),
                    File("/usr/local/share/fonts"),
                    home.resolve(".fonts"),
                    File(System.getenv("XDG_DATA_HOME") ?: home.resolve(".local/share").path, "fonts"),
                )
        }
    }
}
