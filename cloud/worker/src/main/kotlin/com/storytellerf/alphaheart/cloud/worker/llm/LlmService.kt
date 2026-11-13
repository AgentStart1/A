/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.prompt.dsl.emptyPrompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import com.storyteller_f.shared.model.LlmConfig
import com.storyteller_f.shared.model.LlmProvider
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

private const val LOG_PREVIEW_LENGTH = 100

internal interface LlmService : AutoCloseable {
    /**
     * Generate a response from the LLM.
     *
     * @param prompt The user prompt to send to the LLM
     * @param systemPrompt Optional system prompt to set the context
     * @param responseSchema Optional JSON schema that constrains the model response
     * @return The generated response
     */
    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String? = null,
        responseSchema: LlmResponseSchema? = null,
    ): String

    override fun close() {}
}

internal data class LlmResponseSchema(val name: String, val schema: JsonObject)

internal class KoogLlmService(
    private val client: LLMClient,
    private val model: LLModel,
    private val config: LlmConfig,
) : LlmService {
    override suspend fun generateResponse(
        prompt: String,
        systemPrompt: String?,
        responseSchema: LlmResponseSchema?,
    ): String {
        val response =
            try {
                Napier.d(tag = "llm") {
                    "Generating response for prompt: ${prompt.take(LOG_PREVIEW_LENGTH)}..."
                }

                val effectiveSystemPrompt = resolveSystemPrompt(systemPrompt, responseSchema)
                val promptObj =
                    if (effectiveSystemPrompt != null) {
                        prompt(existing = emptyPrompt()) {
                            system(effectiveSystemPrompt)
                            user(prompt)
                        }
                    } else {
                        prompt(existing = emptyPrompt()) {
                            user(prompt)
                        }
                    }

                val result =
                    client.execute(
                        promptObj.withParams(
                            LLMParams(
                                temperature = config.temperature,
                                maxTokens = config.maxTokens,
                                schema =
                                responseSchema?.let { configuredSchema ->
                                    LLMParams.Schema.JSON.Standard(
                                        name = configuredSchema.name,
                                        schema = configuredSchema.schema,
                                    )
                                },
                            ),
                        ),
                        model,
                    )
                val text = result.textContent()

                Napier.d(tag = "llm") {
                    "Generated response: ${text.take(LOG_PREVIEW_LENGTH)}..."
                }

                text
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalStateException) {
                Napier.e(tag = "llm", throwable = exception) {
                    "LLM generation failed"
                }
                throw exception
            }
        return response
    }

    private fun resolveSystemPrompt(systemPrompt: String?, schema: LlmResponseSchema?): String? =
        if (config.provider == LlmProvider.DEEPSEEK && schema != null) {
            // Koog's DeepSeek adapter sends JSON-object mode without the schema, so the model needs it in context.
            listOfNotNull(systemPrompt, "Respond with JSON matching this schema: ${schema.schema}").joinToString("\n")
        } else {
            systemPrompt
        }

    override fun close() {
        Napier.d(tag = "llm") {
            "Closing Koog LLM service"
        }
        client.close()
    }

    companion object {
        /**
         * Create a KoogLlmService from LLM configuration.
         */
        fun create(config: LlmConfig): KoogLlmService {
            val model = KoogClientFactory.resolveModel(config)
            val client = KoogClientFactory.createClient(config)
            return KoogLlmService(client, model, config)
        }
    }
}
