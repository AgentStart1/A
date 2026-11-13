/*
 * This is a private project. All rights reserved.
 */
import com.storytellerf.litert.compat.JniJsonEscaper
import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/** Repairs serialized request JSON without modifying model input or native libraries. */
@CacheableTransform
abstract class LiteRtJniJsonTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile
        if (input.name.startsWith("litertlm-jvm-")) {
            patchLiteRtJniJson(input, outputs.file(input.name))
        } else {
            outputs.file(input)
        }
    }
}

internal fun patchLiteRtJniJson(input: File, output: File) {
    val helper = JniJsonEscaper::class.java.name.replace('.', '/') + ".class"
    val patched = mutableSetOf<String>()
    JarFile(input).use { source ->
        check(source.getJarEntry(helper) == null) { "LiteRT JNI JSON compatibility is already applied" }
        JarOutputStream(output.outputStream().buffered()).use { target ->
            source.entries().asSequence().forEach { entry ->
                target.putNextEntry(JarEntry(entry.name).apply { time = 0L })
                source.getInputStream(entry).use { contents ->
                    if (entry.name.startsWith("com/google/ai/edge/litertlm/") && entry.name.endsWith(".class")) {
                        val result = escapeLiteRtJsonSerializers(contents.readBytes(), helper.removeSuffix(".class"))
                        if (result.second) patched += entry.name
                        target.write(result.first)
                    } else {
                        contents.copyTo(target)
                    }
                }
                target.closeEntry()
            }
            check("com/google/ai/edge/litertlm/Conversation.class" in patched) {
                "LiteRT request serialization changed; review or remove the JNI JSON compatibility transform"
            }
            target.putNextEntry(JarEntry(helper).apply { time = 0L })
            JniJsonEscaper::class.java.getResourceAsStream("/" + helper).use { contents ->
                checkNotNull(contents) { "LiteRT JNI JSON encoder class is missing" }.copyTo(target)
            }
            target.closeEntry()
        }
    }
}

private fun escapeLiteRtJsonSerializers(bytes: ByteArray, helper: String): Pair<ByteArray, Boolean> {
    val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
    var changed = false
    val visitor = object : ClassVisitor(Opcodes.ASM9, writer) {
        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?,
        ): MethodVisitor {
            val delegate = super.visitMethod(access, name, descriptor, signature, exceptions)
            return object : MethodVisitor(Opcodes.ASM9, delegate) {
                override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    if (opcode == Opcodes.INVOKEVIRTUAL && owner in JSON_TYPES && name == "toString" &&
                        descriptor == "()Ljava/lang/String;"
                    ) {
                        // The SDK passes Gson JSON through GetStringUTFChars into a standard UTF-8 JSON parser.
                        // Escape surrogate pairs in the serialized JSON; native JSON decoding restores the same text.
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, helper, "escape", "(Ljava/lang/String;)Ljava/lang/String;", false)
                        changed = true
                    }
                }
            }
        }
    }
    ClassReader(bytes).accept(visitor, 0)
    return writer.toByteArray() to changed
}

private val JSON_TYPES = setOf(
    "com/google/gson/JsonElement",
    "com/google/gson/JsonObject",
    "com/google/gson/JsonArray",
    "com/google/gson/JsonPrimitive",
)
