/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.minio

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.MinIoConnection
import com.storyteller_f.alphaheart.backend.core.error.ObjectStorageError
import com.storyteller_f.alphaheart.backend.core.service.CacheService
import com.storyteller_f.alphaheart.backend.core.service.CacheServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.CopyPack
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageRecord
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageWriteRecord
import com.storyteller_f.alphaheart.backend.core.service.PresignContext
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.recover
import com.storyteller_f.shared.utils.combineCrc32c
import io.github.aakira.napier.Napier
import io.mikael.urlbuilder.UrlBuilder
import io.minio.*
import io.minio.Http.Method
import io.minio.errors.ErrorResponseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinLocalDateTime
import java.io.InputStream
import java.net.URI
import java.util.ServiceLoader
import java.util.concurrent.TimeUnit
import kotlin.String
import kotlin.time.ExperimentalTime

class MinIoObjectStorageService(private val connection: MinIoConnection, private val minioHost: String?) :
    ObjectStorageService {
    /** Cache used for generated object-storage metadata. */
    val cache: CacheService<String, String> =
        ServiceLoader.load(CacheServiceFactory::class.java).first {
            it.match(MergedEnv(emptyList()))
        }.build<String, String>(
            MergedEnv(emptyList()),
            String::class,
        )

    override suspend fun clean(bucketName: String): CustomResult<Unit, ObjectStorageError> =
        useMinIoClient(connection) {
            if (bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                removeAllObject(bucketName)
                Napier.i {
                    "clean media done"
                }
            } else {
                Napier.i {
                    "bucket not exists"
                }
            }
        }

    override suspend fun list(
        bucketName: String,
        prefix: String,
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError> =
        useMinIoClient(
            connection,
        ) {
            val names =
                try {
                    listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).prefix(prefix).recursive(false).build(),
                    ).map {
                        it.get().objectName()
                    }
                } catch (e: ErrorResponseException) {
                    if (e.errorResponse().code() == "NoSuchBucket") {
                        emptyList()
                    } else {
                        throw e
                    }
                }
            get(bucketName, names).getOrThrow()
        }

    override suspend fun copy(
        bucketName: String,
        copyPacks: List<CopyPack>,
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError> =
        useMinIoClient(
            connection,
        ) {
            copyPacks.map { copyPack ->
                copyObject(
                    CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(copyPack.newFullName)
                        .metadataDirective(Directive.COPY)
                        .taggingDirective(Directive.COPY)
                        .source(SourceObject.builder().bucket(bucketName).`object`(copyPack.originFullName).build())
                        .build(),
                ).`object`()
            }
        }.andThen(ObjectStorageError::fromError) { objectNames ->
            get(bucketName, objectNames)
        }

    override suspend fun getInputStream(
        bucketName: String,
        name: String,
    ): CustomResult<InputStream, ObjectStorageError> =
        useMinIoClient(
            connection,
        ) {
            getObject(GetObjectArgs.builder().bucket(bucketName).`object`(name).build())
        }

    override suspend operator fun get(
        bucketName: String,
        names: List<String>,
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError> = getInternal(bucketName, names, null)

    override suspend fun getWithPresignContext(
        bucketName: String,
        names: List<String>,
        presignContext: PresignContext?,
        responseContentTypes: Map<String, String>,
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError> {
        val result =
            getInternal(
                bucketName = bucketName,
                names = names,
                presignContext = presignContext,
                responseContentTypes = responseContentTypes,
            )
        return result
    }

    override suspend fun upload(
        bucketName: String,
        uploadPacks: List<UploadPack>,
    ): CustomResult<List<ObjectStorageWriteRecord>, ObjectStorageError> {
        val result =
            useMinIoClient(connection) {
                if (!bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                    makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                }
                uploadPacks.map { uploadPack ->
                    val resp =
                        uploadPack.file.inputStream().buffered().use { input ->
                            putObject(
                                PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .`object`(uploadPack.fullName)
                                    .stream(input, uploadPack.size, -1)
                                    .checksum(Checksum.Algorithm.CRC32C)
                                    .extraHeaders(mapOf("x-amz-checksum-type" to "FULL_OBJECT"))
                                    .build(),
                            )
                        }
                    ObjectStorageWriteRecord(resp.`object`())
                }
            }
        return result
    }

    override suspend fun compose(
        bucketName: String,
        targetFullName: String,
        sourceFullNames: List<String>,
        expectedSha256: String,
        expectedCrc32c: String,
    ): CustomResult<ObjectStorageWriteRecord, ObjectStorageError> =
        useMinIoClient(connection) {
            if (!bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            }
            var crc32c = "AAAAAA=="
            val sources =
                sourceFullNames.map { name ->
                    val metadata =
                        statObject(
                            StatObjectArgs.builder().bucket(bucketName).`object`(name)
                                .extraHeaders(mapOf("x-amz-checksum-mode" to "ENABLED")).build(),
                        )
                    check(metadata.headers()["x-amz-checksum-type"] == "FULL_OBJECT") { "full object checksum missing" }
                    val sourceChecksum =
                        metadata.headers()["x-amz-checksum-crc32c"]
                            ?: error("source crc32c missing")
                    crc32c = combineCrc32c(crc32c, sourceChecksum, metadata.size())
                    SourceObject.builder().bucket(bucketName).`object`(name).matchETag(metadata.etag()).build()
                }
            check(crc32c == expectedCrc32c) { "file crc32c mismatch" }
            // MinIO multipart-copy does not feed its checksum reader. Combine verified
            // source CRCs instead, and pin each source's ETag so the checked bytes cannot change.
            // https://github.com/minio/minio/blob/master/cmd/object-multipart-handlers.go
            composeObject(
                ComposeObjectArgs.builder().bucket(bucketName).`object`(targetFullName).sources(sources).build(),
            )
            ObjectStorageWriteRecord(targetFullName)
        }

    override suspend fun delete(bucketName: String, names: List<String>): CustomResult<Unit, ObjectStorageError> =
        useMinIoClient(connection) {
            names.forEach { name ->
                try {
                    removeObject(RemoveObjectArgs.builder().bucket(bucketName).`object`(name).build())
                } catch (e: ErrorResponseException) {
                    // Ignore missing keys
                    if (e.errorResponse().code() != "NoSuchKey") throw e
                }
            }
        }

    private suspend fun getInternal(
        bucketName: String,
        names: List<String>,
        presignContext: PresignContext?,
        responseContentTypes: Map<String, String> = emptyMap(),
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError> {
        val hasContext = presignContext?.uid.isNullOrBlank().not() || presignContext?.ip.isNullOrBlank().not()
        return useMinIoClient(connection) {
            names.mapNotNull { objName ->
                try {
                    val responseContentType = responseContentTypes[objName]
                    val cacheKey =
                        presignCacheKey(
                            bucketName = bucketName,
                            objName = objName,
                            presignContext = presignContext,
                            responseContentType = responseContentType,
                        )
                    val minioObjectUrl =
                        if (hasContext) {
                            cache.get(cacheKey) {
                                getMinioObjectUrl(
                                    bucketName = bucketName,
                                    objName = objName,
                                    presignContext = presignContext,
                                    responseContentType = responseContentType,
                                )
                            }
                        } else {
                            cache.get(cacheKey) {
                                getMinioObjectUrl(
                                    bucketName = bucketName,
                                    objName = objName,
                                    presignContext = null,
                                    responseContentType = responseContentType,
                                )
                            }
                        }
                    val url =
                        if (minioHost.isNullOrBlank()) {
                            minioObjectUrl
                        } else {
                            replaceUrl(minioHost, minioObjectUrl)
                        }
                    val statObject = statObject(StatObjectArgs.builder().bucket(bucketName).`object`(objName).build())
                    val lastModified = statObject.lastModified().toLocalDateTime().toKotlinLocalDateTime()
                    ObjectStorageRecord(url, lastModified, objName)
                } catch (e: ErrorResponseException) {
                    if (e.errorResponse().code() == "NoSuchKey") {
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}

fun replaceUrl(minioHost: String, minioObjectUrl: String?): String {
    val host = URI.create(minioHost)
    return UrlBuilder.fromString(minioObjectUrl)
        .withHost(host.host)
        .withPort(host.port.takeIf { it > 0 } ?: if (host.scheme == "https") 443 else 80)
        .withScheme(host.scheme)
        .toString()
}

private suspend fun <R> useMinIoClient(
    minIoConnection: MinIoConnection,
    block: suspend MinioClient.() -> R,
): CustomResult<R, ObjectStorageError> {
    val point = Exception("MinIO request call site")
    return captureFailure(ObjectStorageError::from) {
        MinioClient.builder()
            .endpoint(minIoConnection.url)
            .credentials(minIoConnection.user, minIoConnection.pass)
            .build().use {
                withContext(Dispatchers.IO) {
                    it.block()
                }
            }
    }.recover(ObjectStorageError::fromError) { e ->
        point.initCause(e.cause)
        Napier.e(throwable = point) {
            "minio error"
        }
        CustomResult.Failure(ObjectStorageError.from(point))
    }
}

private fun MinioClient.removeAllObject(bucketName: String) {
    listObjects(ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()).forEach {
        if (!it.get().isDir) {
            removeObject(RemoveObjectArgs.builder().bucket(bucketName).`object`(it.get().objectName()).build())
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun MinioClient.getMinioObjectUrl(
    bucketName: String,
    objName: String,
    presignContext: PresignContext?,
    responseContentType: String?,
) = getPresignedObjectUrl(
    GetPresignedObjectUrlArgs.builder()
        .method(Method.GET)
        .bucket(bucketName)
        .`object`(objName)
        .extraQueryParams(
            buildMap {
                val uid = presignContext?.uid?.takeIf { it.isNotBlank() }
                val ip = presignContext?.ip?.takeIf { it.isNotBlank() }
                if (uid != null) put("a_uid", uid)
                if (ip != null) put("a_ip", ip)
                responseContentType?.takeIf { it.isNotBlank() }?.let {
                    put("response-content-type", it)
                }
            },
        )
        .expiry(7, TimeUnit.DAYS)
        .build(),
)

private fun presignCacheKey(
    bucketName: String,
    objName: String,
    presignContext: PresignContext?,
    responseContentType: String?,
): String {
    val parts =
        listOf(
            bucketName,
            objName,
            presignContext?.uid.orEmpty(),
            presignContext?.ip.orEmpty(),
            responseContentType.orEmpty(),
        )
    return parts.joinToString("\u0000")
}

class MinioObjectStorageServiceFactory : ObjectStorageServiceFactory {
    override fun match(env: MergedEnv): Boolean = env["MEDIA_SERVICE"] == "minio"

    override fun build(env: MergedEnv): ObjectStorageService {
        val url = env["MINIO_URL"] ?: error("MINIO_URL is empty")
        val name = env["MINIO_NAME"] ?: error("MINIO_NAME is empty")
        val pass = env["MINIO_PASS"] ?: error("MINIO_PASS is empty")
        return MinIoObjectStorageService(MinIoConnection(url, name, pass), env["MINIO_HOST"])
    }
}
