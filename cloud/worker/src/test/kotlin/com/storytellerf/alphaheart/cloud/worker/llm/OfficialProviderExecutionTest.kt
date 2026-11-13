/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.bedrock.BedrockAPIMethod
import ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import aws.smithy.kotlin.runtime.net.url.Url
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val CHAT_RESPONSE = """{
  "id":"test-id","object":"chat.completion","created":0,"model":"test-model","system_fingerprint":"test",
  "choices":[{"index":0,"message":{"role":"assistant","content":"test reply"},"finish_reason":"stop"}],
  "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}
}"""
private const val ROUTER_MODEL = "dots-studio/dots-3-note-preview:free"

internal class OfficialProviderExecutionTest {
    @Test
    fun `compatible provider preserves chat JSON`() =
        runTest {
            val request = executeChatProvider(LlmProvider.OPENAI_COMPATIBLE, "/chat/completions")
            assertEquals("gpt-3.5-turbo", request.body.getValue("model").jsonPrimitive.content)
            val messages = request.body.getValue("messages").jsonArray.map { it.jsonObject }
            assertEquals(listOf("system", "user"), messages.map { it.getValue("role").jsonPrimitive.content })
            assertEquals(
                listOf("System instruction", "Classify"),
                messages.map { it.getValue("content").jsonPrimitive.content },
            )
            assertChatSchema(request)
        }

    @Test
    fun `DeepSeek executes native chat requests`() =
        runTest {
            val request = executeChatProvider(LlmProvider.DEEPSEEK, "/chat/completions")
            val format = request.body.getValue("response_format").jsonObject
            assertEquals("json_object", format.getValue("type").jsonPrimitive.content)
            val instruction =
                request.body.getValue("messages")
                    .jsonArray.first().jsonObject
                    .getValue("content").jsonPrimitive.content
            assertTrue(instruction.contains("System instruction"))
            assertTrue(instruction.contains(testResponseSchema().schema.toString()))
        }

    @Test
    fun `Mistral executes native chat requests`() =
        runTest {
            assertChatSchema(executeChatProvider(LlmProvider.MISTRAL_AI, "/v1/chat/completions"))
        }

    @Test
    fun `DashScope executes native chat requests`() =
        runTest {
            assertChatSchema(executeChatProvider(LlmProvider.DASHSCOPE, "/compatible-mode/v1/chat/completions"))
        }

    @Test
    fun `OpenRouter executes unlisted IDs with schemas`() =
        runTest {
            withContext(Dispatchers.IO) {
                LlmHttpTestServer(CHAT_RESPONSE).use { server ->
                    val config = LlmConfig(LlmProvider.OPENROUTER, apiKey = "test-key", model = ROUTER_MODEL)
                    val client =
                        OpenRouterLLMClient(
                            apiKey = "test-key",
                            settings = OpenRouterClientSettings(baseUrl = server.baseUrl),
                            httpClientFactory = KtorKoogHttpClient.Factory(),
                        )
                    KoogLlmService(client, KoogClientFactory.resolveModel(config), config).use { service ->
                        assertEquals(
                            "test reply",
                            service.generateResponse("Classify", "System instruction", testResponseSchema()),
                        )
                    }
                    val request = server.requests.receive()
                    assertEquals("/api/v1/chat/completions", request.path)
                    assertEquals(ROUTER_MODEL, request.body.getValue("model").jsonPrimitive.content)
                    assertChatSchema(request)
                }
            }
        }

    @Test
    fun `Bedrock executes signed native Converse requests`() =
        runTest {
            withContext(Dispatchers.IO) {
                val response = """{
              "output":{"message":{"role":"assistant","content":[{"text":"test reply"}]}},
              "stopReason":"end_turn","usage":{"inputTokens":1,"outputTokens":1,"totalTokens":2},
              "metrics":{"latencyMs":1}
            }"""
                LlmHttpTestServer(response).use { server ->
                    val config = LlmConfig(LlmProvider.BEDROCK, region = "us-west-2")
                    val runtime =
                        BedrockRuntimeClient {
                            region = config.region
                            endpointUrl = Url.parse(server.baseUrl)
                            credentialsProvider =
                                StaticCredentialsProvider {
                                    accessKeyId = "test-access-key"
                                    secretAccessKey = "test-secret-key"
                                }
                        }
                    val client = BedrockLLMClient(runtime, apiMethod = BedrockAPIMethod.Converse)
                    KoogLlmService(client, KoogClientFactory.resolveModel(config), config).use { service ->
                        assertEquals("test reply", service.generateResponse("Classify", "System", testResponseSchema()))
                    }
                    val request = server.requests.receive()
                    assertEquals("POST", request.method)
                    assertTrue(request.path.endsWith("/converse"))
                    assertTrue(request.path.contains("openai.gpt-oss-120b-1:0"))
                    assertTrue(assertNotNull(request.authorization).startsWith("AWS4-HMAC-SHA256 "))
                    assertNotNull(request.body["outputConfig"])
                    val message = request.body.getValue("messages").jsonArray.single()
                    val messageContent = message.jsonObject.getValue("content").jsonArray
                    val textPart = messageContent.single().jsonObject
                    assertEquals("Classify", textPart.getValue("text").jsonPrimitive.content)
                }
            }
        }
}

private suspend fun executeChatProvider(provider: LlmProvider, path: String) =
    withContext(Dispatchers.IO) {
        LlmHttpTestServer(CHAT_RESPONSE).use { server ->
            val config = LlmConfig(provider, apiKey = "test-key", baseUrl = server.baseUrl)
            KoogLlmService.create(config).use { service ->
                assertEquals(
                    "test reply",
                    service.generateResponse("Classify", "System instruction", testResponseSchema()),
                )
            }
            val request = server.requests.receive()
            assertEquals("POST", request.method)
            assertEquals(path, request.path)
            assertEquals("Bearer test-key", request.authorization)
            request
        }
    }

private fun testResponseSchema(): LlmResponseSchema =
    LlmResponseSchema(
        "decision",
        buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject { put("is_harmful", buildJsonObject { put("type", "boolean") }) })
        },
    )

private fun assertChatSchema(request: CapturedLlmRequest) {
    val format = request.body.getValue("response_format").jsonObject
    assertEquals("json_schema", format.getValue("type").jsonPrimitive.content)
    assertEquals(testResponseSchema().schema, format.getValue("json_schema").jsonObject.getValue("schema"))
}
