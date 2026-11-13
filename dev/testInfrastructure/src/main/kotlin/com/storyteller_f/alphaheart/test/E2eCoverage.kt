/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties
import kotlin.time.Duration.Companion.seconds

/** Test-process control only; applications contain the runtime solely in coverage distributions. */
object E2eCoverage {
    val enabled: Boolean get() = System.getProperty("e2e.coverage") == "true"

    /** Select the active process after restart; previously exported process snapshots remain in [output]. */
    suspend fun collectJvm(directory: File, output: File, classSet: String, processIds: Set<Long>? = null) {
        if (!enabled) return
        withContext(NonCancellable + Dispatchers.IO) {
            collect(output) {
                val processes =
                    directory.listFiles().orEmpty().filter {
                        it.isDirectory && it.name.all(Char::isDigit) &&
                            (processIds == null || it.name.toLongOrNull() in processIds)
                    }
                check(processes.isNotEmpty()) { "No instrumented JVM registered coverage control" }
                processes.forEach { process ->
                    check(!process.resolve("error").isFile) { process.resolve("error").readText() }
                    // Control markers also work when an environment provider shares the fixture filesystem.
                    val alive = process.resolve("running").isFile
                    if (alive) {
                        process.resolve("complete").delete()
                        val request = process.resolve("request.pending")
                        request.writeText("snapshot")
                        Files.move(
                            request.toPath(),
                            process.resolve("request").toPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING,
                        )
                    }
                    val completed =
                        withTimeoutOrNull(15.seconds) {
                            while (!process.resolve("complete").isFile) {
                                check(!process.resolve("error").isFile) { process.resolve("error").readText() }
                                check(alive) { "JVM exited without a coverage snapshot" }
                                delay(25)
                            }
                            true
                        }
                    check(completed == true) { "JVM coverage snapshot timed out" }
                    check(!process.resolve("error").isFile) { process.resolve("error").readText() }
                    val destination = output.resolve(process.name).also { it.mkdirs() }
                    copyReport(process.resolve("report.ic"), destination.resolve("report.ic"))
                    copyDescriptor(process.resolve("classes.properties"), destination, classSet)
                }
            }
        }
    }

    /** A completed snapshot and a nonempty file are required; missing data is never valid zero coverage. */
    fun copyReport(source: File, destination: File) {
        check(source.isFile && source.length() > 0) { "Missing or empty coverage binary" }
        destination.parentFile.mkdirs()
        source.copyTo(destination, overwrite = true)
    }

    fun copyDescriptor(source: File, directory: File, classSet: String) {
        val properties = Properties().apply { source.inputStream().buffered().use(::load) }
        check(properties.getProperty("classSet") == classSet) { "Coverage belongs to a different application variant" }
        check(
            properties.getProperty("buildId").orEmpty().matches(Regex("[0-9a-f]{64}")),
        ) { "Missing coverage build identity" }
        if (source.canonicalFile != directory.resolve("classes.properties").canonicalFile) {
            source.copyTo(directory.resolve("classes.properties"), overwrite = true)
        }
    }

    /** Writes an explicit status even when collectors are invoked while preserving a business failure. */
    inline fun <T> collect(directory: File, block: () -> T): T {
        directory.mkdirs()
        directory.resolve("status").writeText("incomplete")
        return try {
            block().also { directory.resolve("status").writeText("complete") }
        } catch (failure: Exception) {
            directory.resolve("collection-error.txt").writeText(failure.stackTraceToString())
            throw failure
        }
    }
}
