/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.alphaheart.backend.exposed.buildExposedDatabase
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun buildMemoryDatabaseConnection(): DatabaseConnection {
    val uuid = Uuid.random().toHexString()
    val h2File = File("./build/test/session/$uuid/h2/default")
    h2File.parentFile!!.let {
        if (!it.exists() && !it.mkdirs()) {
            error("mkdirs failed ${it.canonicalPath}")
        }
    }
    val url = "r2dbc:h2:file:///${h2File.path.replace("\\", "/")}"
    return DatabaseConnection(url, "h2", "sa", "")
}

fun useElasticSearchContainer(block: (MergedEnv) -> Unit) {
    System.setProperty("api.version", "1.44")
    ElasticsearchContainer(ContainerImages.ELASTICSEARCH)
        .withEnv("xpack.security.transport.ssl.enabled", "false")
        .withEnv("xpack.security.http.ssl.enabled", "false")
        .use { container ->
            container.start()
            val env =
                MergedEnv(
                    listOf(
                        mapOf(
                            "SEARCH_SERVICE" to "elastic",
                            "ELASTIC_NAME" to "elastic",
                            "ELASTIC_PASSWORD" to "changeme",
                            "ELASTIC_URL" to "http://${container.httpHostAddress}",
                            "ELASTIC_CERT_FILE" to "",
                            "BUILD_TYPE" to "test",
                        ),
                    ),
                )
            block(env)
        }
}

fun useMinioContainer(block: (MergedEnv) -> Unit) {
    MinIOContainer(ContainerImages.MINIO)
        .use { container ->
            container.start()
            val env =
                MergedEnv(
                    listOf(
                        mapOf(
                            "MEDIA_SERVICE" to "minio",
                            "MINIO_URL" to container.s3URL,
                            "MINIO_NAME" to container.userName,
                            "MINIO_PASS" to container.password,
                        ),
                    ),
                )
            block(env)
        }
}

fun usePostgresContainer(block: (DatabaseConnection) -> Unit) {
    System.setProperty("api.version", "1.44")
    PostgreSQLContainer(ContainerImages.POSTGRESQL)
        .use { container ->
            container.start()
            val connection =
                DatabaseConnection(
                    container.jdbcUrl.replace("jdbc", "r2dbc"),
                    "postgresql",
                    container.username,
                    container.password,
                )
            block(connection)
        }
}

fun testSearchRpc(block: suspend (TopicSearchService) -> Unit) {
    System.setProperty("api.version", "1.44")
    GenericContainer(DockerImageName.parse("a-lucene:latest")).apply {
        withTmpFs(mapOf("/data" to "rw,uid=1000,gid=1000"))
        withExposedPorts(8821)
        waitingFor(Wait.forHttp("/health").forPort(8821).withStartupTimeout(Duration.ofSeconds(90)))
    }.use { container ->
        container.start()
        val env =
            MergedEnv(
                listOf(
                    mapOf(
                        "SEARCH_SERVICE" to "lucene",
                        "LUCENE_RPC_URL" to "ws://${container.host}:${container.getMappedPort(8821)}/rpc",
                        "BUILD_TYPE" to "test",
                    ),
                ),
            )
        runBlocking { block(buildTopicSearchService(env)) }
    }
}

fun testSearchContainer(block: suspend (TopicSearchService) -> Unit) {
    useElasticSearchContainer { env ->
        val service = buildTopicSearchService(env)
        runBlocking { block(service) }
    }
}

fun testOssRpc(block: suspend (ObjectStorageService) -> Unit) {
    System.setProperty("api.version", "1.44")
    GenericContainer(DockerImageName.parse("a-filesystem:latest")).apply {
        withEnv("FILESYSTEM_PUBLIC_URL", "http://filesystem:8822")
        withTmpFs(mapOf("/data" to "rw,uid=1000,gid=1000"))
        withExposedPorts(8820, 8822)
        waitingFor(Wait.forHttp("/health").forPort(8820).withStartupTimeout(Duration.ofSeconds(90)))
    }.use { container ->
        container.start()
        val env =
            MergedEnv(
                listOf(
                    mapOf(
                        "MEDIA_SERVICE" to "filesystem",
                        "FILESYSTEM_RPC_URL" to "ws://${container.host}:${container.getMappedPort(8820)}/rpc",
                        "FILESYSTEM_PUBLIC_URL" to "http://${container.host}:${container.getMappedPort(8822)}",
                    ),
                ),
            )
        runBlocking { block(mediaService(env)) }
    }
}

fun testOssContainer(block: suspend (ObjectStorageService) -> Unit) {
    useMinioContainer { env ->
        val service = mediaService(env)
        runBlocking { block(service) }
    }
}

private fun withDatabase(connection: DatabaseConnection, block: suspend (CombinedDatabase) -> Unit) {
    val db = buildExposedDatabase(connection)
    runBlocking {
        db.init()
        try {
            block(db)
        } finally {
            db.clean()
        }
    }
}

fun testDatabaseMemory(block: suspend (CombinedDatabase) -> Unit) {
    val connection = buildMemoryDatabaseConnection()
    withDatabase(connection, block)
}

fun testDatabaseContainer(block: suspend (CombinedDatabase) -> Unit) {
    usePostgresContainer { connection ->
        withDatabase(connection, block)
    }
}
