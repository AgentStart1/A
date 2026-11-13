/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.params.LLMParams
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val TAG = "OpenAICompatibleClient"
private const val LOG_PREVIEW_LENGTH = 100

/**
 * Creates an LLMClient that directly calls OpenAI-compatible APIs.
 * This bypasses koog's parameter determination logic for non-standard model names.
 */
internal fun createOpenAICompatibleClient(apiKey: String, baseUrl: String): LLMClient {
    val httpClient = HttpClient(OkHttp)
    val jsonSerializer =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    return object : LLMClient() {
        @OptIn(ExperimentalTime::class)
        override suspend fun execute(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
            tools: List<ai.koog.agents.core.tools.ToolDescriptor>,
        ): Message.Assistant {
            val systemPrompt =
                prompt.messages
                    .filterIsInstance<Message.System>()
                    .joinToString("\n") { it.textContent() }

            val userPrompt =
                prompt.messages
                    .filterIsInstance<Message.User>()
                    .joinToString("\n") { it.textContent() }

            Napier.d(tag = TAG) {
                "Calling OpenAI-compatible API with model: ${model.id}"
            }

            val request =
                ChatCompletionRequest(
                    model = model.id,
                    messages =
                    buildList {
                        if (systemPrompt.isNotBlank()) {
                            add(ChatMessage(role = "system", content = systemPrompt))
                        }
                        add(ChatMessage(role = "user", content = userPrompt))
                    },
                    temperature = prompt.params.temperature,
                    maxTokens = prompt.params.maxTokens,
                    responseFormat = prompt.params.schema.toOpenAIResponseFormat(),
                )

            val requestBody = jsonSerializer.encodeToString(request)

            val response =
                httpClient.post("$baseUrl/chat/completions") {
                    header("Authorization", "Bearer $apiKey")
                    header("HTTP-Referer", "https://github.com/storyteller")
                    header("X-Title", "StoryTeller Worker")
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

            val content = decodeChatCompletionResponse(response.status.value, response.body<String>(), jsonSerializer)

            Napier.d(tag = TAG) {
                "LLM response: ${content.take(LOG_PREVIEW_LENGTH)}..."
            }

            return Message.Assistant(
                content = content,
                metaInfo =
                ResponseMetaInfo(
                    timestamp = Clock.System.now(),
                    modelId = model.id,
                ),
            )
        }

        override suspend fun moderate(
            prompt: ai.koog.prompt.Prompt,
            model: LLModel,
        ): ai.koog.prompt.dsl.ModerationResult =
            throw UnsupportedOperationException(
                "Moderation not supported by OpenAI-compatible client",
            )

        override fun llmProvider(): ai.koog.prompt.llm.LLMProvider = ai.koog.prompt.llm.LLMProvider.OpenAI

        override fun close() {
            httpClient.close()
        }
    }
}

@Serializable
private data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double?,
    @SerialName("max_tokens")
    val maxTokens: Int?,
    @SerialName("response_format")
    val responseFormat: JsonObject?,
)

internal fun LLMParams.Schema?.toOpenAIResponseFormat(): JsonObject? {
    val jsonSchema = this as? LLMParams.Schema.JSON ?: return null
    return buildJsonObject {
        put("type", "json_schema")
        put(
            "json_schema",
            buildJsonObject {
                put("name", jsonSchema.name)
                put("strict", JsonPrimitive(true))
                put("schema", jsonSchema.schema)
            },
        )
    }
}

@Serializable
private data class ChatMessage(val role: String, val content: String)

@Serializable
private data class ChatCompletionResponse(val choices: List<Choice>)

@Serializable
private data class Choice(val message: ChatMessage)

internal fun decodeChatCompletionResponse(status: Int, body: String, json: Json): String {
    val envelope =
        try {
            json.parseToJsonElement(body) as? JsonObject
        } catch (exception: SerializationException) {
            throw IllegalStateException("LLM request failed (HTTP $status): invalid JSON response", exception)
        }
    val errorElement = envelope?.get("error")
    check(status in 200..299 && (errorElement == null || errorElement == JsonNull)) {
        val error = errorElement as? JsonObject
        val code = (error?.get("code") as? JsonPrimitive)?.content
        val message = (error?.get("message") as? JsonPrimitive)?.content
        "LLM request failed (HTTP $status, code ${code ?: "unknown"}): ${message ?: "upstream error"}"
    }
    val response =
        try {
            json.decodeFromString<ChatCompletionResponse>(body)
        } catch (exception: SerializationException) {
            throw IllegalStateException(
                "LLM request failed (HTTP $status): invalid chat completion response",
                exception,
            )
        }
    return checkNotNull(
        response.choices.firstOrNull(),
    ) { "No response content from LLM (HTTP $status)" }.message.content
}
