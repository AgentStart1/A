/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared.utils

import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import kotlinx.serialization.Serializable
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.acceptChildren
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.Visitor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.CancellationToken
import org.intellij.markdown.parser.MarkdownParser

fun extractMarkdownHeadline(markdownText: String): String {
    val source = normalizeMarkdownLineEndings(markdownText)
    val parsedTree = astNode(source)

    val paragraphs = StringBuilder()
    var headline: CharSequence = ""
    var captureContent = false

    val typeList = markdownElementTypes()

    parsedTree.accept(
        object : Visitor {
            override fun visitNode(node: ASTNode) {
                val type = node.type
                when {
                    type == MarkdownElementTypes.MARKDOWN_FILE -> captureContent = true

                    type == MarkdownElementTypes.ATX_1 -> {
                        // Extract the first level header content
                        headline = node.getTextInNode(source).trim().take(50)
                    }

                    type == MarkdownElementTypes.PARAGRAPH -> {
                        if (captureContent) {
                            val content = node.getTextInNode(source).trim()
                            if (content.isNotEmpty()) {
                                paragraphs.appendLine(content)
                            }
                        }
                    }

                    typeList.any {
                        type.name == it.name
                    } -> {
                        // Stop capturing when encountering the first second-level header
                        captureContent = false
                    }
                }

                if (captureContent) {
                    node.acceptChildren(this)
                }
            }
        },
    )
    return if (headline.isNotBlank()) {
        "$headline\n${paragraphs.toString().trim().take(150)}"
    } else {
        extractHeadParagraph(source)
    }
}

fun extractHeadParagraph(markdownText: String): String {
    val source = normalizeMarkdownLineEndings(markdownText)
    val paragraphs = StringBuilder()
    val parsedTree = astNode(source)
    var captureContent = false
    val typeList = markdownElementTypes()
    parsedTree.accept(
        object : Visitor {
            override fun visitNode(node: ASTNode) {
                val type = node.type
                val children = node.children
                when {
                    type == MarkdownElementTypes.MARKDOWN_FILE -> captureContent = true

                    type == MarkdownElementTypes.PARAGRAPH -> {
                        if (captureContent) {
                            val content = node.getTextInNode(source).trim()
                            if (content.isNotEmpty()) {
                                if (children.size == 1 && children.first().type == MarkdownElementTypes.IMAGE) {
                                    paragraphs.appendLine(content)
                                } else {
                                    paragraphs.appendLine(content.take(200))
                                }
                                captureContent = false
                            }
                        }
                    }

                    typeList.any {
                        it.name == type.name
                    } -> {
                        if (paragraphs.length <= 100) {
                            val content = node.getTextInNode(source).trim()
                            paragraphs.appendLine(content)
                        }
                        captureContent = false
                    }
                }

                if (captureContent) {
                    node.acceptChildren(this)
                }
            }
        },
    )
    return paragraphs.toString().trim()
}

fun markdownElementTypes(): List<IElementType> =
    listOf(
        MarkdownElementTypes.MARKDOWN_FILE,
        MarkdownElementTypes.UNORDERED_LIST,
        MarkdownElementTypes.ORDERED_LIST,
        MarkdownElementTypes.LIST_ITEM,
        MarkdownElementTypes.BLOCK_QUOTE,
        MarkdownElementTypes.CODE_FENCE,
        MarkdownElementTypes.CODE_BLOCK,
        MarkdownElementTypes.CODE_SPAN,
        MarkdownElementTypes.HTML_BLOCK,
        MarkdownElementTypes.PARAGRAPH,
        MarkdownElementTypes.EMPH,
        MarkdownElementTypes.STRONG,
        MarkdownElementTypes.LINK_DEFINITION,
        MarkdownElementTypes.LINK_LABEL,
        MarkdownElementTypes.LINK_DESTINATION,
        MarkdownElementTypes.LINK_TITLE,
        MarkdownElementTypes.LINK_TEXT,
        MarkdownElementTypes.INLINE_LINK,
        MarkdownElementTypes.FULL_REFERENCE_LINK,
        MarkdownElementTypes.SHORT_REFERENCE_LINK,
        MarkdownElementTypes.IMAGE,
        MarkdownElementTypes.AUTOLINK,
        MarkdownElementTypes.SETEXT_1,
        MarkdownElementTypes.SETEXT_2,
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6,
    )

fun extractMarkdownMediaLink(markdownText: String): MutableList<String> {
    val source = normalizeMarkdownLineEndings(markdownText)
    val parsedTree = astNode(source)

    val list = mutableListOf<String>()

    parsedTree.accept(
        object : Visitor {
            override fun visitNode(node: ASTNode) {
                when (node.type) {
                    MarkdownElementTypes.IMAGE -> {
                        val imagePath = extractImageUrl(node, source)
                        imagePath?.let { list.add(it) }
                    }

                    MarkdownElementTypes.CODE_FENCE -> {
                        val lang = getLang(node, source)
                        if (lang == "object") {
                            try {
                                val content = readCodeFence(node, source)
                                val obj = commonJson.decodeFromString<MarkdownObject>(content)
                                if (obj.name.isNotBlank() && obj.url.isBlank()) {
                                    list.add(obj.name)
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                node.acceptChildren(this)
            }
        },
    )
    return list
}

fun extractImageUrl(node: ASTNode, markdownText: String): String? {
    val source = normalizeMarkdownLineEndings(markdownText)
    // Extract the first level header content
    val markdownImage = node.getTextInNode(source)

    // 正则表达式匹配 ![alt text](image path "title")
    val regex = Regex("""!\[([^]]*)]\(([^ )]+)(?:\s+"([^"]*)")?\)""")

    val matchResult = regex.find(markdownImage)
    val imagePath =
        if (matchResult != null) {
            matchResult.groupValues[2] // 提取图片路径
        } else {
            null
        }
    return imagePath
}

fun trimMarkdownUnusedContent(markdownText: String): String {
    val source = normalizeMarkdownLineEndings(markdownText)
    val parsedTree = astNode(source)
    return buildString {
        parsedTree.accept(
            object : Visitor {
                /**
                 * 容量为3.
                 */
                val queue = mutableListOf<ASTNode>()

                override fun visitNode(node: ASTNode) {
                    queue.add(node)
                    if (queue.size > 3) {
                        queue.removeAt(0)
                    }
                    if (node.type == MarkdownElementTypes.CODE_FENCE) {
                        val lang = getLang(node, source)
                        val content = readCodeFence(node, source)
                        if (lang == "object") {
                            try {
                                val obj = commonJson.decodeFromString<MarkdownObject>(content)
                                appendLine("```object")
                                appendLine(commonJson.encodeToString<MarkdownObject>(obj))
                                append("```")
                            } catch (_: Exception) {
                            }
                        } else if (lang == "csa") {
                            content.trim().lineSequence().firstNotNullOfOrNull {
                                it.takeIf { it.isNotBlank() }?.trim()
                            }?.let {
                                appendLine("```csa")
                                appendLine(it)
                                append("```")
                            }
                        } else if (lang == "asciidoc") {
                            appendLine("```asciidoc")
                            appendLine(content.trimEnd())
                            append("```")
                        } else {
                            append(node.getTextInNode(source))
                        }
                    } else if (node.type == MarkdownElementTypes.MARKDOWN_FILE) {
                        node.acceptChildren(this)
                    } else if (node.type == MarkdownTokenTypes.EOL) {
                        if (queue.any {
                                it.type != MarkdownTokenTypes.EOL
                            }
                        ) {
                            appendLine()
                        }
                    } else {
                        val value = node.getTextInNode(source)
                        append(value)
                    }
                }
            },
        )
    }.trim()
}

data class MathContext(val content: String, val start: Int, val end: Int, val isInline: Boolean)

fun extractMath(markdownText: String): MutableList<MathContext> {
    val source = normalizeMarkdownLineEndings(markdownText)
    val originalOffsets =
        if (source.length == markdownText.length) null else originalMarkdownOffsets(markdownText, source.length)
    fun originalOffset(offset: Int): Int = originalOffsets?.get(offset) ?: offset
    val parsedTree = astNode(source)
    val images = mutableListOf<MathContext>()
    parsedTree.accept(
        object : Visitor {
            override fun visitNode(node: ASTNode) {
                when (node.type) {
                    GFMElementTypes.INLINE_MATH, GFMElementTypes.BLOCK_MATH -> {
                        val mathContent = readInlineMath(node, source)
                        images.add(
                            MathContext(
                                mathContent,
                                originalOffset(node.startOffset),
                                originalOffset(node.endOffset),
                                node.type == GFMElementTypes.INLINE_MATH,
                            ),
                        )
                    }

                    MarkdownElementTypes.CODE_FENCE -> {
                        val lang = getLang(node, source)
                        if (lang == "math") {
                            val content = readCodeFence(node, source)
                            images.add(
                                MathContext(
                                    content,
                                    originalOffset(node.startOffset),
                                    originalOffset(node.endOffset),
                                    false,
                                ),
                            )
                        }
                    }
                }

                node.acceptChildren(this)
            }
        },
    )
    return images
}

private fun originalMarkdownOffsets(source: String, normalizedLength: Int): IntArray {
    val offsets = IntArray(normalizedLength + 1)
    var original = 0
    var normalized = 0
    while (original < source.length) {
        offsets[normalized++] = original
        original += if (source[original] == '\r' && source.getOrNull(original + 1) == '\n') 2 else 1
    }
    offsets[normalized] = source.length
    return offsets
}

/** Rendering and syntax inspection must normalize both the parser input and text read through its offsets. */
fun normalizeMarkdownLineEndings(source: String): String =
    if ('\r' in source) source.replace("\r\n", "\n").replace('\r', '\n') else source

/** Parse a presentation view; node offsets address [normalizeMarkdownLineEndings] of the input. */
fun astNode(markdownText: String): ASTNode {
    val flavour = GFMFlavourDescriptor()
    val parser = MarkdownParser(flavour, cancellationToken = CancellationToken.NonCancellable)
    val text: CharSequence = normalizeMarkdownLineEndings(markdownText)
    return parser.buildMarkdownTreeFromString(text)
}

fun readCodeFence(node: ASTNode, content: String): String {
    val source = normalizeMarkdownLineEndings(content)
    val children = node.children
    val langOffset =
        children.indexOfFirst {
            it.type == MarkdownTokenTypes.FENCE_LANG
        }
    val start =
        children.subList(langOffset + 1, children.size).first {
            it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT
        }.startOffset
    val end =
        children.last {
            it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT
        }.endOffset
    return source.substring(start, end)
}

fun readInlineMath(node: ASTNode, content: String): String {
    val source = normalizeMarkdownLineEndings(content)
    val children = node.children
    val langOffset =
        children.first {
            it.type == GFMTokenTypes.DOLLAR
        }.endOffset
    val end =
        children.last {
            it.type == GFMTokenTypes.DOLLAR
        }.startOffset
    return source.substring(langOffset, end).removeSurrounding("`")
}

fun getLang(node: ASTNode, content: String): String {
    val source = normalizeMarkdownLineEndings(content)
    val children = node.children
    val langOffset =
        children.indexOfFirst {
            it.type == MarkdownTokenTypes.FENCE_LANG
        }
    val languageNode = children.getOrNull(langOffset) ?: return ""
    return languageNode.getTextInNode(source).toString().lowercase()
}

@Serializable
data class MarkdownObject(
    val name: String = "",
    val url: String = "",
    val isPlayList: Boolean? = null,
    val cover: String? = null,
    val title: String? = null,
)

fun generateImageMarkdownContent(info: FileInfo): String = """![${info.name}](${info.name} "${info.name}")"""

fun generateObjectMarkdownContent(info: FileInfo): String =
    """```object
${commonJson.encodeToString(MarkdownObject(name = info.name))}
```"""

fun generateModelMarkdownContent(objectTuple: ObjectTuple): String =
    """```csa
/${
        when (objectTuple.objectType) {
            ObjectType.USER -> "user"
            ObjectType.COMMUNITY -> "community"
            ObjectType.ROOM -> "room"
            ObjectType.TOPIC -> "topic"
            else -> error("unknown object type ${objectTuple.objectType}")
        }
    }/${objectTuple.objectId}
```"""
