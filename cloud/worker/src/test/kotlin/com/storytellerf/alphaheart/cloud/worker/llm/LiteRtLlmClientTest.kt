/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import ai.koog.prompt.dsl.emptyPrompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import com.google.ai.edge.litertlm.Backend
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class LiteRtLlmClientTest {
    @Test
    fun `standard JSON schema uses native response format`() {
        val schema =
            buildJsonObject {
                put("type", "object")
            }
        val prompt =
            buildPrompt().withParams(
                LLMParams(
                    schema =
                    LLMParams.Schema.JSON.Standard(
                        name = "decision",
                        schema = schema,
                    ),
                ),
            )

        val responseFormat = assertNotNull(prompt.liteRtResponseFormat())

        assertEquals(schema.toString(), responseFormat.schemaOrPattern)
    }

    @Test
    fun `response format is absent without schema`() {
        assertNull(buildPrompt().liteRtResponseFormat())
    }

    @Test
    fun `sampler uses request temperature`() {
        val samplerConfig =
            buildPrompt()
                .withParams(LLMParams(temperature = 0.25))
                .liteRtSamplerConfig()

        assertEquals(0.25, samplerConfig.temperature)
    }

    @Test
    fun `system and user messages are extracted`() {
        val prompt = buildPrompt()

        assertEquals("Classify content", prompt.systemInstruction())
        assertEquals("ordinary topic", prompt.singleUserMessage())
    }

    @Test
    fun `multiple user messages are rejected`() {
        val prompt =
            prompt(existing = buildPrompt()) {
                user("another topic")
            }

        assertFailsWith<IllegalArgumentException> {
            prompt.singleUserMessage()
        }
    }

    @Test
    fun `GPU backend is preferred`() {
        val attemptedBackends = mutableListOf<Backend>()
        val selectedBackend =
            createWithPreferredLiteRtBackend { backend ->
                attemptedBackends += backend
                backend
            }

        assertIs<Backend.GPU>(selectedBackend)
        assertEquals(1, attemptedBackends.size)
    }

    @Test
    fun `CPU backend follows GPU failure`() {
        val attemptedBackends = mutableListOf<Backend>()
        val selectedBackend =
            createWithPreferredLiteRtBackend { backend ->
                attemptedBackends += backend
                if (backend is Backend.GPU) {
                    error("GPU unavailable")
                }
                backend
            }

        assertEquals(2, attemptedBackends.size)
        assertIs<Backend.GPU>(attemptedBackends[0])
        assertIs<Backend.CPU>(attemptedBackends[1])
        assertIs<Backend.CPU>(selectedBackend)
    }

    @Test
    fun `configured cache precedes backend selection`() {
        withTemporaryDirectory { temporaryDirectory ->
            val configuredCacheDirectory = temporaryDirectory.resolve("configured-cache")
            val selectedCacheDirectory =
                createWithLiteRtCacheAndPreferredBackend(
                    modelPath = temporaryDirectory.resolve("model.litertlm"),
                    configuredCacheDirectory = configuredCacheDirectory,
                    temporaryDirectory = temporaryDirectory.resolve("default-cache"),
                ) { cacheDirectory, backend ->
                    assertIs<Backend.GPU>(backend)
                    assertTrue(Files.isDirectory(cacheDirectory))
                    cacheDirectory
                }

            assertEquals(configuredCacheDirectory.toAbsolutePath(), selectedCacheDirectory)
        }
    }

    @Test
    fun `default cache directory is isolated by model`() {
        val temporaryDirectory = Path.of("temporary-root")
        val firstModelCache =
            resolveLiteRtCacheDirectory(
                modelPath = Path.of("models", "first.litertlm"),
                configuredCacheDirectory = null,
                temporaryDirectory = temporaryDirectory,
            )
        val secondModelCache =
            resolveLiteRtCacheDirectory(
                modelPath = Path.of("models", "second.litertlm"),
                configuredCacheDirectory = null,
                temporaryDirectory = temporaryDirectory,
            )

        val expectedParent =
            temporaryDirectory
                .toAbsolutePath()
                .normalize()
                .resolve("a-worker")
                .resolve("litertlm")
        assertEquals(expectedParent, firstModelCache.parent)
        assertEquals(64, firstModelCache.fileName.toString().length)
        assertNotEquals(firstModelCache, secondModelCache)
    }

    @Test
    fun `cache failure precedes backend selection`() {
        withTemporaryDirectory { temporaryDirectory ->
            val invalidCacheDirectory = Files.createFile(temporaryDirectory.resolve("cache-file"))
            val attemptedBackends = mutableListOf<Backend>()

            val failure =
                assertFailsWith<IllegalStateException> {
                    createWithLiteRtCacheAndPreferredBackend(
                        modelPath = temporaryDirectory.resolve("model.litertlm"),
                        configuredCacheDirectory = invalidCacheDirectory,
                        temporaryDirectory = temporaryDirectory,
                    ) { _, backend ->
                        attemptedBackends += backend
                    }
                }

            assertTrue(failure.message.orEmpty().startsWith("Unable to prepare LiteRT cache directory:"))
            assertTrue(attemptedBackends.isEmpty())
        }
    }

    private fun buildPrompt() =
        prompt(existing = emptyPrompt()) {
            system("Classify content")
            user("ordinary topic")
        }

    private fun withTemporaryDirectory(block: (Path) -> Unit) {
        val temporaryDirectory = createTempDirectory(prefix = "litert-cache-test-")
        try {
            block(temporaryDirectory)
        } finally {
            check(temporaryDirectory.toFile().deleteRecursively()) {
                "Failed to delete LiteRT cache test directory"
            }
        }
    }
}
