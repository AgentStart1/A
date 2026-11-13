/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchService
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.FileSearchService
import com.storyteller_f.alphaheart.backend.core.service.FileSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchService
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.NameService
import com.storyteller_f.alphaheart.backend.core.service.NameServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchService
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.UserSearchService
import com.storyteller_f.alphaheart.backend.core.service.UserSearchServiceFactory
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.recover
import com.storyteller_f.shared.model.Dimension
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.*
import javax.imageio.ImageIO
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants

interface Backend {
    val customConfig: CustomConfig
    val topicSearchService: TopicSearchService
    val roomSearchService: RoomSearchService
    val communitySearchService: CommunitySearchService
    val userSearchService: UserSearchService
    val memberSearchService: MemberSearchService
    val fileSearchService: FileSearchService
    val objectStorageService: ObjectStorageService
    val nameService: NameService
    val database: CombinedDatabase

    companion object
}

fun mediaService(env: MergedEnv): ObjectStorageService {
    val factory =
        ServiceLoader.load(ObjectStorageServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported media service type ${env["MEDIA_SERVICE"]}")
    return factory.build(env)
}

fun buildTopicSearchService(env: MergedEnv): TopicSearchService {
    val factory =
        ServiceLoader.load(TopicSearchServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported topic search service type ${env["SEARCH_SERVICE"]}")
    return factory.build(env)
}

fun buildUserSearchService(env: MergedEnv): UserSearchService {
    val factory =
        ServiceLoader.load(UserSearchServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported user search service type ${env["SEARCH_SERVICE"]}")
    return factory.build(env)
}

fun buildRoomSearchService(env: MergedEnv): RoomSearchService {
    val factory =
        ServiceLoader.load(RoomSearchServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported room search service type ${env["SEARCH_SERVICE"]}")
    return factory.build(env)
}

fun buildCommunitySearchService(env: MergedEnv): CommunitySearchService {
    val factory =
        ServiceLoader.load(CommunitySearchServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported community search service type ${env["SEARCH_SERVICE"]}")
    return factory.build(env)
}

fun buildMemberSearchService(env: MergedEnv): MemberSearchService {
    val factory =
        ServiceLoader.load(MemberSearchServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported member search service type ${env["SEARCH_SERVICE"]}")
    return factory.build(env)
}

fun buildFileSearchService(env: MergedEnv): FileSearchService {
    val factory =
        ServiceLoader.load(FileSearchServiceFactory::class.java).firstOrNull {
            it.match(env)
        } ?: error("unsupported file search service type ${env["SEARCH_SERVICE"]}")
    return factory.build(env)
}

fun databaseConnection(env: MergedEnv): DatabaseConnection {
    val uri = env["DATABASE_URI"] ?: error("DATABASE_URI is empty")
    val driver = env["DATABASE_DRIVER"] ?: error("DATABASE_DRIVE is empty")
    val user = env["DATABASE_USER"] ?: error("DATABASE_USER is empty")
    val pass = env["DATABASE_PASS"] ?: error("DATABASE_PASS is empty")
    return DatabaseConnection(uri, driver, user, pass)
}

fun buildNameService(env: MergedEnv): NameService {
    val factory =
        ServiceLoader.load(NameServiceFactory::class.java).firstOrNull()
            ?: error("unsupported name service type")
    return factory.build(env)
}

/**
 * 安装教程 https://github.com/AOMediaCodec/libavif
 * On Windows, AVIF_LIBRARY_PATH selects the native library directory.
 */
fun loadAvif() {
    val osName = System.getProperty("os.name")
    when {
        osName.contains("mac", true) -> System.setProperty("jna.library.path", "/opt/homebrew/lib")

        osName.contains("win", true) -> {
            val libraryPath = System.getenv("AVIF_LIBRARY_PATH")
            require(!libraryPath.isNullOrBlank()) { "AVIF_LIBRARY_PATH must be set on Windows" }
            System.setProperty("jna.library.path", libraryPath)
        }

        else -> System.setProperty("jna.library.path", "/usr/local/lib")
    }
}

suspend fun extractSvgDimensionInfo(inputStreamProducer: suspend () -> InputStream): Dimension? =
    withContext(
        Dispatchers.IO,
    ) {
        inputStreamProducer().use { inputStream ->
            val factory = XMLInputFactory.newInstance()
            val reader = factory.createXMLStreamReader(inputStream)
            try {
                if (reader.hasNext()) {
                    val event = reader.next()
                    if (event == XMLStreamConstants.START_ELEMENT &&
                        "svg".equals(
                            reader.localName,
                            true,
                        )
                    ) {
                        val viewBox: String? = reader.getAttributeValue(null, "viewBox")
                        val height: String? = reader.getAttributeValue(null, "height")
                        val width: String? = reader.getAttributeValue(null, "width")
                        getSvgDimension(viewBox, width to height)
                    } else {
                        null
                    }
                } else {
                    null
                }
            } finally {
                reader.close()
            }
        }
    }

suspend fun getImageDimension(
    filePath: String,
    contentType: String,
    inputStreamProducer: suspend () -> BufferedInputStream,
): Dimension? {
    if (contentType == "image/svg+xml") {
        return extractSvgDimensionInfo(inputStreamProducer)
    }
    val dimension =
        ImageIO.getImageReadersByMIMEType(contentType).asSequence()
            .firstNotNullOfOrNull { reader ->
                try {
                    inputStreamProducer().use {
                        reader.input = ImageIO.createImageInputStream(it)
                        reader.read(reader.minIndex)
                        Dimension(reader.getWidth(reader.minIndex), reader.getHeight(reader.minIndex))
                    }
                } catch (e: Throwable) {
                    Napier.e(throwable = e) {
                        "get image dimension failed $filePath"
                    }
                    null
                } finally {
                    reader.dispose()
                }
            }
    if (dimension == null) {
        error("get image dimension failed")
    }
    return dimension
}

fun getSvgDimension(viewBox: String?, pair: Pair<String?, String?>): Dimension? {
    // 获取 width 和 height 属性
    val width = pair.first?.removeSuffix("px")?.toIntOrNull()
    val height = pair.second?.removeSuffix("px")?.toIntOrNull()
    if (width != null && height != null) {
        return Dimension(width, height)
    }
    if (viewBox != null) {
        val viewBoxSizeList =
            viewBox.split(Regex("\\s+")).map {
                it.trim().toFloatOrNull()
            }
        if (viewBoxSizeList.size == 4) {
            val width = viewBoxSizeList[2]
            val height = viewBoxSizeList[3]
            if (width != null && height != null) {
                return Dimension(width.toInt(), height.toInt())
            }
        }
    }
    return null
}

suspend fun <T> Backend.addIfNotExists(
    get: suspend () -> CustomResult<T?, DatabaseError>,
    add: suspend () -> CustomResult<T, DatabaseError>,
): CustomResult<T?, DatabaseError> =
    get().andThen(DatabaseError::fromError) {
        if (it != null) {
            CustomResult.success(it)
        } else {
            add().recover(DatabaseError::fromError) { failure ->
                if (failure is ConstraintFailure) {
                    get()
                } else {
                    CustomResult.Failure(failure)
                }
            }
        }
    }
