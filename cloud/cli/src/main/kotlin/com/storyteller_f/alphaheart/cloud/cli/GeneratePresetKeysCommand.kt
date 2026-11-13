/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.Type2Algo
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import io.github.aakira.napier.Napier
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import java.io.BufferedOutputStream
import java.io.File

private val presetKeyNames =
    listOf(
        "font-provider",
        "robot1",
        "robot2",
        "system",
        "user1",
        "user2",
        "user3",
    )

private const val DEFAULT_PRESET_KEYS_OUTPUT_DIR = "deploy/dev-data/secrets"

@OptIn(ExperimentalCli::class)
class GeneratePresetKeysCommand :
    Subcommand(
        "generate-preset-keys",
        "generate Dilithium preset user private keys",
    ) {
    private val outputDirPath by option(
        ArgType.String,
        fullName = "output-dir",
        shortName = "o",
        description = "directory to write preset secret files",
    ).default(DEFAULT_PRESET_KEYS_OUTPUT_DIR)

    private val isOverwriteEnabled by option(
        ArgType.Boolean,
        fullName = "overwrite",
        shortName = "f",
        description = "overwrite existing key files",
    ).default(false)

    override fun execute() {
        loadCryptoLibIfNeed()
        runBlocking {
            val outputDir = resolvePresetKeysOutputDir(outputDirPath)
            val generatedFiles = generatePresetDilithiumKeys(outputDir, isOverwriteEnabled)
            Napier.i(tag = "cli") {
                "generated ${generatedFiles.size} preset key files in ${outputDir.canonicalPath}"
            }
        }
    }
}

internal suspend fun generatePresetDilithiumKeys(outputDir: File, overwrite: Boolean): List<File> {
    outputDir.mkdirs()
    require(outputDir.isDirectory) {
        "${outputDir.canonicalPath} is not a directory"
    }

    val signAlgo = getAlgo(AlgoType.DILITHIUM)
    val encryptionAlgo = signAlgo.encryptionAlgo as Type2Algo
    return presetKeyNames.flatMap { name ->
        val privateKeyFile = File(outputDir, "p-$name")
        val encryptionPrivateKeyFile = File(outputDir, "ep-$name")
        checkCanWrite(privateKeyFile, overwrite)
        checkCanWrite(encryptionPrivateKeyFile, overwrite)

        val privateKey = signAlgo.generatePemKeyPair().getOrThrow().first
        val encryptionPrivateKey =
            encryptionAlgo.generateEncryptionPemKeyPair().getOrThrow().first
        writeKeyFile(privateKeyFile, privateKey)
        writeKeyFile(encryptionPrivateKeyFile, encryptionPrivateKey)
        listOf(privateKeyFile, encryptionPrivateKeyFile)
    }
}

private fun checkCanWrite(file: File, overwrite: Boolean) {
    require(overwrite || !file.exists()) {
        "${file.canonicalPath} already exists. Use --overwrite to replace it."
    }
}

private fun writeKeyFile(file: File, content: String) {
    file.parentFile?.mkdirs()
    BufferedOutputStream(file.outputStream()).use { output ->
        output.write(content.replace("\r\n", "\n").encodeToByteArray())
    }
}

internal fun resolvePresetKeysOutputDir(outputDirPath: String): File {
    val outputDir = File(outputDirPath)
    if (outputDir.isAbsolute || outputDirPath != DEFAULT_PRESET_KEYS_OUTPUT_DIR) {
        return outputDir
    }
    return generateSequence(File("").absoluteFile) { it.parentFile }
        .map { File(it, outputDirPath) }
        .firstOrNull { File(it.parentFile, "0_preset_user.json").exists() }
        ?: outputDir
}
