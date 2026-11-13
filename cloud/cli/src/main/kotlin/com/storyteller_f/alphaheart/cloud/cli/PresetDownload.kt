/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private class DownloadProgress(private val totalSize: Long?, private var currentSize: Long) {
    private var lastLogTime = 0L
    private var lastLogPercent = -DOWNLOAD_PROGRESS_PERCENT_STEP

    fun add(bytes: Int) {
        currentSize += bytes
        log()
    }

    fun log(force: Boolean = false) {
        val now = System.currentTimeMillis()
        val percent = totalSize?.takeIf { it > 0 }?.let { (currentSize * 100 / it).toInt() }
        val percentProgressed = percent != null && percent >= lastLogPercent + DOWNLOAD_PROGRESS_PERCENT_STEP
        if (!force && now - lastLogTime < DOWNLOAD_PROGRESS_LOG_INTERVAL_MS && !percentProgressed) return

        Napier.i {
            buildString {
                append("download progress ")
                append(formatDownloadSize(currentSize))
                if (totalSize != null) {
                    append("/")
                    append(formatDownloadSize(totalSize))
                }
                if (percent != null) {
                    append(" (")
                    append(percent.coerceAtMost(100))
                    append("%)")
                }
            }
        }
        lastLogTime = now
        if (percent != null) {
            lastLogPercent = percent
        }
    }
}

internal data class DownloadConfig(
    val name: String,
    val link: String,
    val hash: String,
    val excludeArchiveEntries: List<String> = emptyList(),
    val includeArchiveEntries: List<String> = emptyList(),
    val sourceName: String? = null,
)

private val yamlMapper: ObjectMapper =
    ObjectMapper(YAMLFactory())
        .registerKotlinModule()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

private const val DOWNLOAD_PROGRESS_LOG_INTERVAL_MS = 1_000L
private const val DOWNLOAD_PROGRESS_PERCENT_STEP = 5
private const val HTTP_STATUS_PARTIAL_CONTENT = 206

suspend fun downloadWithResume(url: String, file: File, client: HttpClient) {
    val downloadedSize = if (file.exists()) file.length() else 0L

    val response: HttpResponse =
        client.get(url) {
            if (downloadedSize > 0) {
                header("Range", "bytes=$downloadedSize-")
            }
        }

    val status = response.status.value
    Napier.i {
        "download status $status"
    }

    val body = response.bodyAsChannel()
    val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
    val totalSize =
        contentLength?.let {
            if (status == HTTP_STATUS_PARTIAL_CONTENT) downloadedSize + it else it
        }
    val progress = DownloadProgress(totalSize, downloadedSize)

    file.parentFile!!.mkdirs()

    RandomAccessFile(file, "rw").use { raf ->
        raf.seek(downloadedSize)
        progress.log(force = true)
        while (!body.isClosedForRead) {
            val packet = body.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.exhausted()) {
                val bytes = packet.readByteArray()
                raf.write(bytes)
                progress.add(bytes.size)
            }
        }
        progress.log(force = true)
    }
}

private fun formatDownloadSize(size: Long): String =
    when {
        size < 1024L -> "${size}B"
        size < 1024L * 1024L -> "${size / 1024L}KiB"
        size < 1024L * 1024L * 1024L -> "${size / (1024L * 1024L)}MiB"
        else -> "${size / (1024L * 1024L * 1024L)}GiB"
    }

internal fun isDownloadConfigPath(path: String): Boolean =
    path.endsWith(
        "download",
    ) || path.endsWith(".download.yaml") || path.endsWith(".download.yml")

internal fun parseDownloadConfig(path: File): DownloadConfig? {
    val text = path.readText()
    return try {
        yamlMapper.readValue(text, DownloadConfig::class.java)
    } catch (expected: Exception) {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.size >= 3) {
            DownloadConfig(name = lines[0], link = lines[1], hash = lines[2])
        } else {
            null
        }
    }
}

internal suspend fun downloadPresetFileIfNeed(path: String, parentDir: File?, client: HttpClient): File? =
    if (isDownloadConfigPath(path)) {
        val configFile = File(parentDir, path)
        val config = parseDownloadConfig(configFile)
        if (config != null) {
            val realPath = File(parentDir, "download/${config.sourceName ?: config.name}")
            if (realPath.exists()) {
                if (config.hash.startsWith("sha256:")) {
                    val calculatedSha = sha256File(realPath)
                    val hashValue = config.hash.removePrefix("sha256:")
                    Napier.i {
                        "calculated ${config.name} sha $calculatedSha, real $hashValue"
                    }
                    if (calculatedSha != hashValue) {
                        downloadWithResume(config.link, realPath, client)
                    }
                }
            } else {
                downloadWithResume(config.link, realPath, client)
            }

            createProcessedPresetFileIfNeeded(realPath, config)
        } else {
            null
        }
    } else {
        File(parentDir, path)
    }

private fun createProcessedPresetFileIfNeeded(downloadedFile: File, config: DownloadConfig): File {
    if (config.excludeArchiveEntries.isEmpty() && config.includeArchiveEntries.isEmpty()) {
        return downloadedFile
    }
    if (!downloadedFile.name.endsWith(".zip", ignoreCase = true)) {
        return downloadedFile
    }
    val processedFile = File(downloadedFile.parentFile, "processed/${config.name}")
    processedFile.parentFile?.mkdirs()
    downloadedFile.copyTo(processedFile, overwrite = true)
    return repackArchiveWithExclusionsAndInclusionsInPlace(
        processedFile,
        config.excludeArchiveEntries,
        config.includeArchiveEntries,
    )
}

private fun shouldExcludeEntry(relativePath: String, excludeGlobs: List<String>): Boolean {
    if (excludeGlobs.isEmpty()) return false
    val normalized = relativePath.replace('\\', '/')
    return excludeGlobs.any { pattern ->
        val matcher = FileSystems.getDefault().getPathMatcher("glob:${pattern.trim()}")
        matcher.matches(Paths.get(normalized))
    }
}

private fun shouldIncludeEntry(relativePath: String, includeGlobs: List<String>): Boolean {
    if (includeGlobs.isEmpty()) return true
    val normalized = relativePath.replace('\\', '/')
    return includeGlobs.any { pattern ->
        val matcher = FileSystems.getDefault().getPathMatcher("glob:${pattern.trim()}")
        matcher.matches(Paths.get(normalized))
    }
}

private fun unzipToDirectory(zipFile: File, outputDir: File) {
    ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
        while (true) {
            val entry = zis.nextEntry ?: break
            val target = File(outputDir, entry.name)
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                BufferedOutputStream(target.outputStream()).use { output ->
                    zis.copyTo(output)
                }
            }
            zis.closeEntry()
        }
    }
}

private fun zipDirectoryWithFilter(
    sourceDir: File,
    targetZip: File,
    excludeGlobs: List<String>,
    includeGlobs: List<String>,
) {
    ZipOutputStream(BufferedOutputStream(targetZip.outputStream())).use { zos ->
        sourceDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = sourceDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            if (shouldExcludeEntry(relative, excludeGlobs)) {
                return@forEach
            }
            if (!shouldIncludeEntry(relative, includeGlobs)) {
                return@forEach
            }
            zos.putNextEntry(ZipEntry(relative))
            BufferedInputStream(file.inputStream()).use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }
}

internal fun repackArchiveWithExclusionsAndInclusionsInPlace(
    zipFile: File,
    excludeGlobs: List<String>,
    includeGlobs: List<String>,
): File {
    val hasNoFilters = excludeGlobs.isEmpty() && includeGlobs.isEmpty()
    if (hasNoFilters || !zipFile.name.endsWith(".zip", ignoreCase = true)) {
        return zipFile
    }
    val parent = zipFile.parentFile ?: return zipFile
    val extractDir = File(parent, ".${zipFile.name}.extract-${System.currentTimeMillis()}")
    val tempZip = File(parent, ".${zipFile.name}.filtered.tmp")
    try {
        unzipToDirectory(zipFile, extractDir)
        zipDirectoryWithFilter(extractDir, tempZip, excludeGlobs, includeGlobs)
        check(zipFile.delete()) {
            "failed to delete original archive ${zipFile.absolutePath}"
        }
        check(tempZip.renameTo(zipFile)) {
            "failed to replace archive ${zipFile.absolutePath}"
        }
        return zipFile
    } finally {
        extractDir.deleteRecursively()
        if (tempZip.exists()) {
            tempZip.delete()
        }
    }
}

fun sha256File(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    BufferedInputStream(file.inputStream()).use { fis ->
        val buffer = ByteArray(8192)
        var read = fis.read(buffer)
        while (read != -1) {
            digest.update(buffer, 0, read)
            read = fis.read(buffer)
        }
    }
    return digest.digest().joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
}
