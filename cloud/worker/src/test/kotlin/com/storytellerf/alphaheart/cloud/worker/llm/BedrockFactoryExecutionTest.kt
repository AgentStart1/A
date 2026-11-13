/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.smithy.kotlin.runtime.InternalApi
import aws.smithy.kotlin.runtime.http.HttpCall
import aws.smithy.kotlin.runtime.http.HttpStatusCode
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineBase
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngineConfig
import aws.smithy.kotlin.runtime.http.readAll
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.http.response.HttpResponse
import aws.smithy.kotlin.runtime.http.toHttpBody
import aws.smithy.kotlin.runtime.operation.ExecutionContext
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.parallel.ResourceLock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val BEDROCK_RESPONSE = """{
  "output":{"message":{"role":"assistant","content":[{"text":"test reply"}]}},
  "stopReason":"end_turn","usage":{"inputTokens":1,"outputTokens":1,"totalTokens":2},
  "metrics":{"latencyMs":1}
}"""

@ResourceLock("aws-system-properties")
internal class BedrockFactoryExecutionTest {
    @Test
    fun `explicit API key wins over AWS credentials`() =
        runTest {
            withContext(Dispatchers.IO) {
                withSyntheticAwsCredentials {
                    RecordingBedrockEngine().use { engine ->
                        val config = LlmConfig(LlmProvider.BEDROCK, apiKey = "test-bearer", region = "eu-west-1")
                        executeFactoryRequest(config, engine)
                        val request = engine.requests.single()
                        assertEquals("Bearer test-bearer", request.headers["Authorization"])
                        assertEquals("bedrock-runtime.eu-west-1.amazonaws.com", request.url.host.toString())
                    }
                }
            }
        }

    @Test
    fun `default chain signs with configured region`() =
        runTest {
            withContext(Dispatchers.IO) {
                withSyntheticAwsCredentials {
                    RecordingBedrockEngine().use { engine ->
                        val config = LlmConfig(LlmProvider.BEDROCK, region = "eu-central-1")
                        executeFactoryRequest(config, engine)
                        val request = engine.requests.single()
                        val authorization = assertNotNull(request.headers["Authorization"])
                        assertTrue(authorization.startsWith("AWS4-HMAC-SHA256 "))
                        assertTrue(authorization.contains("/eu-central-1/bedrock/aws4_request"))
                        assertEquals("bedrock-runtime.eu-central-1.amazonaws.com", request.url.host.toString())
                    }
                }
            }
        }

    @Test
    fun `default chain receives client region`() {
        for (region in listOf(null, "eu-west-1")) {
            val config = LlmConfig(LlmProvider.BEDROCK, region = region)
            KoogClientFactory.createClient(config).use { client ->
                val runtime = runtimeOf(assertIs<BedrockLLMClient>(client))
                // Unwrap SDK ownership only; never replace the production credential provider.
                val provider = runtime.config.credentialsProvider
                val delegate = provider.javaClass.getDeclaredField("delegate").apply { isAccessible = true }
                val chain = assertIs<DefaultChainCredentialsProvider>(delegate.get(provider))
                assertEquals(region ?: "us-west-2", runtime.config.region)
                assertEquals(runtime.config.region, chain.region)
            }
        }
    }

    @Test
    fun `closing factory clients releases owned resources`() =
        runTest {
            withSyntheticAwsCredentials {
                for (apiKey in listOf(null, "test-bearer")) {
                    val config = LlmConfig(LlmProvider.BEDROCK, apiKey = apiKey)
                    val client = assertIs<BedrockLLMClient>(KoogClientFactory.createClient(config))
                    val runtime = runtimeOf(client)
                    KoogLlmService(client, KoogClientFactory.resolveModel(config), config).use {
                        assertTrue(assertNotNull(runtime.config.httpClient.coroutineContext[Job]).isActive)
                        assertEquals("test-access-key", runtime.config.credentialsProvider.resolve().accessKeyId)
                    }
                    assertTrue(assertNotNull(runtime.config.httpClient.coroutineContext[Job]).isCompleted)
                    val failure =
                        assertFailsWith<IllegalStateException> { runtime.config.credentialsProvider.resolve() }
                    assertEquals("Credentials provider is closed", failure.message)
                }
            }
        }
}

private suspend fun executeFactoryRequest(config: LlmConfig, engine: RecordingBedrockEngine) {
    val client = KoogClientFactory.createClient(config, engine)
    KoogLlmService(client, KoogClientFactory.resolveModel(config), config).use { service ->
        val schema = LlmResponseSchema("decision", buildJsonObject { put("type", "object") })
        assertEquals("test reply", service.generateResponse("Classify", "System", schema))
    }
    val request = engine.requests.single()
    assertEquals("POST", request.method.name)
    assertTrue(request.url.path.toString().endsWith("/converse"))
    assertTrue(request.url.path.toString().contains("openai.gpt-oss-120b-1"))
    val body = Json.parseToJsonElement(assertNotNull(request.body.readAll()).decodeToString()).jsonObject
    assertNotNull(body["outputConfig"])
    val message = body.getValue("messages").jsonArray.single().jsonObject
    val content = message.getValue("content").jsonArray.single().jsonObject
    assertEquals("Classify", content.getValue("text").jsonPrimitive.content)
}

private fun runtimeOf(client: BedrockLLMClient): BedrockRuntimeClient {
    val field = client.javaClass.getDeclaredField("bedrockClient").apply { isAccessible = true }
    return assertIs<BedrockRuntimeClient>(field.get(client))
}

private suspend fun withSyntheticAwsCredentials(block: suspend () -> Unit) {
    val properties = mapOf("aws.accessKeyId" to "test-access-key", "aws.secretAccessKey" to "test-secret-key")
    val previous = properties.mapValues { (key, _) -> System.getProperty(key) }
    try {
        properties.forEach { (key, value) -> System.setProperty(key, value) }
        block()
    } finally {
        previous.forEach { (key, value) ->
            if (value == null) System.clearProperty(key) else System.setProperty(key, value)
        }
    }
}

@OptIn(InternalApi::class)
private class RecordingBedrockEngine : HttpClientEngineBase("bedrock-factory-test") {
    override val config = HttpClientEngineConfig.Default
    val requests = mutableListOf<HttpRequest>()

    override suspend fun roundTrip(context: ExecutionContext, request: HttpRequest): HttpCall {
        requests.add(request)
        val response = HttpResponse(HttpStatusCode.OK, body = BEDROCK_RESPONSE.toHttpBody())
        return HttpCall(request, response)
    }
}
