/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.alphaheart.api.AdminApi
import com.storyteller_f.alphaheart.api.CommonPath
import com.storyteller_f.alphaheart.api.NewUser
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.PanelLogsQuery
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignUpBody
import com.storyteller_f.alphaheart.api.TaskRecordSummaryListResponse
import com.storyteller_f.alphaheart.api.TaskRecordsQuery
import com.storyteller_f.alphaheart.api.TopicQuery
import com.storyteller_f.alphaheart.api.UpdateWorkerTaskBody
import com.storyteller_f.alphaheart.api.WorkerTaskListResponse
import com.storyteller_f.alphaheart.api.WorkerTaskPath
import com.storyteller_f.endpoint4k.ktor.client.invoke
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.obj.UpdateObjectStatusBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import io.ktor.http.ContentType
import io.ktor.http.contentType

suspend fun PanelSessionManager.getAllUsers(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.get(query)
    }

suspend fun PanelSessionManager.signUp(newUser: SignUpBody) =
    serviceCatching {
        AdminApi.signUp(newUser) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.signIn(signInPack: SignInBody) =
    serviceCatching {
        AdminApi.signIn(signInPack) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.restoreSession() =
    serviceCatching {
        val session = AdminApi.session()
        val expected = passHolder.currentUserPass?.address()?.getOrThrow()
        if (expected != null && session.address != expected) throw SessionIdentityChangedException()
        session.account
    }

suspend fun PanelSessionManager.signOut() =
    serviceCatching {
        AdminApi.signOut(Unit) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getData() =
    serviceCatching {
        AdminApi.getData()
    }

suspend fun PanelSessionManager.overview() =
    serviceCatching {
        AdminApi.overview()
    }

suspend fun PanelSessionManager.addUser(newUser: NewUser) =
    serviceCatching {
        AdminApi.Users.add(newUser) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getAllCommunities(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Communities.get(query)
    }

suspend fun PanelSessionManager.getAllPublicRooms(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Rooms.getPublic(query)
    }

suspend fun PanelSessionManager.getAllPrivateRooms(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Rooms.getPrivate(query)
    }

suspend fun PanelSessionManager.getAllTopics(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Topics.get(query)
    }

suspend fun PanelSessionManager.getAllTitles(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Titles.get(query)
    }

suspend fun PanelSessionManager.getAllFiles(query: PaginationQuery) =
    serviceCatching {
        AdminApi.Files.get(query)
    }

suspend fun PanelSessionManager.searchFiles(query: SearchQuery) =
    serviceCatching {
        AdminApi.Files.search(query)
    }

suspend fun PanelSessionManager.getUserById(uid: PrimaryKey) =
    serviceCatching {
        AdminApi.Users.Id.get(CommonPath(uid))
    }

suspend fun PanelSessionManager.updateUserStatus(
    uid: PrimaryKey,
    body: com.storyteller_f.shared.obj.UpdateUserStatusBody,
) = serviceCatching {
    AdminApi.Users.Id.Status.update(CommonPath(uid), body) {
        contentType(ContentType.Application.Json)
    }
}

suspend fun PanelSessionManager.getUserOverview(uid: PrimaryKey) =
    serviceCatching {
        AdminApi.Users.Id.Overview.get(CommonPath(uid))
    }

suspend fun PanelSessionManager.getUserJoinedCommunities(
    uid: PrimaryKey,
    query: com.storyteller_f.alphaheart.api.CustomApi.Users.JoinedCommunities.UserCommunitiesQuery,
) = serviceCatching {
    AdminApi.Users.Id.Communities.get(query, CommonPath(uid))
}

suspend fun PanelSessionManager.getUserJoinedRooms(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Rooms.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getUserReceivedTitles(
    uid: PrimaryKey,
    query: com.storyteller_f.alphaheart.api.CustomApi.Users.Id.Titles.TitleQuery,
) = serviceCatching {
    AdminApi.Users.Id.Titles.get(query, CommonPath(uid))
}

suspend fun PanelSessionManager.getUserFiles(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Files.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getRoomFiles(roomId: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Rooms.Id.Files.get(query, CommonPath(roomId))
    }

suspend fun PanelSessionManager.getUserLogs(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Logs.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getPanelLogs(targetId: PrimaryKey, objectType: ObjectType, query: PaginationQuery) =
    serviceCatching {
        AdminApi.PanelLogs.get(
            PanelLogsQuery(targetId, objectType, query.nextPageToken, query.prePageToken, query.size),
        )
    }

suspend fun PanelSessionManager.getTaskRecords(
    type: TaskRecordType?,
    query: PaginationQuery,
    isSuccess: Boolean? = null,
    failureType: String? = null,
) = serviceCatching {
    AdminApi.TaskRecords.get(
        TaskRecordsQuery(
            type = type,
            isSuccess = isSuccess,
            failureType = failureType,
            nextPageToken = query.nextPageToken,
            prePageToken = query.prePageToken,
            size = query.size,
        ),
    )
}

/** Retrieves aggregate task execution counts. */
suspend fun PanelSessionManager.getTaskRecordSummaries(): CustomResult<TaskRecordSummaryListResponse, RequestError> {
    val result =
        serviceCatching {
            AdminApi.TaskRecords.summary()
        }
    return result.mapError(RequestError::fromError)
}

/** Marks a failed task execution for retry. */
suspend fun PanelSessionManager.markTaskRecordForRetry(id: PrimaryKey): CustomResult<Unit, RequestError> {
    val result =
        serviceCatching {
            AdminApi.TaskRecords.Id.Retry.update(CommonPath(id), Unit)
        }
    return result.mapError(RequestError::fromError)
}

/** Retrieves all persisted worker task configurations. */
suspend fun PanelSessionManager.getWorkerTasks(): CustomResult<WorkerTaskListResponse, RequestError> {
    val result =
        serviceCatching {
            AdminApi.WorkerTasks.get()
        }
    return result.mapError(RequestError::fromError)
}

/** Creates or replaces one worker task configuration. */
suspend fun PanelSessionManager.updateWorkerTask(config: WorkerTask): CustomResult<WorkerTask, RequestError> {
    val result =
        serviceCatching {
            AdminApi.WorkerTasks.Type.update(
                WorkerTaskPath(config.type),
                UpdateWorkerTaskBody(
                    isEnabled = config.isEnabled,
                    fetchSize = config.fetchSize,
                    waitDurationMillis = config.waitDurationMillis,
                ),
            ) {
                contentType(ContentType.Application.Json)
            }
        }
    return result.mapError(RequestError::fromError)
}

suspend fun PanelSessionManager.getUserUploadRecords(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.UploadRecords.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getUserReactions(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Reactions.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getUserComments(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Comments.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getUserFavorites(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Favorites.get(query, CommonPath(uid))
    }

suspend fun PanelSessionManager.getUserSubscriptions(uid: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Users.Id.Subscriptions.get(query, CommonPath(uid))
    }

// Detail fetch for other entities via CustomApi
suspend fun PanelSessionManager.getCommunityById(id: PrimaryKey) =
    serviceCatching {
        AdminApi.Communities.Id.get(CommonPath(id))
    }

suspend fun PanelSessionManager.updateCommunityStatus(id: PrimaryKey, body: UpdateObjectStatusBody) =
    serviceCatching {
        AdminApi.Communities.Id.Status.update(CommonPath(id), body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getCommunityMembers(communityId: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Communities.Id.Members.get(query, CommonPath(communityId))
    }

suspend fun PanelSessionManager.getRoomById(id: PrimaryKey) =
    serviceCatching {
        AdminApi.Rooms.Id.get(CommonPath(id))
    }

suspend fun PanelSessionManager.updateRoomStatus(id: PrimaryKey, body: UpdateObjectStatusBody) =
    serviceCatching {
        AdminApi.Rooms.Id.Status.update(CommonPath(id), body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getRoomMembers(roomId: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Rooms.Id.Members.get(query, CommonPath(roomId))
    }

suspend fun PanelSessionManager.getTopicById(id: PrimaryKey) =
    serviceCatching {
        AdminApi.Topics.Id.get(CommonPath(id))
    }

suspend fun PanelSessionManager.updateTopicStatus(id: PrimaryKey, body: UpdateObjectStatusBody) =
    serviceCatching {
        AdminApi.Topics.Id.Status.update(CommonPath(id), body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getFileById(id: PrimaryKey) =
    serviceCatching {
        AdminApi.Files.Id.get(CommonPath(id))
    }

suspend fun PanelSessionManager.updateFileStatus(id: PrimaryKey, body: UpdateObjectStatusBody) =
    serviceCatching {
        AdminApi.Files.Id.Status.update(CommonPath(id), body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getFileRefs(id: PrimaryKey, query: PaginationQuery) =
    serviceCatching {
        AdminApi.Files.Id.Refs.get(query, CommonPath(id))
    }

suspend fun PanelSessionManager.getTitleById(id: PrimaryKey) =
    serviceCatching {
        AdminApi.Titles.Id.get(CommonPath(id))
    }

suspend fun PanelSessionManager.updateTitleStatus(id: PrimaryKey, body: UpdateObjectStatusBody) =
    serviceCatching {
        AdminApi.Titles.Id.Status.update(CommonPath(id), body) {
            contentType(ContentType.Application.Json)
        }
    }

suspend fun PanelSessionManager.getTopicTopics(
    topicId: PrimaryKey,
    pinType: TopicPinSearch,
    paginationQuery: PaginationQuery,
) = serviceCatching {
    AdminApi.Topics.Id.Topics.get(TopicQuery(pinType, fillHasCommented = null, paginationQuery), CommonPath(topicId))
}

suspend fun SessionManager<*>.recordSigninActivity(panel: Boolean) =
    serviceCatching {
        if (panel) AdminApi.activity(Unit) {} else com.storyteller_f.alphaheart.api.CustomApi.Accounts.activity(Unit) {}
    }

suspend fun UserSessionManager.signOutAll() =
    serviceCatching {
        com.storyteller_f.alphaheart.api.CustomApi.Accounts.signOutAll(Unit) {}
    }

suspend fun PanelSessionManager.signOutAll() = serviceCatching { AdminApi.signOutAll(Unit) {} }
