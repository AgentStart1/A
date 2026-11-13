/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.fold
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.services.filesystem.api.CopyPack
import com.storyteller_f.services.filesystem.api.ObjectStorageRecord
import com.storyteller_f.services.filesystem.api.ObjectStorageWriteRecord
import io.mikael.urlbuilder.UrlBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.pathString
import kotlin.io.path.visitFileTree
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

internal class LocalFileSystemObjectStorageService(private val url: String, base: Path) {
    private val base =
        if (!base.exists()) {
            base.createDirectories()
        } else {
            base.toRealPath()
        }

    init {
        LoggerFactory.getLogger(LocalFileSystemObjectStorageService::class.java)
            .info("Filesystem object storage initialized")
    }

    suspend fun upload(
        bucketName: String,
        uploadPacks: List<UploadPack>,
    ): CustomResult<List<ObjectStorageWriteRecord>, FilesystemError> =
        useFileSystem {
            val bucketPath = resolveBucket(bucketName)
            uploadPacks.map { uploadPack ->
                val target = resolveObject(bucketPath, uploadPack.fullName).createParentDirectories()
                Files.copy(uploadPack.file.toPath(), target, StandardCopyOption.REPLACE_EXISTING)
                ObjectStorageWriteRecord(uploadPack.fullName)
            }
        }

    @OptIn(ExperimentalTime::class)
    suspend fun get(bucketName: String, names: List<String>): CustomResult<List<ObjectStorageRecord>, FilesystemError> =
        useFileSystem {
            names.mapNotNull { name ->
                val mediaPath = resolveObject(bucketName, name)
                if (mediaPath.isRegularFile()) {
                    val newUrl =
                        UrlBuilder.fromString(url)
                            .withPath("objects/$bucketName/$name")
                            .toString()
                    ObjectStorageRecord(
                        newUrl,
                        mediaPath.getLastModifiedTime().toInstant().toKotlinInstant()
                            .toLocalDateTime(TimeZone.UTC),
                        name,
                    )
                } else {
                    null
                }
            }
        }

    @OptIn(ExperimentalPathApi::class)
    suspend fun clean(bucketName: String): CustomResult<Unit, FilesystemError> =
        useFileSystem {
            val bucketPath = resolveBucket(bucketName)
            bucketPath.deleteRecursively()
        }

    suspend fun list(bucketName: String, prefix: String): CustomResult<List<ObjectStorageRecord>, FilesystemError> {
        val p = if (prefix.isBlank()) resolveBucket(bucketName) else resolveObject(bucketName, prefix)
        if (p.notExists()) {
            return CustomResult.success(emptyList())
        }
        return useFileSystem {
            buildList<String?> {
                p.visitFileTree(1) {
                    onVisitFile { file, _ ->
                        add("$prefix${file.name}")
                        FileVisitResult.CONTINUE
                    }
                }
            }.filterNotNull()
        }.fold(onSuccess = { get(bucketName, it) }, onFailure = { CustomResult.Failure(FilesystemError.fromError(it)) })
    }

    suspend fun copy(
        bucketName: String,
        copyPacks: List<CopyPack>,
    ): CustomResult<List<ObjectStorageRecord>, FilesystemError> =
        useFileSystem {
            val bucketPath = resolveBucket(bucketName)
            copyPacks.map {
                val p = resolveObject(bucketPath, it.originFullName)
                if (!p.isRegularFile()) {
                    error("${it.originFullName} not exists")
                }
                val targetFile = resolveObject(bucketPath, it.newFullName).createParentDirectories()
                p.copyTo(targetFile, true)
                it.newFullName
            }
        }.fold(onSuccess = { get(bucketName, it) }, onFailure = { CustomResult.Failure(FilesystemError.fromError(it)) })

    suspend fun getInputStream(bucketName: String, name: String): CustomResult<BufferedInputStream, FilesystemError> =
        useFileSystem {
            val mediaPath = resolveObject(bucketName, name)
            if (mediaPath.isRegularFile()) {
                Files.newInputStream(mediaPath).buffered()
            } else {
                error("file $name not exists")
            }
        }

    suspend fun compose(
        bucketName: String,
        targetFullName: String,
        sourceFullNames: List<String>,
        expectedSha256: String,
    ): CustomResult<ObjectStorageWriteRecord, FilesystemError> =
        useFileSystem {
            val bucketPath = resolveBucket(bucketName)
            val target = resolveObject(bucketPath, targetFullName).createParentDirectories()
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val temporary = Files.createTempFile(target.parent, ".compose-", ".tmp")
            try {
                Files.newOutputStream(temporary).buffered().use { out ->
                    val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
                    sourceFullNames.forEach { src ->
                        val source = resolveObject(bucketPath, src)
                        check(source.isRegularFile()) { "source $src not exists" }
                        Files.newInputStream(source).buffered().use { input ->
                            var count = input.read(bytes)
                            while (count >= 0) {
                                digest.update(bytes, 0, count)
                                out.write(bytes, 0, count)
                                count = input.read(bytes)
                            }
                        }
                    }
                }
                check(digest.digest().toHexString() == expectedSha256) { "file hash mismatch" }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                ObjectStorageWriteRecord(targetFullName)
            } finally {
                Files.deleteIfExists(temporary)
            }
        }

    suspend fun delete(bucketName: String, names: List<String>): CustomResult<Unit, FilesystemError> =
        useFileSystem {
            val bucketPath = resolveBucket(bucketName)
            names.forEach { name ->
                val p = resolveObject(bucketPath, name)
                if (p.exists()) {
                    Files.deleteIfExists(p)
                }
            }
        }

    suspend fun <T> useFileSystem(block: suspend () -> T): CustomResult<T, FilesystemError> =
        withContext(Dispatchers.IO) {
            try {
                CustomResult.success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                CustomResult.Failure(FilesystemError.from(exception))
            }
        }

    suspend fun getPathResponse(it: List<String>): Path? =
        useFileSystem {
            if (it.size < 2) return@useFileSystem null
            val path = resolveObject(it.first(), it.drop(1).joinToString("/"))
            path.takeIf { candidate -> candidate.isRegularFile() }?.toRealPath()
        }.getOrNull()

    private fun resolveBucket(bucketName: String): Path {
        val bucket = Paths.get(bucketName)
        require(!bucket.isAbsolute && bucket.nameCount == 1 && bucketName !in setOf(".", "..")) {
            "invalid bucket name"
        }
        return resolveWithoutLinks(base, bucketName)
    }

    private fun resolveObject(bucketName: String, name: String): Path = resolveObject(resolveBucket(bucketName), name)

    private fun resolveObject(bucketPath: Path, name: String): Path {
        require(name.isNotBlank()) { "object name is empty" }
        val candidate = Paths.get(name)
        require(!candidate.isAbsolute) { "absolute object name is not allowed" }
        val resolved = bucketPath.resolve(candidate).normalize()
        require(resolved.startsWith(bucketPath)) { "object name escapes its bucket" }
        return resolveWithoutLinks(bucketPath, bucketPath.relativize(resolved).pathString)
    }

    private fun resolveWithoutLinks(parent: Path, relative: String): Path {
        var resolved = parent
        Paths.get(relative).forEach { component ->
            resolved = resolved.resolve(component)
            require(!Files.isSymbolicLink(resolved)) { "symbolic links are not allowed in object paths" }
        }
        return resolved
    }
}

internal data class UploadPack(val file: File, val fullName: String)
