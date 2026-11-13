/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.error.ObjectStorageError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.Dimension
import kotlinx.datetime.LocalDateTime
import java.io.File
import java.io.InputStream

data class ObjectStorageRecord(val url: String, val lastModified: LocalDateTime, val fullName: String)

data class ObjectStorageWriteRecord(val fullName: String)

data class PresignContext(val uid: String?, val ip: String?)

data class UploadPack(val file: File, val name: String, val size: Long, val fullName: String, val sha256: String)

data class ProcessedUploadPack(val pack: UploadPack, val contentType: String, val dimension: Dimension? = null)

data class CopyPack(val originFullName: String, val newFullName: String)

interface ObjectStorageService {
    suspend fun upload(
        bucketName: String,
        uploadPacks: List<UploadPack>,
    ): CustomResult<List<ObjectStorageWriteRecord>, ObjectStorageError>

    /**
     * @param bucketName 存储桶名称
     * @param names 完整的name
     * @return 与名称对应的对象存储记录
     */
    suspend fun get(
        bucketName: String,
        names: List<String>,
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError>

    suspend fun getWithPresignContext(
        bucketName: String,
        names: List<String>,
        presignContext: PresignContext?,
        responseContentTypes: Map<String, String> = emptyMap(),
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError> = get(bucketName, names)

    suspend fun clean(bucketName: String): CustomResult<Unit, ObjectStorageError>

    suspend fun list(bucketName: String, prefix: String): CustomResult<List<ObjectStorageRecord>, ObjectStorageError>

    suspend fun copy(
        bucketName: String,
        copyPacks: List<CopyPack>,
    ): CustomResult<List<ObjectStorageRecord>, ObjectStorageError>

    suspend fun getInputStream(bucketName: String, name: String): CustomResult<InputStream, ObjectStorageError>

    /**
     * 使用对象存储的 compose/concat 能力，将同一 bucket 下的多个源对象顺序合并为一个目标对象。.
     * @param bucketName 目标与源对象所在的 bucket（需相同）
     * @param targetFullName 合并后目标对象完整路径
     * @param sourceFullNames 源对象完整路径列表，按顺序依次合并
     * @param expectedSha256 文件内容的十六进制 SHA256，由 filesystem 在复制时校验
     * @param expectedCrc32c 文件内容的 CRC32C（大端四字节 Base64），由 MinIO 合并时校验
     */
    suspend fun compose(
        bucketName: String,
        targetFullName: String,
        sourceFullNames: List<String>,
        expectedSha256: String,
        expectedCrc32c: String,
    ): CustomResult<ObjectStorageWriteRecord, ObjectStorageError>

    /**
     * 删除 bucket 下的多个对象。.
     */
    suspend fun delete(bucketName: String, names: List<String>): CustomResult<Unit, ObjectStorageError>
}

interface ObjectStorageServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): ObjectStorageService
}
