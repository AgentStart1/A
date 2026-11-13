/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.elastic

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch._types.ElasticsearchException
import co.elastic.clients.elasticsearch._types.Refresh
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.TransportUtils
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.storyteller_f.alphaheart.backend.core.ElasticConnection
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.splitKeywords
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.recover
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.type.PrimaryKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.apache.hc.core5.http.ConnectionClosedException
import org.apache.hc.core5.http.ContentType
import java.io.File
import java.io.FileInputStream
import java.net.ConnectException
import javax.net.ssl.SSLContext

open class Elastic(val connection: ElasticConnection) {
    val sslContext = getSslContext(connection)

    suspend fun <T> useElasticClient(block: suspend ElasticsearchAsyncClient.() -> T): CustomResult<T, SearchError> {
        val elasticConnection = connection
        val point = Exception("Elasticsearch request call site")
        return captureFailure(SearchError::from) {
            ElasticsearchAsyncClient.of { b ->
                b.host(elasticConnection.url)
                    .usernameAndPassword(elasticConnection.name, elasticConnection.pass)
                    .sslContext(sslContext)
                    .transportOptions {
                        it.addHeader("Accept", ContentType.APPLICATION_JSON.mimeType)
                            .addHeader("Content-Type", ContentType.APPLICATION_JSON.mimeType)
                    }
                    .jsonMapper(
                        JacksonJsonpMapper().apply {
                            objectMapper().registerKotlinModule()
                        },
                    )
            }.use {
                withContext(Dispatchers.IO) {
                    it.block()
                }
            }
        }.recover(SearchError::fromError) { failure ->
            val e = failure.cause
            if (e is ElasticsearchException) {
                Napier.e {
                    "elastic search failed ${e.status()}"
                }
            }
            CustomResult.Failure(
                SearchError.from(
                    when (e) {
                        is ConnectException,
                        is ConnectionClosedException,
                        ->
                            Exception("elastic service unavailable", e)

                        is ElasticsearchException if e.status() == 404 ->
                            Exception("index not found", e)

                        else -> {
                            point.initCause(e)
                            point
                        }
                    },
                ),
            )
        }
    }
}

suspend fun ElasticsearchAsyncClient.cleanAll(indexName: String) {
    if (indices().exists {
            it.index(indexName)
        }.await().value()
    ) {
        val response =
            indices().delete {
                it.index(indexName)
            }.await()
        Napier.i {
            "elastic clean done $response"
        }
    } else {
        Napier.i {
            "elastic index not exists"
        }
    }
}

suspend fun <T> ElasticsearchAsyncClient.getDocumentList(
    idList: List<PrimaryKey>,
    indexName: String,
    clazz: Class<T>,
): List<T?> =
    try {
        if (idList.size == 1) {
            idList.map { id ->
                get({
                    it.index(indexName).id(id.toString())
                }, clazz).await().source()
            }
        } else {
            mget({ builder ->
                builder.index(indexName).ids(idList.map { it.toString() })
            }, clazz).await().docs().map {
                it.result().source()
            }
        }
    } catch (exception: ElasticsearchException) {
        if (exception.status() != 404) throw exception
        idList.map { null }
    }

suspend fun <T : PrimaryKeyIdentifiable> ElasticsearchAsyncClient.saveDocumentList(
    connection: ElasticConnection,
    documents: List<T>,
    indexName: String,
) {
    if (documents.size == 1) {
        val topic = documents.first()
        val response =
            index {
                it.index(indexName).id(topic.id.toString()).document(topic)
                    .refresh(if (connection.refresh) Refresh.WaitFor else Refresh.False)
            }.await()
        Napier.i(tag = "elastic save") {
            response.toString()
        }
    } else {
        val response =
            bulk { bulkRequest ->
                bulkRequest.operations(
                    documents.map { document ->
                        BulkOperation.of { op ->
                            op.index(
                                IndexOperation.Builder<T>()
                                    .index(indexName)
                                    .id(document.id.toString()) // 指定文档 ID
                                    .document(document)
                                    .build(),
                            )
                        }
                    },
                ).refresh(Refresh.WaitFor)
            }.await()
        Napier.i(tag = "elastic save bulk") {
            "errors:${response.errors()} took: ${response.took()}ms"
        }
    }
}

fun getSslContext(elasticConnection: ElasticConnection): SSLContext? {
    val certFile = elasticConnection.certFile
    Napier.i(message = "cert path ${File(certFile).canonicalPath}")
    return if (certFile.isNotBlank()) {
        FileInputStream(certFile).use {
            TransportUtils.sslContextFromHttpCaCrt(it)
        }
    } else {
        null
    }
}

suspend fun <T> ElasticsearchAsyncClient.searchDocumentList(
    request: SearchRequest?,
    clazz: Class<T>,
): PaginationResult<T> =
    try {
        val response = search(request, clazz).await()
        val hits = response.hits()
        val total = hits.total()?.value()
        Napier.i {
            "size: ${hits.hits().size} maxScore: ${hits.maxScore() ?: "<none>"} total: ${total ?: "<none>"}"
        }
        PaginationResult(
            hits.hits().mapNotNull {
                it.source()
            },
            total ?: 0,
        )
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        Napier.e(e) {
            "elastic search failed"
        }
        PaginationResult(emptyList(), 0)
    }

fun <T> buildElasticSearchService(env: MergedEnv, b: (ElasticConnection) -> T): T {
    val certFile = env["ELASTIC_CERT_FILE"] ?: error("ELASTIC_CERT_FILE is empty")
    // need replace home dir
    val safeCertFile =
        when {
            certFile.isBlank() -> {
                ""
            }

            certFile.startsWith("~") -> {
                val homeDir = System.getProperty("user.home")
                File(certFile.replace("~", homeDir)).canonicalPath
            }

            else -> {
                File(certFile).canonicalPath
            }
        }

    val url = env["ELASTIC_URL"] ?: error("ELASTIC_URL is empty")
    val name = env["ELASTIC_NAME"] ?: error("ELASTIC_NAME is empty")
    val pass = env["ELASTIC_PASSWORD"] ?: error("ELASTIC_PASSWORD is empty")
    val connection = ElasticConnection(url, safeCertFile, name, pass, env["BUILD_TYPE"] == "test")
    return b(connection)
}

fun BoolQuery.Builder.prioritizedFields(keyword: String, aidField: String, nameField: String): BoolQuery.Builder {
    val keywords = splitKeywords(keyword)
    if (keywords != null) {
        // 多关键字模式：仅包含匹配
        keywords.forEachIndexed { index, kw ->
            val positionBoost = (keywords.size - index).toFloat()
            must { m ->
                m.bool { innerBool ->
                    innerBool
                        .should { s -> s.wildcard { it.field(aidField).value("*$kw*").boost(positionBoost * 10f) } }
                        .should { s -> s.wildcard { it.field(nameField).value("*$kw*").boost(positionBoost) } }
                }
            }
        }
        return this
    } else {
        // 单关键字模式：保持原有逻辑
        // 设置 minimumShouldMatch(1) 确保至少一个 should 必须匹配（否则有 filter 时 should 变成可选的）
        return should { s ->
            s.matchPhrasePrefix { it.field(aidField).query(keyword).boost(1000f) }
        }.should { s ->
            s.matchPhrasePrefix { it.field(nameField).query(keyword).boost(100f) }
        }.should { s ->
            s.wildcard { it.field(aidField).value("*$keyword*").boost(10f) }
        }.should { s ->
            s.wildcard { it.field(nameField).value("*$keyword*").boost(1f) }
        }.minimumShouldMatch("1")
    }
}

fun BoolQuery.Builder.prioritizedField(keyword: String, field: String): BoolQuery.Builder {
    val keywords = splitKeywords(keyword)
    if (keywords != null) {
        // 多关键字模式：仅包含匹配
        keywords.forEachIndexed { index, kw ->
            val positionBoost = (keywords.size - index).toFloat()
            must { m ->
                m.wildcard { it.field(field).value("*$kw*").boost(positionBoost) }
            }
        }
        return this
    } else {
        // 单关键字模式：前缀匹配 + 包含匹配
        // 设置 minimumShouldMatch(1) 确保至少一个 should 必须匹配（否则有 filter 时 should 变成可选的）
        return should { s ->
            s.matchPhrasePrefix { it.field(field).query(keyword).boost(10f) }
        }.should { s ->
            s.wildcard { it.field(field).value("*$keyword*").boost(1f) }
        }.minimumShouldMatch("1")
    }
}
