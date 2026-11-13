/*
 * This is a private project. All rights reserved.
 */
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.storytellerf.litert.compat.JniJsonEscaper
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.URLClassLoader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

class LiteRtJniJsonTransformTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `JSON encoding preserves supplementary characters and literal escapes`() {
        val text = "Greek Ελληνικά 中文 \uD83D\uDE00 \uD834\uDD1E quotes\" slash\\ literal\\ud83d\u0000\n"
        val original = JsonObject().apply { addProperty("text", text) }
        val encoded = JniJsonEscaper.escape(original.toString())
        assertFalse(encoded.any(Char::isSurrogate))
        assertEquals(original, JsonParser.parseString(encoded))
    }

    @Test
    fun `unpaired surrogates fail before entering native code`() {
        for (invalid in listOf("\uD83D", "\uDE00", "\uD83Dtext")) {
            assertThrows(IllegalArgumentException::class.java) { JniJsonEscaper.escape("\"$invalid\"") }
        }
    }

    @Test
    fun `transformed SDK JSON survives Modified UTF-8 as the same JSON value`() {
        val input = fixture()
        val output = File(input.parentFile, "patched.jar")
        patchLiteRtJniJson(input, output)
        val gson = JsonObject::class.java.protectionDomain.codeSource.location
        URLClassLoader(arrayOf(output.toURI().toURL(), gson), ClassLoader.getPlatformClassLoader()).use { loader ->
            val jsonClass = loader.loadClass("com.google.gson.JsonObject")
            val json = jsonClass.getConstructor().newInstance()
            val text = "Hello \uD83D\uDE00 and \uD834\uDD1E"
            jsonClass.getMethod("addProperty", String::class.java, String::class.java).invoke(json, "text", text)
            val sdk = loader.loadClass("com.google.ai.edge.litertlm.Conversation")
            val encoded = sdk.getMethod("encode", jsonClass).invoke(null, json) as String
            val nativeJson = decodeStandardUtf8(modifiedUtf8(encoded))
            assertEquals(text, JsonParser.parseString(nativeJson).asJsonObject["text"].asString)
        }
        val unpatched = JsonObject().apply { addProperty("text", "Hello \uD83D\uDE00") }.toString()
        assertThrows(java.nio.charset.CharacterCodingException::class.java) { decodeStandardUtf8(modifiedUtf8(unpatched)) }
    }

    @Test
    fun `transform preserves the bundled native library bytes`() {
        val input = fixture()
        val output = File(input.parentFile, "patched.jar")
        patchLiteRtJniJson(input, output)
        JarFile(output).use { jar ->
            assertArrayEquals(byteArrayOf(1, 2, 3, 4), jar.getInputStream(jar.getJarEntry("native.so")).readBytes())
            assertNotNull(jar.getJarEntry(JniJsonEscaper::class.java.name.replace('.', '/') + ".class"))
        }
    }

    @Test
    fun `unsupported SDK serialization fails the transform`() {
        val dir = temporaryFolder.newFolder()
        val input = File(dir, "empty.jar")
        JarOutputStream(input.outputStream()).close()
        assertThrows(IllegalStateException::class.java) { patchLiteRtJniJson(input, File(dir, "out.jar")) }
    }

    private fun fixture(): File {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/google/ai/edge/litertlm/Conversation", null, "java/lang/Object", null)
        val method = writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "encode", "(Lcom/google/gson/JsonObject;)Ljava/lang/String;", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/google/gson/JsonObject", "toString", "()Ljava/lang/String;", false)
        method.visitInsn(Opcodes.ARETURN)
        method.visitMaxs(1, 1)
        method.visitEnd()
        writer.visitEnd()
        val file = File(temporaryFolder.newFolder(), "sdk.jar")
        JarOutputStream(file.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry("com/google/ai/edge/litertlm/Conversation.class"))
            jar.write(writer.toByteArray())
            jar.closeEntry()
            jar.putNextEntry(JarEntry("native.so"))
            jar.write(byteArrayOf(1, 2, 3, 4))
            jar.closeEntry()
        }
        return file
    }

    private fun modifiedUtf8(text: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { it.writeUTF(text) }
        return bytes.toByteArray().drop(2).toByteArray()
    }

    private fun decodeStandardUtf8(bytes: ByteArray): String =
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
}
