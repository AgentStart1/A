/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.service.benchmark

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.ElasticConnection
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.elastic.ElasticTopicSearchService
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import org.testcontainers.elasticsearch.ElasticsearchContainer

@State(Scope.Benchmark)
open class IndexBenchmark {
    private lateinit var elasticClient: ElasticsearchContainer
    private lateinit var service: ElasticTopicSearchService

    @Setup(Level.Trial)
    fun setUp() {
        SnowflakeFactory.setMachine(0)
        elasticClient =
            ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.17.0").apply {
                withEnv("xpack.security.transport.ssl.enabled", "false")
                withEnv("xpack.security.http.ssl.enabled", "false")
                start() // 只启动一次
            }

        val connection =
            ElasticConnection(
                "http://${elasticClient.httpHostAddress}",
                "",
                "elastic",
                "changeme",
            )
        service = ElasticTopicSearchService(connection)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        elasticClient.stop() // 只停止一次
    }

    @Benchmark
    fun saveDocumentBenchmark(bh: Blackhole) =
        runBlocking {
            val topics =
                listOf(
                    TopicDocument(
                        SnowflakeFactory.nextId(),
                        "test",
                        SnowflakeFactory.nextId(),
                        "ROOM",
                        SnowflakeFactory.nextId(),
                        "ROOM",
                        SnowflakeFactory.nextId(),
                    ),
                )
            bh.consume(service.saveDocument(topics))
        }
}
