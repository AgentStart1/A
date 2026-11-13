/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val GOOGLE_TEST_RESPONSE = "Native Google response"

internal class GoogleLlmExecutionTest {
    @Test
    fun `Google executes native content requests`() =
        runTest {
            withGoogleService { service, requests ->
                assertEquals(GOOGLE_TEST_RESPONSE, service.generateResponse("User message", "System instruction", null))

                val request = requests.receive()
                assertEquals("POST", request.method)
                assertEquals("/v1beta/models/gemini-2.5-flash:generateContent", request.path)
                assertEquals("key=test-key", request.query)
                assertNull(request.authorization)
                val content =
                    request.body.getValue("contents")
                        .jsonArray.single().jsonObject
                assertEquals("user", content.getValue("role").jsonPrimitive.content)
                assertEquals("User message", content.googleText())
                assertEquals("System instruction", request.body.getValue("systemInstruction").jsonObject.googleText())
                val generation = request.body.getValue("generationConfig").jsonObject
                assertEquals("0.7", generation.getValue("temperature").jsonPrimitive.content)
                assertEquals("1024", generation.getValue("maxOutputTokens").jsonPrimitive.content)
                assertNull(generation["responseJsonSchema"])
            }
        }

    @Test
    fun `Google sends native standard JSON schema`() =
        runTest {
            val schema =
                buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject { put("is_harmful", buildJsonObject { put("type", "boolean") }) })
                }
            withGoogleService { service, requests ->
                assertEquals(
                    GOOGLE_TEST_RESPONSE,
                    service.generateResponse("Classify topic", null, LlmResponseSchema("decision", schema)),
                )

                val body = requests.receive().body
                val generation = body.getValue("generationConfig").jsonObject
                assertEquals("application/json", generation.getValue("responseMimeType").jsonPrimitive.content)
                assertEquals(schema, generation.getValue("responseJsonSchema"))
                assertNull(body["response_format"])
                assertNull(body["messages"])
            }
        }
}

private suspend fun withGoogleService(block: suspend (KoogLlmService, Channel<CapturedLlmRequest>) -> Unit) =
    withContext(Dispatchers.IO) {
        LlmHttpTestServer(buildGoogleResponse().toString()).use { server ->
            val config =
                LlmConfig(
                    provider = LlmProvider.GOOGLE,
                    apiKey = "test-key",
                    baseUrl = server.baseUrl,
                )
            KoogLlmService.create(config).use { service -> block(service, server.requests) }
        }
    }

private fun JsonObject.googleText(): String =
    getValue("parts").jsonArray.single().jsonObject
        .getValue("text").jsonPrimitive.content

private fun buildGoogleResponse(): JsonObject =
    buildJsonObject {
        put(
            "candidates",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put(
                            "content",
                            buildJsonObject {
                                put("role", "model")
                                put(
                                    "parts",
                                    buildJsonArray {
                                        add(buildJsonObject { put("text", GOOGLE_TEST_RESPONSE) })
                                    },
                                )
                            },
                        )
                        put("finishReason", "STOP")
                        put("index", 0)
                    },
                )
            },
        )
    }
