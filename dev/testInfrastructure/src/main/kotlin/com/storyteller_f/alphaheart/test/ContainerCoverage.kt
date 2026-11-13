/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import org.testcontainers.containers.GenericContainer
import java.io.File

internal fun collectContainerCoverage(name: String, container: GenericContainer<*>, directory: File) {
    if (!E2eCoverage.enabled || name in setOf("postgresql", "redis")) return
    val output = directory.resolve("coverage/backend/$name")
    E2eCoverage.collect(output) {
        val script =
            checkNotNull(
                E2eCoverage::class.java.getResource("/coverage/request-container-snapshot.sh"),
            ).readText()
        val response = container.execInContainer("sh", "-c", script)
        check(
            response.exitCode == 0,
        ) { "Container coverage snapshot failed: exit=${response.exitCode}; ${response.stderr}" }
        val processes =
            response.stdout.lineSequence()
                .map(String::trim).filter(String::isNotEmpty).toList()
        check(processes.isNotEmpty()) { "Container has no JVM coverage snapshots" }
        processes.forEach { pid ->
            check(pid.all(Char::isDigit)) { "Invalid coverage process identifier" }
            val destination = output.resolve(pid).also { it.mkdirs() }.resolve("report.ic")
            container.copyFileFromContainer("/home/app/logs/e2e-coverage/$pid/report.ic") { input ->
                input.buffered().use { source ->
                    destination.outputStream().buffered().use { target -> source.copyTo(target) }
                }
            }
            check(destination.length() > 0) { "Container exported an empty coverage binary" }
            val descriptor = destination.parentFile.resolve("classes.properties")
            container.copyFileFromContainer("/home/app/logs/e2e-coverage/$pid/classes.properties") { input ->
                input.buffered().use { source ->
                    descriptor.outputStream().buffered().use { target -> source.copyTo(target) }
                }
            }
            val module = if (name in setOf("filesystem", "lucene")) "$name-service" else name
            E2eCoverage.copyDescriptor(descriptor, destination.parentFile, ":cloud:$module")
        }
    }
}
