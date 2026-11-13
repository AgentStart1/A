/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.AdminApi
import com.storyteller_f.alphaheart.api.CommunityInfoListResponse
import com.storyteller_f.alphaheart.api.CommunityMembershipListResponse
import com.storyteller_f.alphaheart.api.FileInfoListResponse
import com.storyteller_f.alphaheart.api.FileRefInfoListResponse
import com.storyteller_f.alphaheart.api.MemberInfoListResponse
import com.storyteller_f.alphaheart.api.PanelLogInfoListResponse
import com.storyteller_f.alphaheart.api.ReactionRecordInfoListResponse
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.TaskRecordInfoListResponse
import com.storyteller_f.alphaheart.api.TaskRecordSummaryListResponse
import com.storyteller_f.alphaheart.api.TitleInfoListResponse
import com.storyteller_f.alphaheart.api.TopicActivityListResponse
import com.storyteller_f.alphaheart.api.TopicInfoListResponse
import com.storyteller_f.alphaheart.api.UploadRecordInfoListResponse
import com.storyteller_f.alphaheart.api.UserFavoriteInfoListResponse
import com.storyteller_f.alphaheart.api.UserInfoListResponse
import com.storyteller_f.alphaheart.api.UserLogInfoListResponse
import com.storyteller_f.alphaheart.api.UserSubscriptionInfoListResponse
import com.storyteller_f.alphaheart.api.WorkerTaskListResponse
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.types.toPanelAccountInfo
import com.storyteller_f.alphaheart.cloud.core.service.addUser
import com.storyteller_f.alphaheart.cloud.core.service.adminSignIn
import com.storyteller_f.alphaheart.cloud.core.service.adminSignUp
import com.storyteller_f.alphaheart.cloud.core.service.getAllCommunities
import com.storyteller_f.alphaheart.cloud.core.service.getAllFileInfos
import com.storyteller_f.alphaheart.cloud.core.service.getAllPrivateRooms
import com.storyteller_f.alphaheart.cloud.core.service.getAllPublicRooms
import com.storyteller_f.alphaheart.cloud.core.service.getAllTitles
import com.storyteller_f.alphaheart.cloud.core.service.getAllTopics
import com.storyteller_f.alphaheart.cloud.core.service.getAllUsers
import com.storyteller_f.alphaheart.cloud.core.service.getCommunity
import com.storyteller_f.alphaheart.cloud.core.service.getCommunityMemberInfos
import com.storyteller_f.alphaheart.cloud.core.service.getFavorites
import com.storyteller_f.alphaheart.cloud.core.service.getFileInfoById
import com.storyteller_f.alphaheart.cloud.core.service.getFileInfoPaginationResult
import com.storyteller_f.alphaheart.cloud.core.service.getOverview
import com.storyteller_f.alphaheart.cloud.core.service.getPanelLogs
import com.storyteller_f.alphaheart.cloud.core.service.getRoomInfo
import com.storyteller_f.alphaheart.cloud.core.service.getTaskRecords
import com.storyteller_f.alphaheart.cloud.core.service.getTitleInfo
import com.storyteller_f.alphaheart.cloud.core.service.getUserById
import com.storyteller_f.alphaheart.cloud.core.service.getUserCommentedTopics
import com.storyteller_f.alphaheart.cloud.core.service.getUserJoinedCommunities
import com.storyteller_f.alphaheart.cloud.core.service.getUserJoinedRooms
import com.storyteller_f.alphaheart.cloud.core.service.getUserLogs
import com.storyteller_f.alphaheart.cloud.core.service.getUserOverview
import com.storyteller_f.alphaheart.cloud.core.service.getUserReactions
import com.storyteller_f.alphaheart.cloud.core.service.getUserSubscriptions
import com.storyteller_f.alphaheart.cloud.core.service.getUserTitles
import com.storyteller_f.alphaheart.cloud.core.service.getUserUploadRecords
import com.storyteller_f.alphaheart.cloud.core.service.uncheckGetTopicById
import com.storyteller_f.alphaheart.cloud.core.service.uncheckGetTopicsByParentId
import com.storyteller_f.alphaheart.cloud.core.service.uncheckedGetFileRefsByFileId
import com.storyteller_f.alphaheart.cloud.core.service.uncheckedGetRoomMemberInfos
import com.storyteller_f.alphaheart.cloud.core.service.uncheckedSearchFiles
import com.storyteller_f.alphaheart.cloud.core.service.updateCommunityStatus
import com.storyteller_f.alphaheart.cloud.core.service.updateFileStatus
import com.storyteller_f.alphaheart.cloud.core.service.updateRoomStatus
import com.storyteller_f.alphaheart.cloud.core.service.updateTitleStatus
import com.storyteller_f.alphaheart.cloud.core.service.updateTopicStatus
import com.storyteller_f.alphaheart.cloud.core.service.updateUserStatus
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.alphaheart.cloud.server.auth.beginSigninChallenge
import com.storyteller_f.alphaheart.cloud.server.auth.clearAllSigninSessions
import com.storyteller_f.alphaheart.cloud.server.auth.clearSigninSession
import com.storyteller_f.alphaheart.cloud.server.auth.consumeSigninChallenge
import com.storyteller_f.alphaheart.cloud.server.auth.getData
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.isSigninSessionValid
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.IdentifiablePagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.alphaheart.cloud.server.common.pagination
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.map
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.WorkerTask
import io.ktor.server.routing.Route
import io.ktor.server.routing.Routing
import kotlinx.collections.immutable.toImmutableList

fun Route.bindProtectedAdminRoute(backend: Backend) {
    bindAdminUserRoutes(backend)
    bindAdminCommunityRoutes(backend)
    bindAdminRoomRoutes(backend)
    bindAdminTopicRoutes(backend)
    bindAdminTitleRoutes(backend)
    bindAdminFileRoutes(backend)
    bindAdminPanelLogRoutes(backend)
    bindAdminTaskRecordRoutes(backend)
    bindAdminWorkerTaskRoutes(backend)
    AdminApi.session(handleResult(backend)) {
        usePrincipal { uid ->
            backend.database.panelAccount.getPanelAccount(
                uid,
            ).map {
                it?.let { account ->
                    com.storyteller_f.alphaheart.api.PanelSigninSessionInfo(
                        account.toPanelAccountInfo(),
                        account.address,
                    )
                }
            }
        }
    }
    AdminApi.signOutAll(handleResult(backend)) {
        call.clearAllSigninSessions()
        CustomResult.Success(Unit)
    }
    AdminApi.activity(handleResult(backend)) {
        if (call.isSigninSessionValid(
                active = true,
            )
        ) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(ServerError.from(com.storyteller_f.alphaheart.backend.core.UnauthorizedException()))
        }
    }
    AdminApi.overview(handleResult(backend)) {
        backend.getOverview()
    }
}

private fun Route.bindAdminWorkerTaskRoutes(backend: Backend) {
    AdminApi.WorkerTasks.get(handleResult(backend)) {
        backend.database.workerTask.getWorkerTasks().map { configs ->
            WorkerTaskListResponse(configs.toImmutableList())
        }
    }
    AdminApi.WorkerTasks.Type.update(handleResult(backend)) { path, api ->
        usePrincipal {
            val body = api.receiveBody()
            val config =
                WorkerTask(
                    type = path.type,
                    isEnabled = body.isEnabled,
                    fetchSize = body.fetchSize,
                    waitDurationMillis = body.waitDurationMillis,
                )
            backend.database.workerTask.upsertWorkerTasks(listOf(config)).map { config }
        }
    }
}

private fun Route.bindAdminTitleRoutes(backend: Backend) {
    AdminApi.Titles.get(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            TitleInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllTitles(fetch)
        }
    }
    AdminApi.Titles.Id.get(handleResult(backend)) { p ->
        backend.getTitleInfo(p.id)
    }
    AdminApi.Titles.Id.Status.update(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateTitleStatus(p.id, api.receiveBody(), uid)
        }
    }
}

private fun Route.bindAdminCommunityRoutes(backend: Backend) {
    AdminApi.Communities.get(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            CommunityInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllCommunities(fetch)
        }
    }
    AdminApi.Communities.Id.get(handleResult(backend)) { p ->
        backend.getCommunity(ObjectFetch.IdFetch(p.id), null, null)
    }
    AdminApi.Communities.Id.Status.update(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateCommunityStatus(p.id, api.receiveBody(), uid)
        }
    }
    AdminApi.Communities.Id.Members.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            MemberInfoListResponse(l, p)
        }) { f ->
            backend.getCommunityMemberInfos(p.id, f)
        }
    }
}

private fun Route.bindAdminFileRoutes(backend: Backend) {
    AdminApi.Files.get(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            FileInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllFileInfos(fetch)
        }
    }
    AdminApi.Files.search(handleResult(backend)) {
        it.pagination(GeneralOffsetPagingGenerator, { l, p ->
            FileInfoListResponse(l, p)
        }) { fetch ->
            backend.uncheckedSearchFiles(it, fetch)
        }
    }
    AdminApi.Files.Id.get(handleResult(backend)) { p ->
        backend.getFileInfoById(p.id)
    }
    AdminApi.Files.Id.Status.update(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateFileStatus(p.id, api.receiveBody(), uid)
        }
    }
    AdminApi.Files.Id.Refs.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            FileRefInfoListResponse(l, p)
        }) { f ->
            backend.uncheckedGetFileRefsByFileId(p.id, f)
        }
    }
}

private fun Route.bindAdminTopicRoutes(backend: Backend) {
    AdminApi.Topics.get(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            TopicInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllTopics(fetch)
        }
    }
    AdminApi.Topics.Id.get(handleResult(backend)) { p ->
        backend.uncheckGetTopicById(p.id, null)
    }
    AdminApi.Topics.Id.Status.update(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateTopicStatus(p.id, api.receiveBody(), uid)
        }
    }
    AdminApi.Topics.Id.Topics.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            TopicInfoListResponse(l, p)
        }) { f ->
            backend.uncheckGetTopicsByParentId(null, p.id, f, q.pinType)
        }
    }
}

private fun Route.bindAdminRoomRoutes(backend: Backend) {
    AdminApi.Rooms.getPublic(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            RoomInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllPublicRooms(fetch)
        }
    }
    AdminApi.Rooms.getPrivate(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            RoomInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllPrivateRooms(fetch)
        }
    }
    AdminApi.Rooms.Id.get(handleResult(backend)) { p ->
        backend.getRoomInfo(ObjectFetch.IdFetch(p.id), null, null)
    }
    AdminApi.Rooms.Id.Status.update(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateRoomStatus(p.id, api.receiveBody(), uid)
        }
    }
    AdminApi.Rooms.Id.Members.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            MemberInfoListResponse(l, p)
        }) { f ->
            backend.uncheckedGetRoomMemberInfos(p.id, f)
        }
    }
    AdminApi.Rooms.Id.Files.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            FileInfoListResponse(l, p)
        }) { f ->
            backend.getFileInfoPaginationResult(p.id, f)
        }
    }
}

private fun Route.bindAdminUserRoutes(backend: Backend) {
    bindAdminUserBasicRoutes(backend)
    bindAdminUserRelationRoutes(backend)
    bindAdminUserActivityRoutes(backend)
}

private fun Route.bindAdminUserBasicRoutes(backend: Backend) {
    AdminApi.Users.get(handleResult(backend)) {
        it.pagination(IdentifiablePagingGenerator, { l, p ->
            UserInfoListResponse(l, p)
        }) { fetch ->
            backend.getAllUsers(fetch)
        }
    }
    AdminApi.Users.Id.get(handleResult(backend)) { p ->
        backend.getUserById(p.id)
    }
    AdminApi.Users.Id.Overview.get(handleResult(backend)) { p ->
        backend.getUserOverview(p.id)
    }
    AdminApi.Users.add(handleResult(backend)) { api ->
        backend.addUser(api.receiveBody())
    }
    AdminApi.Users.Id.Status.update(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateUserStatus(p.id, api.receiveBody(), uid)
        }
    }
}

private fun Route.bindAdminUserRelationRoutes(backend: Backend) {
    AdminApi.Users.Id.Communities.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            CommunityMembershipListResponse(l, p)
        }) { f ->
            backend.getUserJoinedCommunities(null, p.id, f)
        }
    }
    AdminApi.Users.Id.Rooms.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            RoomInfoListResponse(l, p)
        }) { f ->
            backend.getUserJoinedRooms(p.id, f)
        }
    }
    AdminApi.Users.Id.Titles.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            TitleInfoListResponse(l, p)
        }) { f ->
            backend.getUserTitles(p.id, q.searchType, q.type, q.scopeId, q.titleStatus, f)
        }
    }
    AdminApi.Users.Id.Files.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            FileInfoListResponse(l, p)
        }) { f ->
            backend.getFileInfoPaginationResult(p.id, f)
        }
    }
}

private fun Route.bindAdminUserActivityRoutes(backend: Backend) {
    AdminApi.Users.Id.Logs.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            UserLogInfoListResponse(l, p)
        }) { f ->
            backend.getUserLogs(p.id, f)
        }
    }
    AdminApi.Users.Id.UploadRecords.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            UploadRecordInfoListResponse(l, p)
        }) { f ->
            backend.getUserUploadRecords(p.id, f)
        }
    }
    AdminApi.Users.Id.Reactions.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            ReactionRecordInfoListResponse(l, p)
        }) { f ->
            backend.getUserReactions(p.id, f)
        }
    }
    AdminApi.Users.Id.Comments.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            TopicActivityListResponse(l, p)
        }) { f ->
            backend.getUserCommentedTopics(p.id, f)
        }
    }
    AdminApi.Users.Id.Favorites.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            UserFavoriteInfoListResponse(l, p)
        }) { f ->
            backend.getFavorites(p.id, f)
        }
    }
    AdminApi.Users.Id.Subscriptions.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            UserSubscriptionInfoListResponse(l, p)
        }) { f ->
            backend.getUserSubscriptions(p.id, f)
        }
    }
}

private fun Route.bindAdminPanelLogRoutes(backend: Backend) {
    AdminApi.PanelLogs.get(handleResult(backend)) { q ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            PanelLogInfoListResponse(l, p)
        }) { f ->
            backend.getPanelLogs(q.targetId, q.objectType, f)
        }
    }
}

private fun Route.bindAdminTaskRecordRoutes(backend: Backend) {
    AdminApi.TaskRecords.get(handleResult(backend)) { q ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            TaskRecordInfoListResponse(l, p)
        }) { f ->
            backend.getTaskRecords(
                type = q.type,
                isSuccess = q.isSuccess,
                failureType = q.failureType,
                fetch = f,
            )
        }
    }
    AdminApi.TaskRecords.summary(handleResult(backend)) {
        backend.database.user.getTaskRecordSummaries().map { summaries ->
            TaskRecordSummaryListResponse(summaries.toImmutableList())
        }
    }
    AdminApi.TaskRecords.Id.Retry.update(handleResult(backend)) { path, _ ->
        usePrincipal {
            backend.database.user.updateTaskRecordRetryRequested(path.id, true).map { updated ->
                check(updated) { "Task record ${path.id} cannot be marked for retry" }
            }
        }
    }
}

fun Routing.bindUnauthenticatedPanelRoute(backend: Backend) {
    AdminApi.signOut(handleResult(backend)) {
        call.clearSigninSession()
        CustomResult.Success(Unit)
    }
    AdminApi.signIn.invoke(handleResult(backend)) { api ->
        call.consumeSigninChallenge()
        backend.adminSignIn(call.getData(), api.receiveBody()).onSuccess {
            saveSuccessSessionOnFirst(it.id)
        }
    }
    AdminApi.signUp.invoke(handleResult(backend)) {
        call.consumeSigninChallenge()
        backend.adminSignUp(call.getData(), it.receiveBody()).onSuccess { account ->
            saveSuccessSessionOnFirst(account.id)
        }
    }
    AdminApi.getData.invoke(handleResult(backend)) {
        call.beginSigninChallenge()
    }
}
