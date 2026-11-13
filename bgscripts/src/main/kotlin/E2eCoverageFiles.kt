/*
 * This is a private project. All rights reserved.
 */

import kotlinx.kover.features.jvm.ClassFilters
import kotlinx.kover.features.jvm.KoverLegacyFeatures
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Instruments source-owned classes only; external runtimes and generated classes remain intact. */
internal class E2eCoverageFiles(sources: Set<File>, private val originals: File, private val instrumented: File) {
    private val changedJars = mutableListOf<File>()
    val modifiedJars: List<File> get() = changedJars.toList()
    private val ownedSources = sources.mapTo(mutableSetOf()) { source ->
        val packageName = Regex("(?m)^\\s*package\\s+([\\w.]+)").find(source.readText())?.groupValues?.get(1)
        listOfNotNull(packageName?.replace('.', '/'), source.name).joinToString("/")
    }

    fun prepare(inputs: Collection<File>) {
        inputs.forEach { input ->
            if (input.isDirectory) {
                input.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { collect(it.readBytes()) }
            } else if (input.extension == "jar") {
                java.util.zip.ZipFile(input).use { jar ->
                    jar.entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".class") }.forEach { entry ->
                        jar.getInputStream(entry).buffered().use { collect(it.readBytes()) }
                    }
                }
            }
        }
        resetCoverageDirectory(instrumented)
        // The batch API, also used by Kover CLI, preserves classes intentionally filtered by the
        // engine. The 0.9.11 single-class Features wrapper incorrectly asserts a non-null result.
        KoverLegacyFeatures.instrument(instrumented, listOf(originals),
            ClassFilters(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet()), false)
    }

    fun copy(input: File, output: File) {
        output.parentFile.mkdirs()
        if (input.isDirectory) {
            input.walkTopDown().filter(File::isFile).forEach { file ->
                val relative = file.relativeTo(input).invariantSeparatorsPath
                output.resolve(relative).apply { parentFile.mkdirs() }.writeBytes(transform(relative, file.readBytes()))
            }
        } else if (input.extension == "jar") {
            copyJar(input, output)
        } else {
            input.copyTo(output, overwrite = true)
        }
    }

    private fun copyJar(input: File, output: File) {
        var changed = false
        ZipInputStream(input.inputStream().buffered()).use { source ->
            ZipOutputStream(output.outputStream().buffered()).use { target ->
                generateSequence { source.nextEntry }.forEach { entry ->
                    val signature = entry.name.startsWith("META-INF/") &&
                        entry.name.substringAfterLast('.').uppercase() in setOf("SF", "RSA", "DSA", "EC")
                    if (!entry.isDirectory && !signature) {
                        target.putNextEntry(ZipEntry(entry.name).apply { time = 0 })
                        val original = source.readBytes()
                        val bytes = transform(entry.name, original)
                        if (bytes !== original) changed = true
                        target.write(bytes)
                        target.closeEntry()
                    }
                    source.closeEntry()
                }
            }
        }
        if (changed) {
            changedJars.add(output)
        } else {
            // Unmodified providers may rely on their original JAR signatures and metadata.
            input.copyTo(output, overwrite = true)
        }
    }

    private fun collect(bytes: ByteArray) {
        val reader = ClassReader(bytes)
        if (reader.className.startsWith("com/storyteller_f/alphaheart/coverage/")) return
        var sourceName: String? = null
        reader.accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitSource(source: String?, debug: String?) { sourceName = source }
        }, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
        val key = listOf(reader.className.substringBeforeLast('/', ""), sourceName).filterNot { it.isNullOrEmpty() }
            .joinToString("/")
        if (sourceName == null || key !in ownedSources) return
        val original = originals.resolve("${reader.className}.class")
        check(!original.exists() || original.readBytes().contentEquals(bytes)) {
            "Different bytecode for ${reader.className}; coverage variants must remain separate"
        }
        original.parentFile.mkdirs()
        original.writeBytes(bytes)
    }

    private fun transform(name: String, bytes: ByteArray): ByteArray {
        if (!name.endsWith(".class")) return bytes
        val reader = ClassReader(bytes)
        val prepared = instrumented.resolve("${reader.className}.class")
        if (!prepared.isFile) return bytes
        val result = prepared.readBytes()
        if (result.contentEquals(bytes)) return bytes
        val writer = ClassWriter(0)
        ClassReader(result).accept(CounterStores(writer), 0)
        return writer.toByteArray()
    }
}

/** Replaces counter BASTORE with a snapshot-serialized store without changing frames or stack shape. */
private class CounterStores(writer: ClassWriter) : ClassVisitor(Opcodes.ASM9, writer) {
    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val target = super.visitMethod(access, name, descriptor, signature, exceptions)
        return object : MethodVisitor(Opcodes.ASM9, target) {
            override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.BASTORE) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "com/storyteller_f/alphaheart/coverage/CoverageRuntime",
                        "store", "(Ljava/lang/Object;II)V", false)
                } else {
                    super.visitInsn(opcode)
                }
            }
        }
    }
}

/** Cleanup is limited to task-owned descendants of a repository build directory. */
internal fun resetCoverageDirectory(directory: File) {
    val resolved = directory.canonicalFile
    val repository = generateSequence(resolved.parentFile) { it.parentFile }
        .firstOrNull { it.resolve("settings.gradle.kts").isFile }
    check(repository != null && resolved.relativeTo(repository).parentFile?.toPath()?.any { it.toString() == "build" } == true) {
        "Coverage cleanup must stay beneath a repository build directory"
    }
    check(!resolved.exists() || resolved.deleteRecursively()) { "Cannot clear coverage task output" }
    check(resolved.mkdirs() || resolved.isDirectory) { "Cannot create coverage task output" }
}
