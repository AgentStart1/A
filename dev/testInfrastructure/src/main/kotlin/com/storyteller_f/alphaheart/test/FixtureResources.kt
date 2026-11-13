/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.testcontainers.containers.GenericContainer
import java.io.File

/** Owns resources from allocation, including partially started containers. Logs precede removal. */
internal class FixtureResources(private val logDirectory: File) : AutoCloseable {
    private val resources = mutableListOf<AutoCloseable>()
    private val collectionErrors = mutableListOf<Throwable>()

    fun <T : AutoCloseable> own(resource: T): T {
        resources.add(resource)
        return resource
    }

    suspend fun <T : GenericContainer<*>> start(name: String, container: T): T {
        own(container)
        if (E2eCoverage.enabled && name !in setOf("postgresql", "redis")) {
            container.withEnv("E2E_COVERAGE_DIRECTORY", "/home/app/logs/e2e-coverage")
        }
        if (container is FixtureLogSource) {
            container.exportBeforeStop = {
                collectFailure(collectionErrors) {
                    collectContainerCoverage(name, container, logDirectory.parentFile)
                }
                collectFailure(collectionErrors) { exportLogs(name, container) }
            }
        }
        // Testcontainers is blocking. Register ownership before crossing a cancellable dispatcher boundary.
        withContext(Dispatchers.IO) { container.start() }
        currentCoroutineContext().ensureActive()
        return container
    }

    override fun close() {
        // All cleanup is blocking Java IO: no suspension point can interrupt the reverse-order release.
        val errors = mutableListOf<Throwable>()
        resources.asReversed().forEach { resource -> collectFailure(errors) { resource.close() } }
        errors.addAll(collectionErrors)
        if (errors.isNotEmpty()) {
            val first = errors.first()
            errors.drop(1).forEach(first::addSuppressed)
            throw first
        }
    }

    private fun exportLogs(name: String, container: GenericContainer<*>) {
        if (container.containerId == null) return
        check(logDirectory.isDirectory || logDirectory.mkdirs()) { "Cannot create fixture log directory" }
        val errors = mutableListOf<Throwable>()
        collectFailure(errors) { logDirectory.resolve("$name.log").writeText(container.logs) }
        if (name !in setOf("postgresql", "redis")) {
            collectFailure(errors) {
                container.dockerClient.copyArchiveFromContainerCmd(container.containerId, "/home/app/logs/$name")
                    .exec().buffered().use { input ->
                        logDirectory.resolve(
                            "$name.tar",
                        ).outputStream().buffered().use { output -> input.copyTo(output) }
                    }
            }
        }
        errors.forEachIndexed { index, error ->
            logDirectory.resolve("$name-$index.error.txt").writeText(error.stackTraceToString())
        }
        if (errors.isNotEmpty()) throw errors.first()
    }
}

private inline fun collectFailure(errors: MutableList<Throwable>, block: () -> Unit) {
    try {
        block()
    } catch (failure: Exception) {
        errors.add(failure)
    }
}

internal suspend fun <T> withFixtureResources(directory: File, block: suspend (FixtureResources) -> T): T =
    withContext(Dispatchers.IO) {
        System.setProperty("api.version", "1.44")
        // Kotlin use preserves the original failure, including cancellation, and suppresses close failures.
        FixtureResources(directory).use { resources -> block(resources) }
    }
