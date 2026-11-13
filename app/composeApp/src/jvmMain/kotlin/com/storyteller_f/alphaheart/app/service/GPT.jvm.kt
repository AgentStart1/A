/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.service

import com.google.ai.edge.litertlm.Backend.GPU
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.storyteller_f.alphaheart.app.error.ModelGenerationError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.mapError
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.utils.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import java.io.File

actual fun buildGPT(): GPT = JvmEdgeGPT()

actual fun getGPTModelDirectory(): Path {
    val userHome = System.getProperty("user.home")
    return Path(File(userHome, ".storyteller_f_a/llm").absolutePath)
}

class JvmEdgeGPT : GPT {
    override val supportList: List<String> = listOf("litertlm")

    override suspend fun importModel(file: PlatformFile): CustomResult<GPTModel, ModelGenerationError> =
        captureFailure(ModelGenerationError::from) {
            val name = file.file.name
            require(supportList.any { name.endsWith(it, ignoreCase = true) }) {
                "unsupported model file: $name"
            }
            val target = Path(getGPTModelDirectory(), name)
            withContext(Dispatchers.IO) {
                val targetFile = target.toFile()
                targetFile.parentFile?.mkdirs()
                file.file.copyTo(targetFile, overwrite = true)
            }
            GPTModel(target.name, target.toString())
        }.mapError(ModelGenerationError::fromError)

    override suspend fun generate(path: String, prompt: String): CustomResult<Flow<GPTOutput>, ModelGenerationError> {
        val file = File(path)
        if (!file.exists()) {
            return CustomResult.Failure(
                ModelGenerationError.from(Exception("modal not exists")),
            ).mapError(ModelGenerationError::fromError)
        }
        val userHome = System.getProperty("user.home")
        val engineConfig =
            EngineConfig(
                modelPath = path,
                backend = GPU(),
                cacheDir = File(userHome, ".storyteller_f_a/llm-cache").absolutePath,
            )
        val engine = Engine(engineConfig)
        return captureFailure(ModelGenerationError::from) {
            withContext(Dispatchers.IO) {
                engine.initialize()
            }
            Napier.i(tag = "gpt") {
                "engine initialized"
            }
            val conversation = engine.createConversation()
            conversation.sendMessageAsync(prompt).map {
                Napier.d(tag = "gpt") {
                    "gpt output ${it.contents}"
                }
                GPTOutput(it.contents.contents.joinToString())
            }.onCompletion {
                engine.close()
                Napier.i(tag = "gpt") {
                    "engine closed"
                }
            }
        }.mapError(ModelGenerationError::fromError)
    }

    override fun models(scope: CoroutineScope): Flow<List<GPTModel>> =
        observeModels(
            scope,
            getGPTModelDirectory(),
            supportList,
        )
}
