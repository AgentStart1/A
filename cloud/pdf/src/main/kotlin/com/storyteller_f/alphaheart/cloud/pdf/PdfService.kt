/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.pdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.UserInfo
import kotlinx.datetime.LocalDateTime
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.acceptChildren
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.Visitor
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.File

sealed class SnapshotGeneration(open val path: File) {
    class KeyStoreGeneration(
        val keyStoreBase64: String,
        val password: String,
        override val path: File,
        val signedFile: File,
    ) : SnapshotGeneration(path)

    class SimpleGeneration(override val path: File) : SnapshotGeneration(path)
}

/** Both timestamps contain UTC calendar fields, matching shared.utils.now(). */
data class PdfGenerationSpec(val created: LocalDateTime, val captured: LocalDateTime)

interface PdfService {
    fun generateSignedSnapshot(
        creatorInfo: UserInfo,
        authorInfo: UserInfo,
        content: String,
        map: Map<String, File>,
        snapshotGeneration: SnapshotGeneration,
        pdfGenerationSpec: PdfGenerationSpec,
    ): CustomResult<Unit, PdfError>
}

const val BASE_FONT_FAMILY = "Noto Serif"
const val MONO_FONT_FAMILY = "Noto Sans Mono"

fun getFont(): Font {
    val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    return graphicsEnvironment.allFonts.filter {
        it.family == BASE_FONT_FAMILY
    }.firstOrNull {
        it.name == "$BASE_FONT_FAMILY Regular"
    } ?: error("$BASE_FONT_FAMILY not found")
}

fun getMonoFont(): Font {
    val graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment()
    return graphicsEnvironment.allFonts.filter {
        it.family == MONO_FONT_FAMILY
    }.firstOrNull {
        it.name == "$MONO_FONT_FAMILY Regular"
    } ?: error("$MONO_FONT_FAMILY not found")
}

fun collectPlainText(node: ASTNode, content: String): String {
    val sb = StringBuilder()
    node.acceptChildren(
        object : Visitor {
            override fun visitNode(node: ASTNode) {
                if (node.type == MarkdownTokenTypes.TEXT) {
                    sb.append(node.getTextInNode(content))
                } else {
                    node.acceptChildren(this)
                }
            }
        },
    )
    return sb.toString().trim()
}
