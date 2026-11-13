/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CommunityMembershipListResponse
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.ReactionRecordInfoListResponse
import com.storyteller_f.alphaheart.api.RoomActivityListResponse
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.TitleInfoListResponse
import com.storyteller_f.alphaheart.api.TopicActivityListResponse
import com.storyteller_f.alphaheart.api.TopicInfoListResponse
import com.storyteller_f.alphaheart.api.TotpCodeBody
import com.storyteller_f.alphaheart.api.UserFavoriteInfoListResponse
import com.storyteller_f.alphaheart.api.UserInfoListResponse
import com.storyteller_f.alphaheart.api.UserSubscriptionInfoListResponse
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.cloud.core.service.addDevice
import com.storyteller_f.alphaheart.cloud.core.service.addFavorite
import com.storyteller_f.alphaheart.cloud.core.service.addReadLog
import com.storyteller_f.alphaheart.cloud.core.service.addSubscription
import com.storyteller_f.alphaheart.cloud.core.service.deleteFavoriteByObject
import com.storyteller_f.alphaheart.cloud.core.service.disableTwoFactor
import com.storyteller_f.alphaheart.cloud.core.service.enableTotp
import com.storyteller_f.alphaheart.cloud.core.service.fillRoomActivities
import com.storyteller_f.alphaheart.cloud.core.service.generateRecoveryCodes
import com.storyteller_f.alphaheart.cloud.core.service.getAllUsers
import com.storyteller_f.alphaheart.cloud.core.service.getFavorites
import com.storyteller_f.alphaheart.cloud.core.service.getJoinedPinnedTopics
import com.storyteller_f.alphaheart.cloud.core.service.getTopicsByParentId
import com.storyteller_f.alphaheart.cloud.core.service.getTwoFactorSettings
import com.storyteller_f.alphaheart.cloud.core.service.getUnreadMessageCount
import com.storyteller_f.alphaheart.cloud.core.service.getUnreadRoomActivities
import com.storyteller_f.alphaheart.cloud.core.service.getUserCommentedTopics
import com.storyteller_f.alphaheart.cloud.core.service.getUserInfo
import com.storyteller_f.alphaheart.cloud.core.service.getUserJoinedCommunities
import com.storyteller_f.alphaheart.cloud.core.service.getUserJoinedRooms
import com.storyteller_f.alphaheart.cloud.core.service.getUserOverview
import com.storyteller_f.alphaheart.cloud.core.service.getUserReactions
import com.storyteller_f.alphaheart.cloud.core.service.getUserSubscriptions
import com.storyteller_f.alphaheart.cloud.core.service.getUserTitles
import com.storyteller_f.alphaheart.cloud.core.service.hasUnreadRooms
import com.storyteller_f.alphaheart.cloud.core.service.removeSubscriptionByObject
import com.storyteller_f.alphaheart.cloud.core.service.searchCurrentUserRooms
import com.storyteller_f.alphaheart.cloud.core.service.searchUserJoinedCommunities
import com.storyteller_f.alphaheart.cloud.core.service.searchUsers
import com.storyteller_f.alphaheart.cloud.core.service.setupTotp
import com.storyteller_f.alphaheart.cloud.core.service.updateUser
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipalOrNull
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.IdentifiablePagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.alphaheart.cloud.server.common.pagination
import com.storyteller_f.alphaheart.cloud.server.common.pagingGenerator
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.andThen
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.type.ObjectType
import io.ktor.server.routing.Route
import kotlinx.collections.immutable.ImmutableList

fun Route.bindProtectedUserRoute(backend: Backend) {
    bindProtectedUserAccountRoute(backend)
    bindUserFavoriteRoute(backend)
    bindProtectedUserActivityRoute(backend)
    bindUserCommunitiesRoute(backend)
}

private fun Route.bindProtectedUserAccountRoute(backend: Backend) {
    CustomApi.Users.update(handleResult(backend)) { api ->
        usePrincipal { uid ->
            backend.updateUser(uid, api.receiveBody())
        }
    }
    CustomApi.Users.TwoFactor.get(handleResult(backend)) {
        usePrincipal { uid ->
            backend.getTwoFactorSettings(uid)
        }
    }
    CustomApi.Users.TwoFactor.Totp.setup(handleResult(backend)) {
        usePrincipal { uid ->
            backend.setupTotp(uid)
        }
    }
    CustomApi.Users.TwoFactor.Totp.enable(handleResult(backend)) { api ->
        usePrincipal { uid ->
            val body: TotpCodeBody = api.receiveBody()
            backend.enableTotp(uid, body.code)
        }
    }
    CustomApi.Users.TwoFactor.disable(handleResult(backend)) {
        usePrincipal { uid ->
            backend.disableTwoFactor(uid)
        }
    }
    CustomApi.Users.TwoFactor.recoveryCodes(handleResult(backend)) {
        usePrincipal { uid ->
            backend.generateRecoveryCodes(uid)
        }
    }
    CustomApi.Users.Read.add(handleResult(backend)) { api ->
        usePrincipal { uid ->
            backend.addReadLog(uid, api.receiveBody())
        }
    }
    CustomApi.Users.Devices.add(handleResult(backend)) { api ->
        usePrincipal { uid ->
            val newDevice = api.receiveBody()
            backend.addDevice(uid, newDevice)
        }
    }
}

private fun Route.bindProtectedUserActivityRoute(backend: Backend) {
    CustomApi.Users.Id.Favorite.delete(handleResult(backend)) { path, _ ->
        usePrincipal { uid ->
            backend.deleteFavoriteByObject(uid, path.id).map { }
        }
    }

    CustomApi.Users.overview(handleResult(backend)) {
        usePrincipal { uid ->
            backend.getUserOverview(uid)
        }
    }
    CustomApi.Users.ReactionRecords.get(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                ReactionRecordInfoListResponse(l, p)
            }) { fetch ->
                backend.getUserReactions(uid, fetch)
            }
        }
    }
    CustomApi.Users.Comments.get(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                TopicActivityListResponse(l, p)
            }) { fetch ->
                backend.getUserCommentedTopics(uid, fetch)
            }
        }
    }
    bindRoomActivityRoute(backend)
}

private fun Route.bindRoomActivityRoute(backend: Backend) {
    CustomApi.Users.Unread.activities(handleResult(backend)) { query ->
        usePrincipal { uid ->
            query.pagination(
                pagingGenerator { activity: com.storyteller_f.alphaheart.api.RoomActivityInfo ->
                    requireNotNull(activity.latestMessageId)
                },
                { list, pagination -> RoomActivityListResponse(list, pagination) },
            ) { fetch ->
                backend.getUnreadRoomActivities(uid, fetch)
            }
        }
    }
    CustomApi.Users.PinnedTopics.get(handleResult(backend)) { query ->
        usePrincipal { uid ->
            query.pagination(
                IdentifiablePagingGenerator,
                { list, pagination -> TopicActivityListResponse(list, pagination) },
            ) { fetch ->
                backend.getJoinedPinnedTopics(uid, fetch)
            }
        }
    }
    CustomApi.Users.Unread.messages(handleResult(backend)) {
        usePrincipal { uid -> backend.getUnreadMessageCount(uid) }
    }
    CustomApi.Users.Unread.hasUnreadRooms(handleResult(backend)) {
        usePrincipal { uid ->
            backend.hasUnreadRooms(uid)
        }
    }
}

private fun Route.bindUserFavoriteRoute(backend: Backend) {
    CustomApi.Users.Favorites.get(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                UserFavoriteInfoListResponse(l, p)
            }) { fetch ->
                backend.getFavorites(uid, fetch)
            }
        }
    }

    CustomApi.Users.Id.Favorite.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addFavorite(uid, NewFavorite(ObjectType.USER, p.id)).map { }
        }
    }
}

private fun Route.bindUserCommunitiesRoute(backend: Backend) {
    CustomApi.Users.JoinedCommunities.get(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                CommunityMembershipListResponse(l, p)
            }) { fetch ->
                backend.getUserJoinedCommunities(uid, uid, fetch, q.hasPoster)
            }
        }
    }
    CustomApi.Users.JoinedCommunities.search(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l, p ->
                CommunityMembershipListResponse(l, p)
            }) { fetch ->
                backend.searchUserJoinedCommunities(uid, q, fetch)
            }
        }
    }
    CustomApi.Users.JoinedRooms.get(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(IdentifiablePagingGenerator, { l: ImmutableList<RoomInfo>, p ->
                RoomInfoListResponse(l, p)
            }) { fetch ->
                backend.getUserJoinedRooms(uid, fetch)
            }.andThen(ServerError::fromError) { backend.fillRoomActivities(uid, it) }
        }
    }
    CustomApi.Users.JoinedRooms.search(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l: ImmutableList<RoomInfo>, p ->
                RoomInfoListResponse(l, p)
            }) { fetch ->
                backend.searchCurrentUserRooms(uid, fetch, q)
            }.andThen(ServerError::fromError) { backend.fillRoomActivities(uid, it) }
        }
    }
}

fun Route.bindProtectedUserSubscriptionRoute(backend: Backend) {
    CustomApi.Users.Subscriptions.get(handleResult(backend)) { q ->
        usePrincipal { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                UserSubscriptionInfoListResponse(l, p)
            }) { fetch ->
                backend.getUserSubscriptions(uid, fetch)
            }
        }
    }
    CustomApi.Users.Id.Subscription.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addSubscription(uid, NewSubscription(p.id, ObjectType.USER)).map { }
        }
    }

    CustomApi.Users.Id.Subscription.delete(handleResult(backend)) { path, _ ->
        usePrincipal { uid ->
            backend.removeSubscriptionByObject(uid, path.id).map { }
        }
    }
}

fun Route.bindUserRoute(backend: Backend) {
    CustomApi.Users.Aid.get(handleResult(backend)) { q ->
        usePrincipalOrNull { uid ->
            backend.getUserInfo(ObjectFetch.AidFetch(q.aid), uid)
        }
    }
    CustomApi.Users.Id.get(handleResult(backend)) {
        usePrincipalOrNull { uid ->
            backend.getUserInfo(ObjectFetch.IdFetch(it.id), uid)
        }
    }

    CustomApi.Users.Id.Topics.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.getTopicsByParentId(p.id, ObjectType.USER, uid, q.fillHasCommented, f, q.pinType)
            }
        }
    }

    CustomApi.Users.Id.Communities.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                CommunityMembershipListResponse(l, p)
            }) { f ->
                backend.getUserJoinedCommunities(uid, p.id, f, q.hasPoster)
            }
        }
    }

    CustomApi.Users.Id.Titles.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            TitleInfoListResponse(l, p)
        }) { f ->
            backend.getUserTitles(p.id, q.searchType, q.type, q.scopeId, q.titleStatus, f)
        }
    }

    CustomApi.Users.search(handleResult(backend)) { q ->
        usePrincipalOrNull { uid ->
            if (q.word.isBlank()) {
                q.pagination(IdentifiablePagingGenerator, { l, p ->
                    UserInfoListResponse(l, p)
                }) { f ->
                    backend.getAllUsers(f)
                }
            } else {
                q.pagination(GeneralOffsetPagingGenerator, { l, p ->
                    UserInfoListResponse(l, p)
                }) { f ->
                    backend.searchUsers(q.word, uid, f)
                }
            }
        }
    }
}
