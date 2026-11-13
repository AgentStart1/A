/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewTopic
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.ReactionFetch
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.fixedSort
import com.storyteller_f.alphaheart.backend.core.getAllRawTopics
import com.storyteller_f.alphaheart.backend.core.getLatestRawTopic
import com.storyteller_f.alphaheart.backend.core.getRawTopic
import com.storyteller_f.alphaheart.backend.core.getRawTopicByParentId
import com.storyteller_f.alphaheart.backend.core.getRawTopicListByIds
import com.storyteller_f.alphaheart.backend.core.mapPagingResultIfNotNullNullable
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNullable
import com.storyteller_f.alphaheart.backend.core.paging
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.alphaheart.backend.core.types.FileRef
import com.storyteller_f.alphaheart.backend.core.types.RawTopic
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.core.types.toTopicInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.TopicError
import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.alphaheart.cloud.pdf.PdfService
import com.storyteller_f.alphaheart.cloud.pdf.SnapshotGeneration
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onPresent
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.model.TopicReplyPreview
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.obj.NewRoomTopic
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.checkContent
import com.storyteller_f.shared.utils.extractMarkdownMediaLink
import com.storyteller_f.shared.utils.groupByPair
import com.storyteller_f.shared.utils.now
import com.storyteller_f.shared.utils.sha256
import com.storyteller_f.shared.utils.trimMarkdownUnusedContent
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.util.ServiceLoader
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

suspend fun Backend.createPlainTopic(uid: PrimaryKey, newTopic: NewTopic): CustomResult<TopicCreated?, TopicError> {
    if (newTopic.parentType == ObjectType.ROOM) {
        return CustomResult.Failure(
            TopicError.from(ForbiddenException("can't use api to add topic in room")),
        ).mapError(TopicError::fromError)
    }

    val content = newTopic.content.trim()

    if (content.isEmpty()) {
        return CustomResult.Failure(
            TopicError.from(CustomBadRequestException("content is empty")),
        ).mapError(TopicError::fromError)
    }
    checkContent(content).exceptionOrNull()?.let {
        return CustomResult.Failure(TopicError.from(it)).mapError(TopicError::fromError)
    }
    if (content.length > 1000) {
        return CustomResult.Failure(
            TopicError.from(CustomBadRequestException("too long")),
        ).mapError(TopicError::fromError)
    }

    return checkRootWritePermission(
        newTopic.parentType,
        newTopic.parentId,
        uid,
    ).andThenPresent(TopicError::fromError) { (rootType, rootId, level) ->
        if (level >= 10) {
            CustomResult.Failure(TopicError.from(CustomBadRequestException("not support")))
        } else if (rootType == ObjectType.ROOM) {
            CustomResult.Failure(TopicError.from(ForbiddenException("can't use api to add topic in room")))
        } else {
            val parentTuple = ObjectTuple(newTopic.parentId, newTopic.parentType)
            val rootTuple = ObjectTuple(rootId, rootType)
            savePlainTopic(uid, content, level, parentTuple, rootTuple)
        }
    }.onPresent {
        addSubscription(uid, it.tuple())
    }.mapError(TopicError::fromError)
}

private suspend fun Backend.addSubscription(uid: PrimaryKey, objectTuple: ObjectTuple) {
    database.subscription.addSubscription(
        UserSubscription(SnowflakeFactory.nextId(), uid, objectTuple.objectId, objectTuple.objectType, now()),
    ).onFailure {
        Napier.e(it.cause) {
            "add user subscription failed"
        }
    }
}

private suspend fun Backend.savePlainTopic(
    uid: PrimaryKey,
    content: String,
    level: Int,
    parentTuple: ObjectTuple,
    rootTuple: ObjectTuple,
): CustomResult<TopicCreated?, TopicError> {
    val trimmedContent = trimMarkdownUnusedContent(content)
    val newId = SnowflakeFactory.nextId()
    val topic =
        Topic(
            id = newId,
            createdTime = now(),
            author = uid,
            parentId = parentTuple.objectId,
            parentType = parentTuple.objectType,
            rootId = rootTuple.objectId,
            rootType = rootTuple.objectType,
            trimmedContent.encodeToByteArray(),
            false,
            level + 1,
            lastModifiedTime = null,
        )
    val plain = TopicContent.Plain(trimmedContent)
    val topicInfo = RawTopic(topic, plain).toTopicInfo()

    val documentFileList =
        documentFileList(listOf(topicInfo)).map {
            it.second
        }

    objectStorageService.get(A_FILE_DEFAULT_BUCKET, documentFileList).map { list ->
        list.map {
            it.fullName
        }
    }.onSuccess { fileList ->
        val notExistsFileList =
            documentFileList.filter {
                !fileList.contains(it)
            }
        if (notExistsFileList.isNotEmpty()) {
            return CustomResult.Failure(
                TopicError.from(CustomBadRequestException("${notExistsFileList.joinToString()} not exists")),
            ).mapError(TopicError::fromError)
        }
    }.onFailure {
        return CustomResult.Failure(TopicError.fromError(it)).mapError(TopicError::fromError)
    }

    topicSearchService.saveDocument(
        listOf(TopicDocument.fromTopic(topic, plain)),
    ).onFailure {
        Napier.e(it.cause) {
            "save plain topic document failed"
        }
    }
    return buildFileRefs(documentFileList, topic, uid).andThen(TopicError::fromError) {
        database.topic.savePlainTopic(topic, plain, it).onSuccess {
            addUserLog(uid, UserLogType.CREATE, topicInfo.tuple())
        }
    }.andThen(TopicError::fromError) {
        processTopicAfterCreate(topicInfo, uid)
    }.mapError(TopicError::fromError)
}

private suspend fun Backend.buildFileRefs(
    documentFileList: List<String>,
    topic: Topic,
    uid: PrimaryKey,
): CustomResult<List<FileRef>, TopicError> =
    database.file.getFileRecordByNames(documentFileList).map {
        it.associateBy { fileRecord ->
            fileRecord.fullName
        }
    }.map { fileRecordMap ->
        documentFileList.map { mediaName ->
            val fileRecord = fileRecordMap.getValue(mediaName)
            FileRef(
                id = SnowflakeFactory.nextId(),
                createdTime = now(),
                objectId = topic.id,
                objectType = ObjectType.TOPIC,
                author = uid,
                mediaName = mediaName,
                fileId = fileRecord.id,
            )
        }
    }.mapError(TopicError::fromError)

suspend fun Backend.createTopicAtRoom(
    newTopic: NewRoomTopic,
    uid: PrimaryKey,
): CustomResult<TopicCreated?, TopicError> {
    if (newTopic.parentType != ObjectType.TOPIC && newTopic.parentType != ObjectType.ROOM) {
        return CustomResult.Failure(TopicError.from(ForbiddenException())).mapError(TopicError::fromError)
    }
    return checkRootWritePermission(
        newTopic.parentType,
        newTopic.parentId,
        uid,
    ).andThenPresent(TopicError::fromError) {
        if (it.level >= 10) {
            CustomResult.Failure(TopicError.from(CustomBadRequestException("not support")))
        } else {
            database.room.checkRoomIsPrivate(it.rootId).andThenPresent(TopicError::fromError) { isPrivate ->
                createTopicAtRoom(uid, it, newTopic, isPrivate, newTopic.content)
            }
        }
    }.onPresent {
        addSubscription(uid, it.tuple())
    }.mapError(TopicError::fromError)
}

private suspend fun Backend.createTopicAtRoom(
    uid: PrimaryKey,
    permission: RootWritePermission,
    newTopic: NewRoomTopic,
    isPrivate: Boolean,
    content: TopicContent,
): CustomResult<TopicCreated?, TopicError> {
    if (isPrivate) {
        if (content !is TopicContent.Encrypted) {
            return CustomResult.Failure(
                TopicError.from(ForbiddenException("Private room only accept encrypted content.")),
            ).mapError(TopicError::fromError)
        }
        return saveEncryptedTopic(
            permission,
            content,
            uid,
            permission.tuple,
            newTopic.tuple,
        ).mapError(TopicError::fromError)
    }
    if (content !is TopicContent.Plain) {
        return CustomResult.Failure(
            TopicError.from(ForbiddenException("Public room only accept unencrypted content.")),
        ).mapError(TopicError::fromError)
    }
    return savePlainTopic(
        uid,
        content.plain,
        permission.level,
        newTopic.tuple,
        permission.tuple,
    ).mapError(TopicError::fromError)
}

private suspend fun Backend.saveEncryptedTopic(
    permission: RootWritePermission,
    content: TopicContent.Encrypted,
    uid: PrimaryKey,
    rootTuple: ObjectTuple,
    parentTuple: ObjectTuple,
) = isKeyVerified(permission.rootId, content.encryptedKey).andThen(TopicError::fromError) { isVerified ->
    if (isVerified) {
        val newId = SnowflakeFactory.nextId()
        val bytes = content.bytes
        val topic =
            Topic(
                newId,
                now(),
                uid,
                parentTuple.objectId,
                parentTuple.objectType,
                rootTuple.objectId,
                rootTuple.objectType,
                bytes,
                true,
                permission.level + 1,
                false,
                null,
            )
        database.topic.saveEncryptedTopic(topic, content).map {
            RawTopic(topic, content).toTopicInfo()
        }
    } else {
        CustomResult.Failure(TopicError.from(ForbiddenException("Key not found ${content.encryptedKey.size}")))
    }
}.andThenPresent(TopicError::fromError) { topicInfo ->
    processTopicAfterCreate(topicInfo, uid)
}

suspend fun Backend.processTopicAfterCreate(topicInfo: TopicInfo, uid: PrimaryKey) =
    captureFailure(TopicError::from) {
        val info =
            if (topicInfo.content is TopicContent.Plain) {
                processTopicFileObject(listOf(topicInfo)).firstOrNull().getOrThrow()
            } else {
                topicInfo
            }
        val authorInfo = getUserInfo(ObjectFetch.IdFetch(uid)).getOrThrow()
        info?.let { TopicCreated(it, authorInfo) }
    }

suspend fun Backend.createTopicSnapshot(uid: PrimaryKey, topicId: PrimaryKey): CustomResult<FileInfo?, TopicError> =
    checkTopicReadPermission(
        topicId,
        uid,
    ).andThenPresent(TopicError::fromError) { (hasRead, _, isPrivate) ->
        if (hasRead && !isPrivate) {
            database.getRawTopic(ObjectFetch.IdFetch(topicId), null)
        } else {
            CustomResult.Failure(TopicError.from(ForbiddenException("Permission denied")))
        }
    }.andThenPresent(TopicError::fromError) {
        processTopicActivities(listOf(it), uid)
    }.firstOrNull().andThenPresent(TopicError::fromError) { topicInfo ->
        getUserInfo(ObjectFetch.IdFetch(uid)).andThenPresent(TopicError::fromError) { userInfo ->
            createTopicSnapshot(topicInfo.topicInfo, userInfo, uid)
        }
    }.mapError(TopicError::fromError)

private suspend fun Backend.createTopicSnapshot(
    topicInfo: TopicInfo,
    creatorInfo: UserInfo,
    uid: PrimaryKey,
): CustomResult<FileInfo?, TopicError> {
    val topicId = topicInfo.id
    val name = "$uid/$topicId.pdf"
    val pdfFile = File(System.getProperty("java.io.tmpdir"), name)
    val signedFile = File(System.getProperty("java.io.tmpdir"), "${pdfFile.nameWithoutExtension}_signed.pdf")
    return try {
        getUserInfo(
            ObjectFetch.IdFetch(topicInfo.author),
        ).andThenPresent(TopicError::fromError) { userInfo ->
            generateSignedSnapshot(
                userInfo,
                creatorInfo,
                topicInfo,
                customConfig.snapshotKeyStore?.let {
                    SnapshotGeneration.KeyStoreGeneration(it.base64, it.pass, pdfFile, signedFile)
                } ?: SnapshotGeneration.SimpleGeneration(pdfFile),
            )
        }.andThenPresent(TopicError::fromError) {
            tryUploadFiles(
                uid,
                ObjectType.USER,
                listOf(
                    UploadPack(
                        pdfFile,
                        "$topicId.pdf",
                        pdfFile.length(),
                        "$uid/$topicId.pdf",
                        pdfFile.inputStream().buffered().use { input ->
                            sha256(input.asSource().buffered())
                        },
                    ),
                ),
            )
        }.firstOrNull()
    } finally {
        pdfFile.delete()
        signedFile.delete()
    }.mapError(TopicError::fromError)
}

@OptIn(ExperimentalUuidApi::class)
suspend fun Backend.generateSignedSnapshot(
    authorInfo: UserInfo,
    creatorInfo: UserInfo,
    topicInfo: TopicInfo,
    snapshotGeneration: SnapshotGeneration,
): CustomResult<Unit?, TopicError> {
    val content = topicInfo.content
    if (content !is TopicContent.Plain) {
        return CustomResult.Failure(
            TopicError.from(CustomBadRequestException("unsupported")),
        ).mapError(TopicError::fromError)
    }
    val saveToFile = snapshotGeneration.path
    val parent = saveToFile.parentFile ?: return CustomResult.success(null)
    if (!parent.exists() && !parent.mkdirs()) {
        return CustomResult.Failure(
            TopicError.from(Exception("failed")),
        ).mapError(TopicError::fromError)
    }
    val plainContent = content.plain
    val map = mutableMapOf<String, File>()
    val userHome = System.getProperty("user.home")
    val pdfService =
        ServiceLoader.load(PdfService::class.java).firstOrNull() ?: return CustomResult.Failure(
            TopicError.from(
                Exception("not register pdf service"),
            ),
        ).mapError(TopicError::fromError)
    return try {
        content.fileInfos.forEach {
            val targetFile = File(userHome, "a-temp/${Uuid.random()}")
            map[it.name] = targetFile
            objectStorageService.getInputStream(A_FILE_DEFAULT_BUCKET, it.fullName).getOrThrow()
                .buffered().use { input ->
                    targetFile.outputStream().buffered().use { output ->
                        input.copyTo(output)
                    }
                }
        }
        pdfService.generateSignedSnapshot(
            creatorInfo,
            authorInfo,
            plainContent,
            map,
            snapshotGeneration,
            PdfGenerationSpec(topicInfo.createdTime, now()),
        )
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        CustomResult.Failure(TopicError.from(e))
    } finally {
        map.forEach {
            it.value.delete()
        }
    }.mapError(TopicError::fromError)
}

suspend fun Backend.getTopic(
    topicId: PrimaryKey,
    uid: PrimaryKey?,
    fillHasCommented: Boolean?,
): CustomResult<TopicDetail?, TopicError> {
    if (uid == null && fillHasCommented == true) {
        return CustomResult.Failure(
            TopicError.from(UnauthorizedException()),
        ).mapError(TopicError::fromError)
    }
    return checkTopicReadPermission(
        topicId,
        uid,
    ).andThenPresent(TopicError::fromError) { permission ->
        if (permission.hasRead) {
            uncheckGetTopicById(topicId, uid)
        } else {
            CustomResult.Failure(TopicError.from(ForbiddenException("Permission Denied")))
        }
    }.mapError(TopicError::fromError)
}

suspend fun Backend.uncheckGetTopicById(topicId: PrimaryKey, uid: PrimaryKey?): CustomResult<TopicDetail?, TopicError> =
    database.getRawTopic(
        ObjectFetch.IdFetch(topicId),
        uid,
    ).andThenPresent(TopicError::fromError) { info ->
        processTopicDetails(listOf(info), uid)
    }.firstOrNull().mapError(TopicError::fromError)

suspend fun Backend.getTopicByAid(
    aid: String,
    uid: PrimaryKey?,
    fillHasCommented: Boolean?,
): CustomResult<TopicDetail?, TopicError> {
    if (uid == null && fillHasCommented == true) {
        return CustomResult.Failure(
            TopicError.from(UnauthorizedException()),
        ).mapError(TopicError::fromError)
    }
    return database.getRawTopic(ObjectFetch.AidFetch(aid), uid)
        .andThenPresent(TopicError::fromError) { info ->
            checkTopicReadPermission(
                info.topic.id,
                uid,
            ).andThenPresent(TopicError::fromError) { (hasRead, hasJoined) ->
                if (hasRead) {
                    CustomResult.success(info.copy(hasJoined = hasJoined))
                } else {
                    CustomResult.Failure(TopicError.from(ForbiddenException("Permission Denied")))
                }
            }
        }.andThenPresent(TopicError::fromError) { info ->
            processTopicDetails(listOf(info), uid)
        }.firstOrNull().mapError(TopicError::fromError)
}

suspend fun Backend.getTopicsByParentId(
    parentId: PrimaryKey,
    parentType: ObjectType,
    uid: PrimaryKey? = null,
    fillHasCommented: Boolean?,
    primaryKeyFetch: PrimaryKeyFetch,
    pinType: TopicPinSearch? = null,
): CustomResult<PaginationResult<TopicDetail>?, TopicError> {
    if (uid == null && fillHasCommented == true) {
        return CustomResult.Failure(
            TopicError.from(UnauthorizedException()),
        ).mapError(TopicError::fromError)
    }
    return checkRootReadPermission(
        parentType,
        parentId,
        uid,
    ).andThenPresent(TopicError::fromError) { (hasRead, _, isPrivate) ->
        if (isPrivate && !hasRead) {
            CustomResult.Failure(TopicError.from(ForbiddenException("Permission Denied")))
        } else {
            CustomResult.Success(Unit)
        }
    }.andThenPresent(TopicError::fromError) {
        uncheckGetTopicsByParentId(uid, parentId, primaryKeyFetch, pinType)
    }.mapError(TopicError::fromError)
}

suspend fun Backend.uncheckGetTopicsByParentId(
    uid: PrimaryKey?,
    parentId: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
    pinType: TopicPinSearch? = null,
) = database.getRawTopicByParentId(uid, primaryKeyFetch, parentId, pinType)
    .andThenPresent(TopicError::fromError) { (data, total) ->
        processTopicDetails(data, uid).paging(total)
    }

suspend fun Backend.processTopicDetails(topics: List<RawTopic>, uid: PrimaryKey?) =
    processTopicProjection(topics, uid, true)

suspend fun Backend.processTopicActivities(topics: List<RawTopic>, uid: PrimaryKey?) =
    processTopicProjection(topics, uid, false).mapPresent { details ->
        details.map { TopicActivity(it.topicInfo, it.authorInfo, it.reactions, it.roomInfo) }
    }

private suspend fun Backend.processTopicProjection(
    topics: List<RawTopic>,
    uid: PrimaryKey?,
    addLatestSubTopic: Boolean,
): CustomResult<List<TopicDetail>?, TopicError> {
    if (topics.isEmpty()) return CustomResult.success(emptyList())
    return captureFailure(TopicError::from) {
        val rooms = getRoomMapByTopics(topics)
        val subTopicsMap =
            if (addLatestSubTopic) {
                topics.flatMap { t ->
                    database.getLatestRawTopic(uid, t.topic.id).getOrThrow()
                }.groupBy {
                    it.topic.parentId
                }
            } else {
                emptyMap()
            }

        val uidList =
            topics.map {
                it.topic.author
            } +
                subTopicsMap.flatMap {
                    it.value
                }.map {
                    it.topic.author
                }.distinct()
        val userMap = getUserInfoList(ObjectListFetch.IdListFetch(uidList)).getOrThrow().associateBy { it.id }
        val processedSubTopic =
            subTopicsMap.mapValues {
                it.value.map { subTopic ->
                    TopicReplyPreview(subTopic.toTopicInfo(), userMap[subTopic.topic.author])
                }
            }
        val reactionMap = getReactionMap(topics, uid)
        topics.map { topic ->
            TopicDetail(
                topic.toTopicInfo(),
                userMap[topic.topic.author],
                subTopics = processedSubTopic[topic.topic.id].orEmpty().toImmutableList(),
                reactions = reactionMap[topic.topic.id].orEmpty().toImmutableList(),
                roomInfo = if (topic.topic.rootType == ObjectType.ROOM) rooms[topic.topic.rootId] else null,
            )
        }
    }.andThenPresent(TopicError::fromError) {
        val projections = it
        processTopicFileObject(projections.map { projection -> projection.topicInfo }).mapPresent { infos ->
            projections.zip(infos) { projection, info -> projection.copy(topicInfo = info) }
        }
    }.mapError(TopicError::fromError)
}

private suspend fun Backend.getReactionMap(
    topics: List<RawTopic>,
    uid: PrimaryKey?,
): Map<PrimaryKey, List<ReactionInfo>> =
    database.reaction.getReactionInfoPaginationResult(
        topics.map {
            it.topic.id
        },
        uid,
        ReactionFetch(null, 20),
    ).map {
        it.list
    }.map {
        it.groupBy(ReactionInfo::objectId)
    }.getOrThrow()

private suspend fun Backend.getRoomMapByTopics(topics: List<RawTopic>): Map<PrimaryKey, RoomInfo> {
    val roomIds =
        topics.mapNotNull {
            if (it.topic.rootType == ObjectType.ROOM) {
                it.topic.rootId
            } else {
                null
            }
        }
    return getRoomInfoList(ObjectListFetch.IdListFetch(roomIds)).getOrThrow().associateBy {
        it.id
    }
}

/**
 * 搜索用户主题.
 */
suspend fun Backend.searchUserTopics(
    userId: PrimaryKey,
    search: CustomApi.Topics.Users.Id.UserTopicSearchQuery,
    primaryKeyFetch: OffsetFetch,
    uid: PrimaryKey?,
): CustomResult<PaginationResult<TopicDetail>?, TopicError> {
    if (search.fillHasCommented == true && uid == null) {
        return CustomResult.Failure(
            TopicError.from(UnauthorizedException()),
        ).mapError(TopicError::fromError)
    }

    val word = search.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(PaginationResult(emptyList(), 0))
    }
    if (word.length > 20) {
        return CustomResult.Failure(
            TopicError.from(CustomBadRequestException("word too long")),
        ).mapError(TopicError::fromError)
    }

    // 创建TopicDocumentSearch.Topics对象进行用户主题搜索
    return topicSearchService.searchDocument(
        TopicDocumentSearch.Topics(userId, word, fetch = primaryKeyFetch),
    ).mapPagingResultNullable(TopicError::fromError) { list ->
        processTopicsDocument(uid, list)
    }.mapError(TopicError::fromError)
}

/**
 * 搜索房间主题.
 */
suspend fun Backend.searchRoomTopics(
    roomId: PrimaryKey,
    search: CustomApi.Topics.Rooms.Id.RoomTopicSearchQuery,
    primaryKeyFetch: OffsetFetch,
    uid: PrimaryKey?,
): CustomResult<PaginationResult<TopicDetail>?, TopicError> {
    if (search.fillHasCommented == true && uid == null) {
        return CustomResult.Failure(TopicError.from(UnauthorizedException())).mapError(TopicError::fromError)
    }
    val word = search.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(PaginationResult(emptyList(), 0))
    }
    if (word.length > 20) {
        return CustomResult.Failure(
            TopicError.from(CustomBadRequestException("word too long")),
        ).mapError(TopicError::fromError)
    }
    return checkRoomReadPermission(roomId, uid).andThenPresent(TopicError::fromError) {
        if (it.isPrivate) {
            CustomResult.Failure(TopicError.from(CustomBadRequestException("can't search in private room")))
        } else {
            CustomResult.success(Unit)
        }
    }.andThenPresent(TopicError::fromError) {
        // 创建TopicDocumentSearch.Topics对象进行房间主题搜索
        topicSearchService.searchDocument(
            TopicDocumentSearch.Topics(roomId, word, fetch = primaryKeyFetch),
        )
    }.mapPagingResultIfNotNullNullable(TopicError::fromError) { list ->
        processTopicsDocument(uid, list)
    }.mapError(TopicError::fromError)
}

/**
 * 搜索社区主题.
 */
suspend fun Backend.searchCommunityTopics(
    communityId: PrimaryKey,
    search: CustomApi.Topics.Communities.Id.CommunityTopicSearchQuery,
    primaryKeyFetch: OffsetFetch,
    uid: PrimaryKey?,
): CustomResult<PaginationResult<TopicDetail>?, TopicError> {
    if (search.fillHasCommented == true && uid == null) {
        return CustomResult.Failure(TopicError.from(UnauthorizedException())).mapError(TopicError::fromError)
    }
    val word = search.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(PaginationResult(emptyList(), 0))
    }
    if (word.length > 20) {
        return CustomResult.Failure(
            TopicError.from(CustomBadRequestException("word too long")),
        ).mapError(TopicError::fromError)
    }
    return checkCommunityReadPermission(communityId, uid).andThenPresent(TopicError::fromError) {
        if (it.isPrivate) {
            CustomResult.Failure(TopicError.from(CustomBadRequestException("can't search in private community")))
        } else {
            CustomResult.success(Unit)
        }
    }.andThenPresent(TopicError::fromError) {
        // 创建TopicDocumentSearch.Topics对象进行社区主题搜索
        topicSearchService.searchDocument(
            TopicDocumentSearch.Topics(communityId, word, fetch = primaryKeyFetch),
        )
    }.mapPagingResultIfNotNullNullable(TopicError::fromError) { list ->
        processTopicsDocument(uid, list)
    }.mapError(TopicError::fromError)
}

suspend fun Backend.processTopicFileObject(infos: List<TopicInfo>): CustomResult<List<TopicInfo>?, TopicError> {
    // id + mediaLink，此处的mediaLink 应该包含前缀，内部会自动添加前缀
    val fileList =
        documentFileList(infos).filter {
            val temp = File(it.second)
            temp.canonicalPath == temp.absolutePath
        }
    // 所有用到的media
    val fileNameList =
        fileList.map {
            it.second
        }.distinct()
    // 根据topicId 保存的mediaName 的map
    val fileMap = fileList.groupByPair()
    return getFileInfoList(fileNameList).mapPresent { fileInfos ->
        val mediaInfoMap = fileInfos.filterNotNull().associateBy { it.fullName }
        infos.map { info ->
            val content = info.content
            if (content is TopicContent.Plain) {
                val list =
                    fileMap[info.id]?.mapNotNull {
                        mediaInfoMap[it]
                    }.orEmpty()
                info.copy(content = content.copy(fileInfos = list))
            } else {
                info
            }
        }
    }.mapError(TopicError::fromError)
}

fun documentFileList(documentList: List<TopicInfo>): List<Pair<PrimaryKey, String>> =
    documentList.flatMap { document ->
        val markdownText = document.content
        if (markdownText is TopicContent.Plain) {
            val mediaLinks = extractMarkdownMediaLink(markdownText.plain)
            mediaLinks.map {
                val prefix = document.author
                document.id to "$prefix/$it"
            }
        } else {
            emptyList()
        }
    }.distinct()

suspend fun Backend.recommendTopics(
    uid: PrimaryKey?,
    primaryKeyFetch: OffsetFetch,
    q: CustomApi.Topics.RecommendQuery,
): CustomResult<PaginationResult<TopicDetail>?, TopicError> {
    if (uid == null && q.fillHasCommented == true) {
        return CustomResult.Failure(TopicError.from(UnauthorizedException())).mapError(TopicError::fromError)
    }
    return if (uid != null) {
        database.community.getJoinedCommunityIds(uid).andThen(TopicError::fromError) { joinedCommunityIds ->
            topicSearchService.searchDocument(
                topicDocumentSearch =
                TopicDocumentSearch.Recommend(
                    uid,
                    joinedCommunityIds,
                    fetch = primaryKeyFetch,
                ),
            )
        }
    } else {
        topicSearchService.searchDocument(
            topicDocumentSearch = TopicDocumentSearch.RecommendNotLogin(fetch = primaryKeyFetch),
        )
    }.mapPagingResultNullable(TopicError::fromError) { list ->
        processTopicsDocument(uid, list)
    }.mapError(TopicError::fromError)
}

private suspend fun Backend.processTopicsDocument(
    uid: PrimaryKey?,
    list: List<TopicDocument>,
): CustomResult<List<TopicDetail>?, TopicError> {
    val ids =
        list.map {
            it.id
        }
    if (ids.isEmpty()) {
        return CustomResult.success(emptyList())
    }
    return database.getRawTopicListByIds(uid, ids).andThen(TopicError::fromError) { infos ->
        val processedTopics =
            fixedSort(infos, ids) {
                it.topic.id
            }
        processTopicDetails(processedTopics, uid)
    }.mapError(TopicError::fromError)
}

suspend fun Backend.getTopicByIds(
    ids: List<PrimaryKey>,
    uid: PrimaryKey?,
): CustomResult<List<TopicDetail>?, TopicError> {
    if (ids.isEmpty()) {
        return CustomResult.success(emptyList())
    }
    try {
        ids.forEach {
            val permission = checkTopicReadPermission(it, uid).getOrThrow()
            if (permission == null || !permission.hasRead) {
                return CustomResult.Failure(
                    TopicError.from(ForbiddenException("Permission Denied")),
                ).mapError(TopicError::fromError)
            }
        }
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        return CustomResult.Failure(TopicError.from(e)).mapError(TopicError::fromError)
    }

    return database.getRawTopicListByIds(uid, ids).andThen(TopicError::fromError) { infos ->
        processTopicDetails(infos, uid)
    }.mapError(TopicError::fromError)
}

suspend fun Backend.updateTopicPin(uid: PrimaryKey, topicId: PrimaryKey, newValue: Boolean) =
    checkObjectWritable(ObjectType.TOPIC, topicId).andThenPresent(TopicError::fromError) {
        checkTopicAdminPermission(topicId, uid)
    }.andThenPresent(TopicError::fromError) {
        database.getRawTopic(ObjectFetch.IdFetch(topicId), uid)
    }.andThenPresent(TopicError::fromError) { rawTopic ->
        val topicInfo = rawTopic.toTopicInfo()
        if (rawTopic.topic.isPin == newValue) {
            CustomResult.success(topicInfo)
        } else {
            database.topic.updateTopicPinned(topicId, newValue).map { isSuccess ->
                if (isSuccess) {
                    topicInfo.copy(isPin = newValue)
                } else {
                    topicInfo
                }
            }
        }
    }

suspend fun Backend.getAllTopics(primaryKeyFetch: PrimaryKeyFetch) =
    database.getAllRawTopics(primaryKeyFetch).andThen(TopicError::fromError) { (topics, total) ->
        processTopicDetails(topics, uid = null).paging(total)
    }
