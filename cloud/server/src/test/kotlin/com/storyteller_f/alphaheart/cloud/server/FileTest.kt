/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.backend.core.getImageDimension
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.abortChunkUpload
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.completeChunkUpload
import com.storyteller_f.alphaheart.client.core.copy
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.extractAlbum
import com.storyteller_f.alphaheart.client.core.getChunkStatus
import com.storyteller_f.alphaheart.client.core.getFavorites
import com.storyteller_f.alphaheart.client.core.getFileInfo
import com.storyteller_f.alphaheart.client.core.getFileList
import com.storyteller_f.alphaheart.client.core.getFileRefs
import com.storyteller_f.alphaheart.client.core.getQuotaInfo
import com.storyteller_f.alphaheart.client.core.getSubscriptions
import com.storyteller_f.alphaheart.client.core.initChunkUpload
import com.storyteller_f.alphaheart.client.core.removeFavorite
import com.storyteller_f.alphaheart.client.core.removeSubscription
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.alphaheart.client.core.uploadChunk
import com.storyteller_f.alphaheart.cloud.core.service.getCoverExtensionFromMimeType
import com.storyteller_f.alphaheart.cloud.core.utils.readFlacAlbumFromAudioStream
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import com.storyteller_f.shared.utils.calculateFileChecksums
import com.storyteller_f.shared.utils.sha256
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.bytedeco.opencv.global.opencv_core
import org.bytedeco.opencv.global.opencv_imgcodecs
import org.bytedeco.opencv.global.opencv_imgproc
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Scalar
import org.bytedeco.opencv.opencv_core.Size
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileTest {
    @Test
    fun `upload adds file and consumes quota`() =
        test {
            attachSession { session ->
                val response =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("hello"),
                    ).getOrThrow()
                assertEquals("${session.uid}/hello.txt", response.data.first().fullName)
                val fileList = getFileList(session.uid, ObjectType.USER, null, 10)
                assertListSize(1, fileList)
                val quotaInfo = getQuotaInfo(ObjectTuple(session.uid, ObjectType.USER)).getOrThrow()
                val fileInfo = fileList.getOrThrow().data.first()
                HttpClient().use { downloadClient ->
                    assertEquals("hello", downloadClient.get(fileInfo.url).bodyAsText())
                }
                assertEquals(fileInfo.size, quotaInfo.used)
                assertEquals(null, quotaInfo.lockId)
                fileInfo
            }
        }

    @Test
    fun `copy creates file for new owner and consumes quota`() =
        test {
            val firstTuple =
                attachSession { session ->
                    val response =
                        upload(
                            ObjectTuple(session.uid, ObjectType.USER),
                            getUploadDataFromText("hello"),
                        ).getOrThrow()
                    response.data.first()
                }
            attachSession { session ->
                val response = copy(firstTuple.custom.id).getOrThrow()
                assertEquals("${session.uid}/hello.txt", response.fullName)
                assertListSize(1, getFileList(session.uid, ObjectType.USER, null, 10))
                val quotaInfo = getQuotaInfo(ObjectTuple(session.uid, ObjectType.USER)).getOrThrow()
                assertEquals(firstTuple.custom.size, quotaInfo.used)
                assertEquals(null, quotaInfo.lockId)
            }
        }

    @Test
    fun `get png size`() =
        runTest {
            val dimension =
                getImageDimension("avatar1.png", "image/png") {
                    ClassLoader.getSystemResourceAsStream("avatar1.png")!!.buffered()
                }
            assertNotNull(dimension)
            assertEquals(dimension.width, 420)
            assertEquals(dimension.height, 420)
        }

    @Test
    fun `extract audio album`() {
        val vendor = System.getProperty("java.vendor") // 厂商
        val version = System.getProperty("java.version") // 版本
        val runtime = System.getProperty("java.runtime.name")

        Napier.d { "JDK under test: vendor=$vendor, version=$version, runtime=$runtime" }
        // https://github.com/bytedeco/javacpp-presets/issues/1649
        if (vendor.contains("OpenJDK", ignoreCase = true)) {
            error("❌ 不允许使用 OpenJDK")
        }
        ClassLoader.getSystemResourceAsStream("I_Do_not_Wanna_Live_Forever.flac")?.buffered()?.use {
            it.readFlacAlbumFromAudioStream { image, mimeType ->
                val name = "build/test/cover${getCoverExtensionFromMimeType(mimeType)}"
                FileOutputStream(name).use { output ->
                    output.write(image)
                }
            }
        } ?: error("flac is not exists")
        ClassLoader.getSystemResourceAsStream("cover.jpg")?.use {
            Files.copy(it, Path("build/test/cover_origin.jpg"), StandardCopyOption.REPLACE_EXISTING)
        } ?: error("cover is not exists")
        val img1 = opencv_imgcodecs.imread("build/test/cover.jpg")
        if (img1.empty()) {
            error("图像加载失败！")
        }
        val img2 = opencv_imgcodecs.imread("build/test/cover_origin.jpg")
        if (img2.empty()) {
            error("图像加载失败！")
        }
        val psnr = getPSNR(img1, img2)
        val mssim = getMSSIM(img1, img2)!!
        assertEquals(0.0, psnr, 0.00001)
        assertEquals(0.999999, mssim.get(0), 0.00001)
        assertEquals(0.999999, mssim.get(1), 0.00001)
        assertEquals(0.999999, mssim.get(2), 0.00001)
    }

    @Test
    fun `test audio album`() =
        test {
            attachSession { session ->
                val name = "I_Do_not_Wanna_Live_Forever.flac"
                val data = getUploadDataFromResources(name)
                val response = upload(session.uid ob ObjectType.USER, data).getOrThrow()
                extractAlbum(response.data.first().id).getOrThrow()
            }
        }

    @Test
    fun `test remove image info`() =
        test {
            attachSession { session ->
                val oldMeta =
                    Imaging.getMetadata(ClassLoader.getSystemResourceAsStream("52873358902_7857530666_o.jpg")!!, null)
                val first =
                    upload(
                        session.uid ob ObjectType.USER,
                        getUploadDataFromResources("52873358902_7857530666_o.jpg"),
                    ).getOrThrow().data.first()
                val tmpFile = File("build/test/52873358902_7857530666_o.jpg")
                downloadFile(tmpFile, first)
                val newMeta = Imaging.getMetadata(tmpFile)
                assertTrue(oldMeta is JpegImageMetadata)
                assertTrue(newMeta is JpegImageMetadata)
                assertNotNull(oldMeta.exif)
                assertNull(newMeta.exif)
            }
        }

    @Test
    fun `chunked upload end-to-end`() =
        test {
            attachSession { session ->
                val tuple = ObjectTuple(session.uid, ObjectType.USER)
                val name = "chunked.bin" // 保持长度 < 60，避免保存名被改写
                val totalSize = 6_000_000L // 5.72Mib
                val chunkSize = 5_242_880L // 5Mib
                val fileSha256 =
                    sha256(
                        Buffer().apply {
                            for (i in 0 until totalSize) {
                                writeByte((i % 256).toByte())
                            }
                        }.peek(),
                    )

                // 初始化分块上传，拿到 recordId
                val init =
                    initChunkUpload(
                        tuple,
                        name,
                        totalSize,
                        ContentType.Application.OctetStream,
                        chunkSize,
                        fileSha256,
                        calculateTestFileCrc(totalSize),
                    ).getOrThrow()
                val recordId = init.recordId

                val chunkCount = ceil(totalSize / chunkSize.toDouble()).toInt()
                for (index in 0 until chunkCount) {
                    val start = index * chunkSize
                    val endExclusive = minOf(start + chunkSize, totalSize)
                    val buf =
                        Buffer().apply {
                            for (i in start until endExclusive) {
                                writeByte((i % 256).toByte())
                            }
                        }
                    val hash = sha256(buf.peek())
                    uploadChunk(recordId, index, buf, hash).getOrThrow()
                }

                // 校验状态返回的分片索引与元信息
                val status = getChunkStatus(recordId).getOrThrow()
                assertEquals(chunkCount, status.uploaded.size)
                assertEquals((0 until chunkCount).toList(), status.uploaded)
                assertEquals(chunkSize, status.chunkSize)
                assertEquals(totalSize, status.size)

                // 完成分块合并并验证文件记录
                val fileInfo = completeChunkUpload(recordId).getOrThrow()
                assertEquals("${session.uid}/$name", fileInfo.fullName)
                assertEquals(totalSize, fileInfo.size)
                assertTrue(fileInfo.contentType.startsWith("application"))

                // 列表查询应返回 1 个文件
                val list = getFileList(session.uid, ObjectType.USER, null, 10)
                assertEquals(1, list.getOrThrow().data.size)
            }
        }

    @Test
    fun `abort chunk upload cleans up and unlocks quota`() =
        test {
            attachSession { session ->
                val tuple = ObjectTuple(session.uid, ObjectType.USER)
                val name = "to-abort.bin"
                val totalSize = 180_500L
                val chunkSize = 64_000L
                val fileSha256 =
                    sha256(
                        Buffer().apply {
                            for (i in 0 until totalSize) {
                                writeByte((i % 256).toByte())
                            }
                        }.peek(),
                    )

                val init =
                    initChunkUpload(
                        tuple,
                        name,
                        totalSize,
                        ContentType.Application.OctetStream,
                        chunkSize,
                        fileSha256,
                        calculateTestFileCrc(totalSize),
                    ).getOrThrow()
                val recordId = init.recordId

                // 初始锁应打开
                val quotaLockId = getQuotaInfo(tuple).getOrThrow().lockId
                assertNotEquals(quotaLockId, null)

                // 上传一个分片后取消
                val bytes = ByteArray(chunkSize.toInt()) { idx -> (idx % 256).toByte() }
                val buf = Buffer().apply { write(bytes) }
                val hash = sha256(buf.peek())
                uploadChunk(recordId, 0, buf, hash).getOrThrow()

                // 取消上传
                abortChunkUpload(recordId).getOrThrow()

                // 状态查询应失败（记录已删除 + 分片已清理）
                val statusResult = getChunkStatus(recordId).getOrThrow()
                assertEquals(UploadRecordStatus.ABORTED, statusResult.status)

                // 配额锁应释放
                val quotaAfter = getQuotaInfo(tuple).getOrThrow()
                assertEquals(quotaAfter.lockId, null)
            }
        }

    @Test
    fun `chunk hash mismatch is rejected`() =
        test {
            attachSession { session ->
                val tuple = ObjectTuple(session.uid, ObjectType.USER)
                val name = "bad-hash.bin"
                val totalSize = 10_000L
                val chunkSize = 1_000L
                val fileSha256 =
                    sha256(
                        Buffer().apply {
                            for (i in 0 until totalSize) {
                                writeByte((i % 256).toByte())
                            }
                        }.peek(),
                    )

                val init =
                    initChunkUpload(
                        tuple,
                        name,
                        totalSize,
                        ContentType.Application.OctetStream,
                        chunkSize,
                        fileSha256,
                        calculateTestFileCrc(totalSize),
                    ).getOrThrow()
                val recordId = init.recordId

                // 构造错误哈希的分片
                val bytes = ByteArray(chunkSize.toInt()) { 1 }
                val buf = Buffer().apply { write(bytes) }

                @Suppress("SpellCheckingInspection")
                val wrongHash = "deadbeef" // 故意错误
                val r = uploadChunk(recordId, 0, buf, wrongHash)
                assertTrue(r.isFailure)

                // 状态查询不应包含该分片索引
                val status = getChunkStatus(recordId).getOrThrow()
                assertTrue(status.uploaded.isEmpty())
            }
        }

    @Test
    fun `file sha256 mismatch is rejected for direct upload`() =
        test {
            attachSession { session ->
                val tuple = ObjectTuple(session.uid, ObjectType.USER)
                val data = getUploadDataFromText("hello", "mismatch.txt")
                val wrongSha256 = "0".repeat(64)
                val wrongData =
                    UploadData(
                        data.size,
                        data.name,
                        data.contentType,
                        wrongSha256,
                        data.block,
                    )
                val r = upload(tuple, wrongData)
                assertTrue(r.isFailure)
                assertEquals(0, getFileList(session.uid, ObjectType.USER, null, 10).getOrThrow().data.size)
            }
        }

    @Test
    fun `sha256 mismatch fails chunk completion and record`() =
        test {
            attachSession { session ->
                val tuple = ObjectTuple(session.uid, ObjectType.USER)
                val name = "bad-file-hash.bin"
                val totalSize = 300_000L
                val chunkSize = 64_000L
                val wrongSha256 = "0".repeat(64)

                val init =
                    initChunkUpload(
                        tuple,
                        name,
                        totalSize,
                        ContentType.Application.OctetStream,
                        chunkSize,
                        wrongSha256,
                        calculateTestFileCrc(totalSize),
                    ).getOrThrow()
                val recordId = init.recordId

                val chunkCount = ceil(totalSize / chunkSize.toDouble()).toInt()
                for (index in 0 until chunkCount) {
                    val start = index * chunkSize
                    val endExclusive = minOf(start + chunkSize, totalSize)
                    val buf =
                        Buffer().apply {
                            for (i in start until endExclusive) {
                                writeByte((i % 256).toByte())
                            }
                        }
                    val hash = sha256(buf.peek())
                    uploadChunk(recordId, index, buf, hash).getOrThrow()
                }

                val result = completeChunkUpload(recordId)
                assertTrue(result.isFailure)

                val status = getChunkStatus(recordId).getOrThrow()
                assertEquals(UploadRecordStatus.FAILED, status.status)
                assertTrue(status.uploaded.isEmpty())

                val quotaAfter = getQuotaInfo(tuple).getOrThrow()
                assertEquals(null, quotaAfter.lockId)
                assertEquals(0, getFileList(session.uid, ObjectType.USER, null, 10).getOrThrow().data.size)
            }
        }

    @Test
    fun `get refs for a file referenced by a topic`() =
        test {
            attachSession { session ->
                // 上传一个文件
                val fileInfo =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("test file content"),
                    ).getOrThrow().data.first()

                // 创建一个community和topic引用该文件
                val communityId = createCommunity(NewCommunity("test-aid", "test-name")).getOrThrow().id
                createTopic(
                    ObjectType.COMMUNITY,
                    communityId,
                    "![image](${fileInfo.name})",
                ).getOrThrow()

                // 获取文件引用
                val refs = getFileRefs(fileInfo.id, PaginationQuery(size = 10)).getOrThrow()
                assertEquals(1, refs.data.size)
                assertEquals(ObjectType.TOPIC, refs.data.first().objectType)
                assertEquals(fileInfo.id, refs.data.first().fileId)
            }
        }

    @Test
    fun `test get file refs when file has no references`() =
        test {
            attachSession { session ->
                // 上传一个文件但不引用它
                val fileInfo =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("unreferenced file"),
                    ).getOrThrow().data.first()

                // 获取文件引用应该为空
                val refs = getFileRefs(fileInfo.id, PaginationQuery(size = 10)).getOrThrow()
                assertEquals(0, refs.data.size)
            }
        }

    @Test
    fun `get refs for a file used by multiple topics`() =
        test {
            attachSession { session ->
                // 上传一个文件
                val fileInfo =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("shared file"),
                    ).getOrThrow().data.first()

                // 创建community
                val communityId = createCommunity(NewCommunity("test-aid-2", "test-name-2")).getOrThrow().id

                // 创建多个topic引用该文件
                createTopic(
                    ObjectType.COMMUNITY,
                    communityId,
                    "First topic ![image](${fileInfo.name})",
                ).getOrThrow()

                createTopic(
                    ObjectType.COMMUNITY,
                    communityId,
                    "Second topic ![image](${fileInfo.name})",
                ).getOrThrow()

                // 获取文件引用应该有2个
                val refs = getFileRefs(fileInfo.id, PaginationQuery(size = 10)).getOrThrow()
                assertEquals(2, refs.data.size)
                refs.data.forEach { ref ->
                    assertEquals(ObjectType.TOPIC, ref.objectType)
                    assertEquals(fileInfo.id, ref.fileId)
                }
            }
        }

    @Test
    fun `test add file favorite`() =
        test {
            attachSession { session ->
                val response =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("hello favorite"),
                    ).getOrThrow()
                val fileId = response.data.first().id

                addFavorite(NewFavorite(ObjectType.FILE, fileId)).getOrThrow()
                assertListTotalSize(1, getFavorites(PaginationQuery()))

                val fileInfo = getFileInfo(fileId).getOrThrow()
                assertNotNull(fileInfo.favoriteId)
            }
        }

    @Test
    fun `test remove file favorite`() =
        test {
            attachSession { session ->
                val response =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("hello favorite"),
                    ).getOrThrow()
                val fileId = response.data.first().id

                addFavorite(NewFavorite(ObjectType.FILE, fileId)).getOrThrow()

                removeFavorite(fileId, ObjectType.FILE).getOrThrow()
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }

    @Test
    fun `test add file subscription`() =
        test {
            attachSession { session ->
                val response =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("hello subscription"),
                    ).getOrThrow()
                val fileId = response.data.first().id

                addSubscription(NewSubscription(fileId, ObjectType.FILE)).getOrThrow()
                assertListTotalSize(1, getSubscriptions(PaginationQuery()))

                val fileInfo = getFileInfo(fileId).getOrThrow()
                assertNotNull(fileInfo.subscriptionId)
            }
        }

    @Test
    fun `test remove file subscription`() =
        test {
            attachSession { session ->
                val response =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText("hello subscription"),
                    ).getOrThrow()
                val fileId = response.data.first().id

                addSubscription(NewSubscription(fileId, ObjectType.FILE)).getOrThrow()

                removeSubscription(fileId, ObjectType.FILE).getOrThrow()
                assertListTotalSize(0, getSubscriptions(PaginationQuery()))
            }
        }
}

suspend fun UserSessionManager.downloadFile(tmpFile: File, first: FileInfo) {
    tmpFile.parentFile.mkdir()
    val bytes =
        if (first.url.startsWith("http://localhost/")) {
            client
        } else {
            HttpClient { }
        }.get(first.url) { }.body<ByteArray>()
    tmpFile.writeBytes(bytes)
}

private suspend fun getUploadDataFromResources(name: String): UploadData {
    val inputStream = ClassLoader.getSystemResourceAsStream(name)!!
    val bytes = inputStream.readBytes()
    val data =
        UploadData(
            bytes.size.toLong(),
            name,
            ContentType.defaultForFileExtension("flac"),
            sha256(
                Buffer().apply {
                    write(bytes)
                }.peek(),
            ),
        ) {
            Buffer().apply {
                write(bytes)
            }
        }
    return data
}

private fun getPSNR(mat1: Mat, mat2: Mat): Double {
    val s1 = Mat()
    opencv_core.absdiff(mat1, mat2, s1) // |I1 - I2|
    s1.convertTo(s1, opencv_core.CV_32F) // cannot make a square on 8 bits
    val s2 = s1.mul(s1).asMat() // |I1 - I2|^2

    val s = opencv_core.sumElems(s2) // sum elements per channel

    val sse = s.get(0) + s.get(1) + s.get(2) // sum channels

    if (sse <= 1e-10) { // for small values return zero
        return 0.0
    } else {
        val mse = sse / (mat1.channels() * mat1.total()).toDouble()
        return 10.0 * log10(255 * 255 / mse)
    }
}

private fun getMSSIM(i1: Mat, i2: Mat): Scalar? {
    val constant1 = 6.5025
    val constant2 = 58.5225

    // Initialize intermediate matrices.
    val dataType: Int = opencv_core.CV_32F
    val matrix1 = Mat()
    val matrix2 = Mat()
    i1.convertTo(matrix1, dataType) // cannot calculate on one byte large values
    i2.convertTo(matrix2, dataType)
    val matrix2Squared = matrix2.mul(matrix2).asMat() // I2^2
    val matrix1Squared = matrix1.mul(matrix1).asMat() // I1^2
    val matrix1Matrix2 = matrix1.mul(matrix2).asMat() // I1 * I2

    // Compute intermediate statistics.
    val mean1 = Mat()
    val mean2 = Mat()
    opencv_imgproc.GaussianBlur(matrix1, mean1, Size(11, 11), 1.5)
    opencv_imgproc.GaussianBlur(matrix2, mean2, Size(11, 11), 1.5)
    val mean1Squared = mean1.mul(mean1).asMat()
    val mean2Squared = mean2.mul(mean2).asMat()
    val mean1Mean2 = mean1.mul(mean2).asMat()
    val sigma1Squared = Mat()
    val sigma2Squared = Mat()
    val sigma12 = Mat()
    opencv_imgproc.GaussianBlur(matrix1Squared, sigma1Squared, Size(11, 11), 1.5)
    val sigma1Squared1 = opencv_core.subtract(sigma1Squared, mean1Squared).asMat()
    opencv_imgproc.GaussianBlur(matrix2Squared, sigma2Squared, Size(11, 11), 1.5)
    val sigma2Squared1 = opencv_core.subtract(sigma2Squared, mean2Squared).asMat()
    opencv_imgproc.GaussianBlur(matrix1Matrix2, sigma12, Size(11, 11), 1.5)
    val sigma121 = opencv_core.subtract(sigma12, mean1Mean2).asMat()
    val temp1 = opencv_core.add(opencv_core.multiply(2.0, mean1Mean2), Scalar.all(constant1)).asMat()
    val temp2 = opencv_core.add(opencv_core.multiply(2.0, sigma121), Scalar.all(constant2)).asMat()
    val temp3 = temp1.mul(temp2).asMat() // t3 = ((2*mu1_mu2 + C1).*(2*sigma12 + C2))
    val temp1Final = Mat()
    opencv_core.addWeighted(mean1Squared, 1.0, mean2Squared, 1.0, constant1, temp1Final)
    val temp2Final = Mat()
    opencv_core.addWeighted(sigma1Squared1, 1.0, sigma2Squared1, 1.0, constant2, temp2Final)
    val temp1Result = temp1Final.mul(temp2Final).asMat() // t1 =((mu1_2 + mu2_2 + C1).*(sigma1_2 + sigma2_2 + C2))
    val ssimMap = Mat()
    opencv_core.divide(temp3, temp1Result, ssimMap) // ssim_map =  t3./t1;
    return opencv_core.mean(ssimMap)
}

private suspend fun calculateTestFileCrc(size: Long): String =
    calculateFileChecksums(
        Buffer().apply {
            for (index in 0 until size) writeByte((index % 256).toByte())
        },
    ).crc32c
