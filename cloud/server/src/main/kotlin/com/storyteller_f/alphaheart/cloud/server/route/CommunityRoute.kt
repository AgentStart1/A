/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CommunityMembershipListResponse
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.MemberInfoListResponse
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.TopicInfoListResponse
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.cloud.core.service.addFavorite
import com.storyteller_f.alphaheart.cloud.core.service.addSubscription
import com.storyteller_f.alphaheart.cloud.core.service.createCommunity
import com.storyteller_f.alphaheart.cloud.core.service.deleteFavoriteByObject
import com.storyteller_f.alphaheart.cloud.core.service.exitCommunity
import com.storyteller_f.alphaheart.cloud.core.service.fillRoomActivities
import com.storyteller_f.alphaheart.cloud.core.service.getCommunity
import com.storyteller_f.alphaheart.cloud.core.service.getCommunityMemberInfos
import com.storyteller_f.alphaheart.cloud.core.service.getCommunityRooms
import com.storyteller_f.alphaheart.cloud.core.service.getTopicsByParentId
import com.storyteller_f.alphaheart.cloud.core.service.joinCommunity
import com.storyteller_f.alphaheart.cloud.core.service.removeSubscriptionByObject
import com.storyteller_f.alphaheart.cloud.core.service.searchCommunities
import com.storyteller_f.alphaheart.cloud.core.service.searchCommunityRooms
import com.storyteller_f.alphaheart.cloud.core.service.searchContainerMembers
import com.storyteller_f.alphaheart.cloud.core.service.updateCommunity
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipalOrNull
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.IdentifiablePagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.alphaheart.cloud.server.common.pagination
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.andThen
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.type.ObjectType
import io.ktor.server.routing.Route
import kotlinx.collections.immutable.ImmutableList

fun Route.bindCommunityRoute(backend: Backend) {
    CustomApi.Communities.search(handleResult(backend)) {
        usePrincipalOrNull { uid ->
            it.pagination(GeneralOffsetPagingGenerator, { l, p ->
                CommunityMembershipListResponse(l, p)
            }) { f ->
                backend.searchCommunities(uid, it, f)
            }
        }
    }

    bindCommunityMemberRoute(backend)

    CustomApi.Communities.Id.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            backend.getCommunity(ObjectFetch.IdFetch(p.id), uid, q.fillJoinInfo)
        }
    }

    CustomApi.Communities.Aid.get(handleResult(backend)) {
        usePrincipalOrNull { uid ->
            backend.getCommunity(ObjectFetch.AidFetch(it.aid), uid, it.fillJoinInfo)
        }
    }

    CustomApi.Communities.Id.Topics.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.getTopicsByParentId(p.id, ObjectType.COMMUNITY, uid, q.fillHasCommented, f, q.pinType)
            }
        }
    }

    CustomApi.Communities.Id.Rooms.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l: ImmutableList<RoomInfo>, p ->
                RoomInfoListResponse(l, p)
            }) { f ->
                backend.getCommunityRooms(p.id, f, uid, q.joinStatus)
            }.andThen(ServerError::fromError) { backend.fillRoomActivities(uid, it) }
        }
    }

    CustomApi.Communities.Id.Rooms.search(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l: ImmutableList<RoomInfo>, p ->
                RoomInfoListResponse(l, p)
            }) { f ->
                backend.searchCommunityRooms(uid, p.id, f, q)
            }.andThen(ServerError::fromError) { backend.fillRoomActivities(uid, it) }
        }
    }
}

private fun Route.bindCommunityMemberRoute(backend: Backend) {
    CustomApi.Communities.Id.Members.get(handleResult(backend)) { q, p ->
        q.pagination(IdentifiablePagingGenerator, { l, p ->
            MemberInfoListResponse(l, p)
        }) { f ->
            backend.getCommunityMemberInfos(p.id, f)
        }
    }

    CustomApi.Communities.Id.Members.search(handleResult(backend)) { q, p ->
        q.pagination(GeneralOffsetPagingGenerator, { l, p ->
            MemberInfoListResponse(l, p)
        }) { f ->
            backend.searchContainerMembers(p.id, q.word, f)
        }
    }
}

fun Route.bindProtectedCommunityRoute(backend: Backend) {
    CustomApi.Communities.Id.Members.join(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.joinCommunity(uid, p.id)
        }
    }

    CustomApi.Communities.Id.Members.leave(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.exitCommunity(p.id, uid)
        }
    }
    CustomApi.Communities.add(handleResult(backend)) { api ->
        val newCommunity = api.receiveBody()
        usePrincipal { uid ->
            backend.createCommunity(newCommunity, uid)
        }
    }

    CustomApi.Communities.Id.update(handleResult(backend)) { p, api ->
        val newCommunity = api.receiveBody()
        usePrincipal { uid ->
            backend.updateCommunity(p.id, newCommunity, uid)
        }
    }

    CustomApi.Communities.Id.Favorite.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addFavorite(uid, NewFavorite(ObjectType.COMMUNITY, p.id)).map { }
        }
    }
    CustomApi.Communities.Id.Favorite.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.deleteFavoriteByObject(uid, p.id).map { }
        }
    }
    CustomApi.Communities.Id.Subscription.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addSubscription(uid, NewSubscription(p.id, ObjectType.COMMUNITY)).map { }
        }
    }
    CustomApi.Communities.Id.Subscription.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.removeSubscriptionByObject(uid, p.id).map { }
        }
    }
}
