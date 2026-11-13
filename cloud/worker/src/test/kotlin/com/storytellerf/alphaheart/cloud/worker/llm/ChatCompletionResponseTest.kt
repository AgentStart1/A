/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChatCompletionResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun reportUpstreamError() {
        for (status in listOf(200, 403, 404, 429, 502)) {
            val failure =
                assertFailsWith<IllegalStateException> {
                    decodeChatCompletionResponse(
                        status,
                        """{"error":{"code":403,"message":"Content rejected","metadata":{"provider_name":"test"}}}""",
                        json,
                    )
                }
            assertTrue(failure.message.orEmpty().contains("HTTP $status"))
            assertTrue(failure.message.orEmpty().contains("Content rejected"))
        }
    }

    @Test
    fun rejectNonJsonResponse() {
        val failure =
            assertFailsWith<IllegalStateException> {
                decodeChatCompletionResponse(502, "<html>Bad gateway</html>", json)
            }
        assertTrue(failure.message.orEmpty().contains("HTTP 502"))
    }

    @Test
    fun rejectFailedStatusWithSuccessBody() {
        assertFailsWith<IllegalStateException> {
            decodeChatCompletionResponse(500, """{"choices":[]}""", json)
        }
    }

    @Test
    fun decodeSuccessfulResponse() {
        val response =
            decodeChatCompletionResponse(
                200,
                """{"choices":[{"message":{"role":"assistant","content":"safe"}}],"usage":{}}""",
                json,
            )
        assertEquals("safe", response)
    }

    @Test
    fun rejectEmptyChoices() {
        val failure =
            assertFailsWith<IllegalStateException> {
                decodeChatCompletionResponse(200, """{"choices":[]}""", json)
            }
        assertTrue(failure.message.orEmpty().contains("No response content"))
    }

    @Test
    fun rejectMalformedSuccessResponse() {
        for (body in listOf("{}", "null", "[]")) {
            val failure =
                assertFailsWith<IllegalStateException> {
                    decodeChatCompletionResponse(200, body, json)
                }
            assertTrue(failure.message.orEmpty().contains("invalid chat completion response"))
        }
    }
}
