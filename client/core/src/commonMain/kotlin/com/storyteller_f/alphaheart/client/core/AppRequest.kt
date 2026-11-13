/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.alphaheart.api.CommonPath
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.DeleteReaction
import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewDevice
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewReaction
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.NewTopic
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignUpBody
import com.storyteller_f.alphaheart.api.TopicQuery
import com.storyteller_f.alphaheart.api.TotpCodeBody
import com.storyteller_f.endpoint4k.ktor.client.invoke
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.buildEncryptedTopicContent
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.QuotaDetail
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.model.UnreadRoomsResponse
import com.storyteller_f.shared.model.UserPubKeyInfo
import com.storyteller_f.shared.obj.ListResponse
import com.storyteller_f.shared.obj.NewRoomTopic
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.ObjectType.COMMUNITY
import com.storyteller_f.shared.type.ObjectType.ROOM
import com.storyteller_f.shared.type.ObjectType.TOPIC
import com.storyteller_f.shared.type.ObjectType.USER
import com.storyteller_f.shared.type.PrimaryKey
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.Input

suspend fun <R, U> SessionManager<U>.serviceCatching(block: suspend HttpClient.() -> R): CustomResult<R, RequestError> {
    val point = Exception("Service request call site")
    return try {
        val value = client.block()
        CustomResult.success(value)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: SessionIdentityChangedException) {
        model.sessionExpired.value = true
        failServiceRequest(point, e)
    } catch (e: ServerErrorException) {
        if (e.isSessionAuthenticationFailure && e.status == io.ktor.http.HttpStatusCode.Unauthorized) {
            model.sessionExpired.value = true
        }
        failServiceRequest(point, e)
    } catch (e: Throwable) {
        failServiceRequest(point, e)
    }.mapError(RequestError::fromError)
}

suspend fun UserSessionManager.getRoomInfo(id: PrimaryKey) =
    serviceCatching {
        CustomApi.Rooms.Id.get(CustomApi.Rooms.Id.RoomIdQuery(currentIsAlreadySignUp), CommonPath(id))
    }

suspend fun UserSessionManager.getRoomInfoByAid(aid: String) =
    serviceCatching {
        CustomApi.Rooms.Aid.get(CustomApi.Rooms.Aid.RoomAidQuery(aid, currentIsAlreadySignUp))
    }

suspend fun UserSessionManager.getRoomMembersPublicKeys(id: PrimaryKey, paginationQuery: PaginationQuery) =
    serviceCatching {
        CustomApi.Rooms.Id.Members.publicKeys(paginationQuery, CommonPath(id))
    }

suspend fun UserSessionManager.joinRoom(id: PrimaryKey) =
    serviceCatching {
        CustomApi.Rooms.Id.Members.join(CommonPath(id), Unit) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.joinCommunity(id: PrimaryKey) =
    serviceCatching {
        CustomApi.Communities.Id.Members.join(CommonPath(id), Unit) {}
    }

suspend fun UserSessionManager.getRoomTopics(
    roomId: PrimaryKey,
    pinType: TopicPinSearch? = null,
    paginationQuery: PaginationQuery,
) = serviceCatching {
    CustomApi.Rooms.Id.Topics.get(TopicQuery(pinType, currentIsAlreadySignUp, paginationQuery), CommonPath(roomId))
}

suspend fun UserSessionManager.getCommunityTopics(
    communityId: PrimaryKey,
    pinType: TopicPinSearch? = null,
    paginationQuery: PaginationQuery,
) = serviceCatching {
    CustomApi.Communities.Id.Topics.get(
        TopicQuery(pinType, currentIsAlreadySignUp, paginationQuery),
        CommonPath(communityId),
    )
}

suspend fun UserSessionManager.getUserTopics(
    userId: PrimaryKey,
    pinType: TopicPinSearch? = null,
    paginationQuery: PaginationQuery,
) = serviceCatching {
    CustomApi.Users.Id.Topics.get(TopicQuery(pinType, currentIsAlreadySignUp, paginationQuery), CommonPath(userId))
}

suspend fun UserSessionManager.getCommunityInfo(id: PrimaryKey) =
    serviceCatching {
        CustomApi.Communities.Id.get(CustomApi.Communities.Id.CommunityIdQuery(currentIsAlreadySignUp), CommonPath(id))
    }

suspend fun UserSessionManager.getCommunityInfoByAid(aid: String) =
    serviceCatching {
        CustomApi.Communities.Aid.get(CustomApi.Communities.Aid.CommunityAidQuery(aid, currentIsAlreadySignUp))
    }

suspend fun UserSessionManager.searchCommunity(
    size: Int,
    joinStatusSearch: JoinStatusSearch,
    word: String,
    target: PrimaryKey? = null,
    nextCommunityId: String? = null,
    hasPosterSearch: PosterSearch? = null,
) = serviceCatching {
    CustomApi.Communities.search(
        CustomApi.Communities.CommunitySearchQuery(
            joinStatusSearch,
            word,
            target,
            hasPosterSearch,
            nextCommunityId,
            size,
        ),
    )
}

suspend fun UserSessionManager.getUserCommunities(query: CustomApi.Users.JoinedCommunities.UserCommunitiesQuery) =
    serviceCatching {
        CustomApi.Users.JoinedCommunities.get(query)
    }

suspend fun UserSessionManager.getUserJoinedCommunities(
    userId: PrimaryKey,
    query: CustomApi.Users.JoinedCommunities.UserCommunitiesQuery,
) = serviceCatching {
    CustomApi.Users.Id.Communities.get(query, CommonPath(userId))
}

suspend fun UserSessionManager.getCommunityRooms(
    communityId: PrimaryKey,
    paginationQuery: CustomApi.Communities.Id.Rooms.CommunityRoomQuery,
) = serviceCatching {
    this@getCommunityRooms.decryptRoomList(CustomApi.Communities.Id.Rooms.get(paginationQuery, CommonPath(communityId)))
}

suspend fun UserSessionManager.searchCommunityRooms(
    communityId: PrimaryKey,
    word: String,
    joinStatusSearch: JoinStatusSearch,
    size: Int,
    nextRoomId: String?,
) = serviceCatching {
    this@searchCommunityRooms.decryptRoomList(
        CustomApi.Communities.Id.Rooms.search(
            CustomApi.Communities.Id.Rooms.CommunityRoomSearchQuery(word, joinStatusSearch, nextRoomId, size),
            CommonPath(communityId),
        ),
    )
}

suspend fun UserSessionManager.searchCommunityMembers(
    communityId: PrimaryKey,
    nextToken: String?,
    size: Int,
    word: String,
) = serviceCatching {
    if (word.isBlank()) {
        CustomApi.Communities.Id.Members.get(
            PaginationQuery(nextToken, size = size),
            CommonPath(communityId),
        )
    } else {
        CustomApi.Communities.Id.Members.search(
            SearchQuery(word, nextToken, size = size),
            CommonPath(communityId),
        )
    }
}

suspend fun UserSessionManager.searchAllMembers(nextUserId: String?, size: Int, word: String) =
    serviceCatching {
        CustomApi.Users.search(SearchQuery(word, nextUserId, size))
    }

suspend fun UserSessionManager.searchRoomMembers(roomId: PrimaryKey, nextToken: String?, size: Int, word: String) =
    serviceCatching {
        if (word.isBlank()) {
            CustomApi.Rooms.Id.Members.get(
                PaginationQuery(nextToken, size = size),
                CommonPath(roomId),
            )
        } else {
            CustomApi.Rooms.Id.Members.search(
                SearchQuery(word, nextToken, size = size),
                CommonPath(roomId),
            )
        }
    }

suspend fun UserSessionManager.getRecommendTopics(paginationQuery: PaginationQuery) =
    serviceCatching {
        CustomApi.Topics.recommend(
            CustomApi.Topics.RecommendQuery(
                currentIsAlreadySignUp,
                paginationQuery.nextPageToken,
                paginationQuery.size,
                paginationQuery.prePageToken,
            ),
        )
    }

suspend fun UserSessionManager.getUserInfo(id: PrimaryKey) =
    serviceCatching {
        CustomApi.Users.Id.get(CommonPath(id))
    }

suspend fun UserSessionManager.updateUserInfo(newInfo: UpdateUserBody) =
    serviceCatching {
        CustomApi.Users.update(newInfo) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.getUserInfoByAid(aid: String) =
    serviceCatching {
        CustomApi.Users.Aid.get(CustomApi.Users.Aid.UserAidQuery(aid))
    }

suspend fun UserSessionManager.getTopicTopics(
    topicId: PrimaryKey,
    pinType: TopicPinSearch,
    paginationQuery: PaginationQuery,
) = serviceCatching {
    CustomApi.Topics.Id.Topics.get(TopicQuery(pinType, currentIsAlreadySignUp, paginationQuery), CommonPath(topicId))
}

suspend fun UserSessionManager.getTopicInfo(id: PrimaryKey) =
    serviceCatching {
        CustomApi.Topics.Id.get(CustomApi.Topics.Id.TopicIdQuery(currentIsAlreadySignUp), CommonPath(id))
    }

suspend fun UserSessionManager.getTopicInfoByAid(aid: String) =
    serviceCatching {
        CustomApi.Topics.Aid.get(CustomApi.Topics.Aid.TopicAidQuery(aid, currentIsAlreadySignUp))
    }

suspend fun UserSessionManager.getUserRooms(paginationQuery: PaginationQuery) =
    serviceCatching {
        this@getUserRooms.decryptRoomList(CustomApi.Users.JoinedRooms.get(paginationQuery))
    }

suspend fun UserSessionManager.searchCurrentUserRooms(word: String, size: Int, nextRoomId: String?) =
    serviceCatching {
        this@searchCurrentUserRooms.decryptRoomList(
            CustomApi.Users.JoinedRooms.search(
                CustomApi.Users.JoinedRooms.UserRoomsSearchQuery(word, nextRoomId, size),
            ),
        )
    }

suspend fun UserSessionManager.createTopic(objectType: ObjectType, objectId: PrimaryKey, input: String) =
    serviceCatching {
        CustomApi.Topics.add(NewTopic(objectType, objectId, input)) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.signUp(body: SignUpBody) =
    serviceCatching {
        CustomApi.Accounts.signUp(body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.signIn(body: SignInBody) =
    serviceCatching {
        CustomApi.Accounts.signIn(body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.signInTotp(code: String) =
    serviceCatching {
        CustomApi.Accounts.signInTotp(TotpCodeBody(code)) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.getData() =
    serviceCatching {
        CustomApi.Accounts.getData()
    }

suspend fun UserSessionManager.createTopicSnapshot(topicId: PrimaryKey) =
    serviceCatching {
        CustomApi.Topics.Id.createSnapshot(CommonPath(topicId), Unit) {
        }
    }

/**
 * 搜索用户主题.
 */
suspend fun UserSessionManager.searchUserTopics(
    userId: PrimaryKey,
    size: Int,
    word: String,
    nextTopicId: String? = null,
    prePageToken: String? = null,
    fillHasCommented: Boolean? = null,
) = serviceCatching {
    CustomApi.Topics.Users.Id.search(
        CustomApi.Topics.Users.Id.UserTopicSearchQuery(
            word,
            nextTopicId,
            prePageToken,
            size,
            fillHasCommented,
        ),
        CommonPath(userId),
    )
}

/**
 * 搜索房间主题.
 */
suspend fun UserSessionManager.searchRoomTopics(
    roomId: PrimaryKey,
    size: Int,
    word: String,
    nextTopicId: String? = null,
    prePageToken: String? = null,
    fillHasCommented: Boolean? = null,
) = serviceCatching {
    CustomApi.Topics.Rooms.Id.search(
        CustomApi.Topics.Rooms.Id.RoomTopicSearchQuery(
            word,
            nextTopicId,
            prePageToken,
            size,
            fillHasCommented,
        ),
        CommonPath(roomId),
    )
}

/**
 * 搜索社区主题.
 */
suspend fun UserSessionManager.searchCommunityTopics(
    communityId: PrimaryKey,
    size: Int,
    word: String,
    nextTopicId: String? = null,
    prePageToken: String? = null,
    fillHasCommented: Boolean? = null,
) = serviceCatching {
    CustomApi.Topics.Communities.Id.search(
        CustomApi.Topics.Communities.Id.CommunityTopicSearchQuery(
            word,
            nextTopicId,
            prePageToken,
            size,
            fillHasCommented,
        ),
        CommonPath(communityId),
    )
}

suspend fun UserSessionManager.exitRoom(roomId: PrimaryKey) =
    serviceCatching {
        CustomApi.Rooms.Id.Members.leave(CommonPath(roomId), Unit) {}
    }

suspend fun UserSessionManager.exitCommunity(communityId: PrimaryKey) =
    serviceCatching {
        CustomApi.Communities.Id.Members.leave(CommonPath(communityId), Unit) {}
    }

suspend fun UserSessionManager.addReaction(topicId: PrimaryKey, emoji: String) =
    serviceCatching {
        CustomApi.Topics.Id.Reactions.add(CommonPath(topicId), NewReaction(emoji)) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.deleteReaction(emoji: String, objectId: PrimaryKey) =
    serviceCatching {
        CustomApi.Topics.Id.Reactions.delete(CommonPath(objectId), DeleteReaction(emoji)) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.getReactions(topicId: PrimaryKey, size: Int, nextCursor: String? = null) =
    serviceCatching {
        CustomApi.Topics.Id.Reactions.get(
            CustomApi.Topics.Id.Reactions.ReactionQuery(currentIsAlreadySignUp, nextCursor, size = size),
            CommonPath(topicId),
        )
    }

suspend fun UserSessionManager.restoreSession() =
    serviceCatching {
        val user = CustomApi.Accounts.session()
        val expected = passHolder.currentUserPass?.address()?.getOrThrow()
        if (expected != null && user.address != expected) throw SessionIdentityChangedException()
        user
    }

suspend fun UserSessionManager.signOut() =
    serviceCatching {
        CustomApi.Accounts.signOut(Unit) {}
    }

suspend fun UserSessionManager.getTwoFactorSettings() =
    serviceCatching {
        CustomApi.Users.TwoFactor.get()
    }

suspend fun UserSessionManager.setupTotp() =
    serviceCatching {
        CustomApi.Users.TwoFactor.Totp.setup(Unit) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.enableTotp(code: String) =
    serviceCatching {
        CustomApi.Users.TwoFactor.Totp.enable(TotpCodeBody(code)) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.disableTwoFactor() =
    serviceCatching {
        CustomApi.Users.TwoFactor.disable(Unit) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.generateRecoveryCodes() =
    serviceCatching {
        CustomApi.Users.TwoFactor.recoveryCodes(Unit) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.getFileList(objectId: PrimaryKey, objectType: ObjectType, nextId: String?, size: Int) =
    serviceCatching {
        val query = PaginationQuery(nextId, null, size)
        when (objectType) {
            USER -> CustomApi.Users.Id.Files.get(query, CommonPath(objectId))
            ROOM -> CustomApi.Rooms.Id.Files.get(query, CommonPath(objectId))
            COMMUNITY -> CustomApi.Communities.Id.Files.get(query, CommonPath(objectId))
            else -> throw IllegalArgumentException("Unsupported object type: $objectType")
        }
    }

suspend fun UserSessionManager.searchFiles(query: SearchQuery, objectId: PrimaryKey, objectType: ObjectType) =
    serviceCatching {
        when (objectType) {
            USER -> CustomApi.Users.Id.Files.search(query, CommonPath(objectId))
            ROOM -> CustomApi.Rooms.Id.Files.search(query, CommonPath(objectId))
            COMMUNITY -> CustomApi.Communities.Id.Files.search(query, CommonPath(objectId))
            else -> throw IllegalArgumentException("Unsupported object type: $objectType")
        }
    }

suspend fun UserSessionManager.getMediaByName(word: String, objectId: PrimaryKey, objectType: ObjectType) =
    serviceCatching {
        CustomApi.Files.getByName(CustomApi.Files.MediaSearchQuery(word, objectId, objectType))
    }

suspend fun UserSessionManager.getFileRefs(fileId: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        CustomApi.Files.Id.Refs.get(query, CommonPath(fileId))
    }

data class UploadData(
    val size: Long,
    val name: String,
    val contentType: ContentType,
    val sha256: String,
    val block: () -> Input,
)

suspend fun UserSessionManager.upload(
    objectTuple: ObjectTuple,
    data: UploadData,
    onUpload: suspend (Long, Long?) -> Unit = { _, _ -> },
) = serviceCatching {
    CustomApi.Files.upload(
        CustomApi.Files.UploadQuery(
            objectTuple.objectId,
            objectTuple.objectType,
            data.sha256,
        ),
        Unit,
    ) {
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("description", "a_file")
                    appendInput(
                        "file",
                        Headers.build {
                            append(HttpHeaders.ContentType, data.contentType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${data.name}\"")
                        },
                        data.size,
                        data.block,
                    )
                },
                boundary = "WebAppBoundary",
            ),
        )
        onUpload { bytesSentTotal, contentLength ->
            onUpload(bytesSentTotal, contentLength)
        }
    }
}

// Chunked upload APIs
suspend fun UserSessionManager.initChunkUpload(
    objectTuple: ObjectTuple,
    name: String,
    size: Long,
    contentType: ContentType,
    chunkSize: Long,
    sha256: String,
    crc32c: String,
) = serviceCatching {
    CustomApi.Files.Chunks.init(
        CustomApi.Files.Chunks.InitBody(
            objectTuple.objectId,
            objectTuple.objectType,
            name,
            size,
            contentType.contentType,
            chunkSize,
            sha256,
            crc32c,
        ),
    ) {
        contentType(ContentType.Application.Json)
    }
}

suspend fun UserSessionManager.uploadChunk(
    recordId: PrimaryKey,
    index: Int,
    input: Input,
    hash: String,
    onUpload: suspend (Long, Long?) -> Unit = { _, _ -> },
) = serviceCatching {
    CustomApi.Files.Chunks.upload(
        CustomApi.Files.Chunks.UploadQuery(hash),
        CustomApi.Files.Chunks.UploadPath(recordId, index),
        Unit,
    ) {
        setBody(ByteReadChannel(input))
        onUpload { bytesSentTotal, contentLength ->
            onUpload(bytesSentTotal, contentLength)
        }
    }
}

suspend fun UserSessionManager.completeChunkUpload(recordId: PrimaryKey) =
    serviceCatching {
        CustomApi.Files.Chunks.complete(
            CommonPath(recordId),
            Unit,
        ) {
        }
    }

suspend fun UserSessionManager.abortChunkUpload(recordId: PrimaryKey) =
    serviceCatching {
        CustomApi.Files.Chunks.abort(
            CommonPath(recordId),
            Unit,
        ) {
        }
    }

suspend fun UserSessionManager.getChunkStatus(recordId: PrimaryKey) =
    serviceCatching {
        CustomApi.Files.Chunks.status(CommonPath(recordId))
    }

suspend fun UserSessionManager.copy(mediaId: PrimaryKey) =
    serviceCatching {
        CustomApi.Files.Id.copy(CommonPath(mediaId), Unit) {
        }
    }

@OptIn(ExperimentalStdlibApi::class)
suspend fun DefaultClientWebSocketSession.sendMessage(
    parentTarget: ObjectTuple,
    isPrivate: Boolean,
    input: String,
    keyData: List<UserPubKeyInfo>,
) {
    val content =
        if (isPrivate) {
            buildEncryptedTopicContent(input, keyData)
        } else {
            TopicContent.Plain(input)
        }
    val message = RoomFrame.Message(NewRoomTopic(parentTarget.objectType, parentTarget.objectId, content))
    sendFrame(message)
}

suspend fun UserSessionManager.userTitles(
    uid: PrimaryKey,
    size: Int,
    searchType: TitleSearchType,
    nextId: String? = null,
    status: TitleWorkStatus? = null,
    type: TitleType? = null,
    scopeId: PrimaryKey? = null,
): CustomResult<ListResponse<TitleDetail>, RequestError> =
    serviceCatching {
        CustomApi.Users.Id.Titles.get(
            CustomApi.Users.Id.Titles.TitleQuery(searchType, type, scopeId, status, nextId, size),
            CommonPath(uid),
        )
    }.mapError(RequestError::fromError)

suspend fun UserSessionManager.createTitle(newTitle: NewTitle) =
    serviceCatching {
        CustomApi.Titles.add(newTitle) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.createCommunity(newCommunity: NewCommunity) =
    serviceCatching {
        CustomApi.Communities.add(newCommunity) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.createRoom(newRoom: NewRoom) =
    serviceCatching {
        CustomApi.Rooms.add(newRoom) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.pinTopic(topicId: PrimaryKey) =
    serviceCatching {
        CustomApi.Topics.Id.pin(CommonPath(topicId), Unit) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.unpinTopic(topicId: PrimaryKey) =
    serviceCatching {
        CustomApi.Topics.Id.unpin(CommonPath(topicId), Unit) {
        }
    }

suspend fun UserSessionManager.updateCommunityInfo(id: PrimaryKey, newInfo: UpdateCommunityBody) =
    serviceCatching {
        CustomApi.Communities.Id.update(CommonPath(id), newInfo) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.updateRoomInfo(id: PrimaryKey, newInfo: UpdateRoomBody) =
    serviceCatching {
        CustomApi.Rooms.Id.update(CommonPath(id), newInfo) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.getTopicList(
    type: ObjectType,
    id: PrimaryKey,
    pinSearch: TopicPinSearch,
    paginationQuery: PaginationQuery,
) = when (type) {
    ROOM -> getRoomTopics(id, pinSearch, paginationQuery)
    COMMUNITY -> getCommunityTopics(id, pinSearch, paginationQuery)
    USER -> getUserTopics(id, pinSearch, paginationQuery)
    TOPIC -> getTopicTopics(id, pinSearch, paginationQuery)
    else -> CustomResult.Failure(RequestError.from(IllegalArgumentException("unrecognized $type")))
}

suspend fun UserSessionManager.addReadLog(info: UpdateUserRead): CustomResult<Unit, RequestError> =
    serviceCatching {
        CustomApi.Users.Read.add(info) {
            contentType(ContentType.Application.Json)
        }
    }.mapError(RequestError::fromError)

suspend fun UserSessionManager.addDevice(endpointUrl: String) =
    serviceCatching {
        CustomApi.Users.Devices.add(NewDevice(endpointUrl)) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun UserSessionManager.getReactionRecords(query: PaginationQuery) =
    serviceCatching {
        CustomApi.Users.ReactionRecords.get(query)
    }

suspend fun UserSessionManager.getComments(query: PaginationQuery) =
    serviceCatching {
        CustomApi.Users.Comments.get(query)
    }

suspend fun UserSessionManager.extractAlbum(mediaId: PrimaryKey) =
    serviceCatching {
        CustomApi.Files.Id.extractAlbum(CommonPath(mediaId), Unit) {}
    }

suspend fun UserSessionManager.addChildAccount(
    childAlgoType: com.storyteller_f.shared.model.AlgoType = com.storyteller_f.shared.model.AlgoType.P256,
): CustomResult<ChildAccountInfo, RequestError> {
    val userPass =
        passHolder.currentUserPass ?: return CustomResult.Failure(
            RequestError.from(IllegalStateException("User not authenticated")),
        ).mapError(RequestError::fromError)
    return serviceCatching {
        val request = userPass.encryptChildAccount(childAlgoType).getOrThrow()
        CustomApi.Accounts.ChildAccounts.add(request) {
            contentType(ContentType.Application.Json)
        }
    }.mapError(RequestError::fromError)
}

suspend fun UserSessionManager.getChildAccounts(nextId: String?, size: Int) =
    serviceCatching {
        CustomApi.Accounts.ChildAccounts.get(CustomApi.Accounts.ChildAccounts.ChildAccountQuery(nextId, size))
    }

suspend fun UserSessionManager.addFavorite(newFavorite: NewFavorite) =
    serviceCatching {
        val commonPath = CommonPath(newFavorite.objectId)
        when (newFavorite.objectType) {
            USER -> CustomApi.Users.Id.Favorite.add(commonPath, Unit) {}
            TOPIC -> CustomApi.Topics.Id.Favorite.add(commonPath, Unit) {}
            COMMUNITY -> CustomApi.Communities.Id.Favorite.add(commonPath, Unit) {}
            ROOM -> CustomApi.Rooms.Id.Favorite.add(commonPath, Unit) {}
            ObjectType.TITLE -> CustomApi.Titles.Id.Favorite.add(commonPath, Unit) {}
            ObjectType.FILE -> CustomApi.Files.Id.Favorite.add(commonPath, Unit) {}
            else -> throw IllegalArgumentException("Unsupported object type for favorite")
        }
    }

suspend fun UserSessionManager.removeFavorite(objectId: PrimaryKey, objectType: ObjectType) =
    serviceCatching {
        val commonPath = CommonPath(objectId)
        when (objectType) {
            USER -> CustomApi.Users.Id.Favorite.delete(commonPath, Unit) {}
            TOPIC -> CustomApi.Topics.Id.Favorite.delete(commonPath, Unit) {}
            COMMUNITY -> CustomApi.Communities.Id.Favorite.delete(commonPath, Unit) {}
            ROOM -> CustomApi.Rooms.Id.Favorite.delete(commonPath, Unit) {}
            ObjectType.TITLE -> CustomApi.Titles.Id.Favorite.delete(commonPath, Unit) {}
            ObjectType.FILE -> CustomApi.Files.Id.Favorite.delete(commonPath, Unit) {}
            else -> throw IllegalArgumentException("Unsupported object type for favorite")
        }
    }

suspend fun UserSessionManager.getFavorites(paginationQuery: PaginationQuery) =
    serviceCatching {
        CustomApi.Users.Favorites.get(paginationQuery)
    }

suspend fun UserSessionManager.addSubscription(newFavorite: NewSubscription) =
    serviceCatching {
        val commonPath = CommonPath(newFavorite.objectId)
        when (newFavorite.objectType) {
            USER -> CustomApi.Users.Id.Subscription.add(commonPath, Unit) {}
            TOPIC -> CustomApi.Topics.Id.Subscription.add(commonPath, Unit) {}
            COMMUNITY -> CustomApi.Communities.Id.Subscription.add(commonPath, Unit) {}
            ROOM -> CustomApi.Rooms.Id.Subscription.add(commonPath, Unit) {}
            ObjectType.TITLE -> CustomApi.Titles.Id.Subscription.add(commonPath, Unit) {}
            ObjectType.FILE -> CustomApi.Files.Id.Subscription.add(commonPath, Unit) {}
            else -> throw IllegalArgumentException("Unsupported object type for subscription")
        }
    }

suspend fun UserSessionManager.removeSubscription(objectId: PrimaryKey, objectType: ObjectType) =
    serviceCatching {
        val commonPath = CommonPath(objectId)
        when (objectType) {
            USER -> CustomApi.Users.Id.Subscription.delete(commonPath, Unit) {}
            TOPIC -> CustomApi.Topics.Id.Subscription.delete(commonPath, Unit) {}
            COMMUNITY -> CustomApi.Communities.Id.Subscription.delete(commonPath, Unit) {}
            ROOM -> CustomApi.Rooms.Id.Subscription.delete(commonPath, Unit) {}
            ObjectType.TITLE -> CustomApi.Titles.Id.Subscription.delete(commonPath, Unit) {}
            ObjectType.FILE -> CustomApi.Files.Id.Subscription.delete(commonPath, Unit) {}
            else -> throw IllegalArgumentException("Unsupported object type for subscription")
        }
    }

suspend fun UserSessionManager.getSubscriptions(paginationQuery: PaginationQuery) =
    serviceCatching {
        CustomApi.Users.Subscriptions.get(paginationQuery)
    }

suspend fun UserSessionManager.getUserOverview() =
    serviceCatching {
        CustomApi.Users.overview()
    }
suspend fun UserSessionManager.getQuotaInfo(
    objectTuple: ObjectTuple,
    quotaType: QuotaType = QuotaType.FILE,
): CustomResult<QuotaDetail, RequestError> =
    serviceCatching {
        CustomApi.Files.quota(CustomApi.Files.QuotaQuery(objectTuple.objectId, objectTuple.objectType, quotaType))
    }.mapError(RequestError::fromError)

suspend fun UserSessionManager.getFileInfo(fileId: PrimaryKey) =
    serviceCatching {
        CustomApi.Files.Id.get(CommonPath(fileId))
    }

suspend fun UserSessionManager.hasUnreadRooms(): CustomResult<UnreadRoomsResponse, RequestError> =
    serviceCatching {
        CustomApi.Users.Unread.hasUnreadRooms()
    }.mapError(RequestError::fromError)

internal class SessionIdentityChangedException : Exception("Login identity changed; explicit sign-in is required")

private fun <R> failServiceRequest(point: Exception, failure: Throwable): CustomResult<R, RequestError> {
    point.addSuppressed(failure)
    Napier.e(point) { "serviceCatching" }
    return CustomResult.Failure(RequestError.from(failure)).mapError(RequestError::fromError)
}
