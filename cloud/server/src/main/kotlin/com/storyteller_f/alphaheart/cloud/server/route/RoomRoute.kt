/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.MemberInfoListResponse
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.TopicInfoListResponse
import com.storyteller_f.alphaheart.api.UserPubKeyInfoListResponse
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.cloud.core.service.addFavorite
import com.storyteller_f.alphaheart.cloud.core.service.addSubscription
import com.storyteller_f.alphaheart.cloud.core.service.createRoom
import com.storyteller_f.alphaheart.cloud.core.service.deleteFavoriteByObject
import com.storyteller_f.alphaheart.cloud.core.service.exitRoom
import com.storyteller_f.alphaheart.cloud.core.service.getRoomInfo
import com.storyteller_f.alphaheart.cloud.core.service.getRoomMemberInfos
import com.storyteller_f.alphaheart.cloud.core.service.getRoomPubKeys
import com.storyteller_f.alphaheart.cloud.core.service.getTopicsByParentId
import com.storyteller_f.alphaheart.cloud.core.service.joinRoom
import com.storyteller_f.alphaheart.cloud.core.service.removeSubscriptionByObject
import com.storyteller_f.alphaheart.cloud.core.service.searchRoomMembers
import com.storyteller_f.alphaheart.cloud.core.service.updateRoom
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipalOrNull
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.IdentifiablePagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.PrimaryKeyPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.alphaheart.cloud.server.common.pagination
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.UserPubKeyInfo
import com.storyteller_f.shared.type.ObjectType
import io.ktor.server.routing.Route

fun Route.bindRoomRoute(backend: Backend) {
    CustomApi.Rooms.Id.Members.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                MemberInfoListResponse(l, p)
            }) { f ->
                // 检查权限
                backend.getRoomMemberInfos(p.id, uid, f)
            }
        }
    }

    CustomApi.Rooms.Id.Members.search(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l, p ->
                MemberInfoListResponse(l, p)
            }) { f ->
                searchRoomMembers(backend, p, uid, q, f)
            }
        }
    }
    CustomApi.Rooms.Aid.get(handleResult(backend)) {
        usePrincipalOrNull { uid ->
            backend.getRoomInfo(ObjectFetch.AidFetch(it.aid), uid, it.fillJoinInfo)
        }
    }

    CustomApi.Rooms.Id.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            backend.getRoomInfo(ObjectFetch.IdFetch(p.id), uid, q.fillJoinInfo)
        }
    }

    CustomApi.Rooms.Id.Topics.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.getTopicsByParentId(p.id, ObjectType.ROOM, uid, q.fillHasCommented, f, q.pinType)
            }
        }
    }
}

fun Route.bindProtectedRoomRoute(backend: Backend) {
    CustomApi.Rooms.Id.Members.join(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.joinRoom(p.id, uid)
        }
    }
    CustomApi.Rooms.Id.Members.leave(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.exitRoom(p.id, uid)
        }
    }
    CustomApi.Rooms.Id.Members.publicKeys(handleResult(backend)) { q, p ->
        usePrincipal { uid ->
            q.pagination(object : PrimaryKeyPagingGenerator<UserPubKeyInfo>(UserPubKeyInfo::id) {}, { l, p ->
                UserPubKeyInfoListResponse(l, p)
            }) { f ->
                backend.getRoomPubKeys(p.id, uid, f)
            }
        }
    }
    CustomApi.Rooms.add(handleResult(backend)) { api ->
        val newRoom = api.receiveBody()
        usePrincipal { uid ->
            backend.createRoom(newRoom, uid)
        }
    }
    CustomApi.Rooms.Id.update(handleResult(backend)) { p, api ->
        val newRoom = api.receiveBody()
        usePrincipal { uid ->
            backend.updateRoom(p.id, newRoom, uid)
        }
    }

    CustomApi.Rooms.Id.Favorite.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addFavorite(uid, NewFavorite(ObjectType.ROOM, p.id)).map { }
        }
    }
    CustomApi.Rooms.Id.Favorite.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.deleteFavoriteByObject(uid, p.id).map { }
        }
    }
    CustomApi.Rooms.Id.Subscription.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addSubscription(uid, NewSubscription(p.id, ObjectType.ROOM)).map { }
        }
    }
    CustomApi.Rooms.Id.Subscription.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.removeSubscriptionByObject(uid, p.id).map { }
        }
    }
}
