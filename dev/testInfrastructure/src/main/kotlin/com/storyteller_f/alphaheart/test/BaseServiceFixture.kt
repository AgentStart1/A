/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.time.Duration

internal const val FILESYSTEM_PORT = 8820
internal const val FILESYSTEM_HTTP_PORT = 8822
internal const val LUCENE_PORT = 8821
internal val STARTUP_TIMEOUT: Duration = Duration.ofSeconds(90)

/** Separate host and Docker-network connection settings for one isolated topology. */
@ConsistentCopyVisibility
data class BaseServiceFixture internal constructor(
    internal val network: Network,
    val hostEnvironment: Map<String, String>,
    val containerEnvironment: Map<String, String>,
    val filesystemHttpPort: Int,
    val redis: GenericContainer<*>,
    val postgres: PostgreSQLContainer<*>,
)

/** Allocates a fresh network and data directories; exports logs even on failure or cancellation. */
suspend fun <T> useBaseServiceFixture(logDirectory: File, block: suspend (BaseServiceFixture) -> T): T =
    withFixtureResources(logDirectory) { resources -> block(startBaseServices(resources)) }

internal suspend fun startBaseServices(resources: FixtureResources): BaseServiceFixture {
    val network = resources.own(Network.newNetwork())
    val postgres = resources.start("postgresql", createPostgres(network))
    val filesystem = resources.start("filesystem", createFilesystem(network))
    val lucene = resources.start("lucene", createLucene(network))
    val redis =
        resources.start(
            "redis",
            FixtureContainer(DockerImageName.parse("redis:7.4-alpine"))
                .withNetwork(network).withNetworkAliases("signin-redis").withExposedPorts(6379),
        )
    val common =
        mapOf(
            "DATABASE_DRIVER" to "postgresql",
            "DATABASE_USER" to postgres.username,
            "DATABASE_PASS" to postgres.password,
            "MEDIA_SERVICE" to "filesystem",
            "SEARCH_SERVICE" to "lucene",
            "FILESYSTEM_PUBLIC_URL" to "http://${filesystem.host}:${filesystem.getMappedPort(FILESYSTEM_HTTP_PORT)}",
        )
    val host =
        common +
            mapOf(
                "DATABASE_URI" to
                    "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}",
                "FILESYSTEM_RPC_URL" to "ws://${filesystem.host}:${filesystem.getMappedPort(FILESYSTEM_PORT)}/rpc",
                "LUCENE_RPC_URL" to "ws://${lucene.host}:${lucene.getMappedPort(LUCENE_PORT)}/rpc",
                "REDIS_ENDPOINT" to "${redis.host}:${redis.getMappedPort(6379)}",
                "SIGNIN_REDIS_ENDPOINT" to "${redis.host}:${redis.getMappedPort(6379)}",
            )
    val internal =
        common +
            mapOf(
                "DATABASE_URI" to "r2dbc:postgresql://test-postgresql:5432/${postgres.databaseName}",
                "FILESYSTEM_RPC_URL" to "ws://test-filesystem:$FILESYSTEM_PORT/rpc",
                "LUCENE_RPC_URL" to "ws://test-lucene:$LUCENE_PORT/rpc",
                "REDIS_ENDPOINT" to "signin-redis:6379",
                "SIGNIN_REDIS_ENDPOINT" to "signin-redis:6379",
            )
    return BaseServiceFixture(network, host, internal, filesystem.getMappedPort(FILESYSTEM_HTTP_PORT), redis, postgres)
}

private fun createPostgres(network: Network): PostgreSQLContainer<*> =
    FixturePostgres().apply {
        withNetwork(network)
        withNetworkAliases("test-postgresql")
        withTmpFs(mapOf("/var/lib/postgresql/data" to "rw"))
    }

private fun createFilesystem(network: Network): GenericContainer<*> =
    FixtureContainer(
        DockerImageName.parse("a-filesystem:${if (E2eCoverage.enabled) "e2e-coverage" else "latest"}"),
    ).apply {
        withNetwork(network)
        withNetworkAliases("test-filesystem")
        withEnv("FILESYSTEM_PUBLIC_URL", "http://test-filesystem:$FILESYSTEM_HTTP_PORT")
        withTmpFs(mapOf("/data" to "rw,uid=1000,gid=1000"))
        withExposedPorts(FILESYSTEM_PORT, FILESYSTEM_HTTP_PORT)
        waitingFor(Wait.forHttp("/health").forPort(FILESYSTEM_PORT).withStartupTimeout(STARTUP_TIMEOUT))
    }

private fun createLucene(network: Network): GenericContainer<*> =
    FixtureContainer(DockerImageName.parse("a-lucene:${if (E2eCoverage.enabled) "e2e-coverage" else "latest"}")).apply {
        withNetwork(network)
        withNetworkAliases("test-lucene")
        withTmpFs(mapOf("/data" to "rw,uid=1000,gid=1000"))
        withExposedPorts(LUCENE_PORT)
        waitingFor(Wait.forHttp("/health").forPort(LUCENE_PORT).withStartupTimeout(STARTUP_TIMEOUT))
    }
