/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.io.File

private const val CLI_READY_PORT = 8081
private const val SERVER_PORT = 8811
private const val WS_PORT = 8813

/** Host endpoints and base services, valid only within [useBackendFixture]. */
@ConsistentCopyVisibility
data class BackendFixture internal constructor(
    val services: BaseServiceFixture,
    val host: String,
    val serverPort: Int,
    val wsPort: Int,
)

/**
 * Starts the full backend, copying presets through the Docker API for remote Docker support.
 * [environment] may customize public origins; internal service connections come from the base fixture.
 * Callers own platform forwarding and must finish using endpoints before the block returns.
 */
suspend fun <T> useBackendFixture(
    logDirectory: File,
    environment: (BaseServiceFixture) -> Map<String, String> = { emptyMap() },
    block: suspend (BackendFixture) -> T,
): T =
    withFixtureResources(logDirectory) { resources ->
        val services = startBaseServices(resources)
        val commonEnv = backendEnvironment() + services.containerEnvironment + environment(services)
        resources.start("cli", createCli(services.network, commonEnv))
        val ws = resources.start("ws", createWs(services.network, commonEnv))
        val server = resources.start("server", createServer(services.network, commonEnv))
        resources.start("worker", createWorker(services.network, commonEnv))
        block(BackendFixture(services, server.host, server.getMappedPort(SERVER_PORT), ws.getMappedPort(WS_PORT)))
    }

/** Finds repository inputs independently of the test module's working directory. */
fun resolveTestPresetPath(): File = resolveRepositoryFile("dev/preset/0_preset_user.json").parentFile

private fun resolveRepositoryFile(path: String): File =
    generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
        .map { File(it, path) }.firstOrNull(File::isFile)
        ?: error("Shared test input not found: $path")

private fun backendEnvironment(): Map<String, String> {
    val file = resolveRepositoryFile("cloud/server/src/test/resources/test.env")
    val env =
        file.readLines().asSequence().map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line -> line.substringBefore('=') to line.substringAfter('=', "") }
    return env - "LOG_PATH" +
        mapOf(
            "BUILD_TYPE" to "test",
            "FLAVOR" to "dev",
            "SERVER_PORT" to SERVER_PORT.toString(),
            "WS_SERVER_PORT" to WS_PORT.toString(),
            "SERVER_URL" to "http://10.0.2.2:$SERVER_PORT",
            "WS_SERVER_URL" to "ws://10.0.2.2:$WS_PORT",
            "WS_RPC_URL" to "ws://test-ws:$WS_PORT/rpc",
            "SESSION_SECRET" to "e2e-session-secret",
            "INIT_ENABLE" to "false",
        )
}

private fun createCli(network: Network, env: Map<String, String>): GenericContainer<*> =
    FixtureContainer(DockerImageName.parse("a-cli:${if (E2eCoverage.enabled) "e2e-coverage" else "latest"}")).apply {
        withNetwork(network)
        withEnv(env + mapOf("CLI_INIT_ENABLE" to "true", "CLI_READY_PORT" to CLI_READY_PORT.toString()))
        withCopyFileToContainer(MountableFile.forHostPath(resolveTestPresetPath().toPath()), "/app/deploy/preset_data")
        withExposedPorts(CLI_READY_PORT)
        waitingFor(Wait.forHttp("/").forPort(CLI_READY_PORT).forStatusCode(200).withStartupTimeout(STARTUP_TIMEOUT))
    }

private fun createWs(network: Network, env: Map<String, String>): GenericContainer<*> =
    FixtureContainer(DockerImageName.parse("a-ws:${if (E2eCoverage.enabled) "e2e-coverage" else "latest"}")).apply {
        withNetwork(network)
        withNetworkAliases("test-ws")
        withEnv(env)
        withExposedPorts(WS_PORT)
        waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP_TIMEOUT))
    }

private fun createServer(network: Network, env: Map<String, String>): GenericContainer<*> =
    FixtureContainer(DockerImageName.parse("a-server:${if (E2eCoverage.enabled) "e2e-coverage" else "latest"}")).apply {
        withNetwork(network)
        withEnv(env)
        withExposedPorts(SERVER_PORT)
        waitingFor(Wait.forHttp("/metrics").forPort(SERVER_PORT).forStatusCode(200).withStartupTimeout(STARTUP_TIMEOUT))
    }

private fun createWorker(network: Network, env: Map<String, String>): GenericContainer<*> =
    FixtureContainer(DockerImageName.parse("a-worker:${if (E2eCoverage.enabled) "e2e-coverage" else "latest"}"))
        .withNetwork(network).withEnv(env)
