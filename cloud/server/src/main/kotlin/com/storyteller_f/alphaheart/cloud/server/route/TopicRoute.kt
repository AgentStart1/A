/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.ReactionInfoListResponse
import com.storyteller_f.alphaheart.api.TopicInfoListResponse
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.cloud.core.service.addFavorite
import com.storyteller_f.alphaheart.cloud.core.service.addReaction
import com.storyteller_f.alphaheart.cloud.core.service.addSubscription
import com.storyteller_f.alphaheart.cloud.core.service.createPlainTopic
import com.storyteller_f.alphaheart.cloud.core.service.createTopicSnapshot
import com.storyteller_f.alphaheart.cloud.core.service.deleteFavoriteByObject
import com.storyteller_f.alphaheart.cloud.core.service.deleteReaction
import com.storyteller_f.alphaheart.cloud.core.service.getTopic
import com.storyteller_f.alphaheart.cloud.core.service.getTopicByAid
import com.storyteller_f.alphaheart.cloud.core.service.getTopicsByParentId
import com.storyteller_f.alphaheart.cloud.core.service.reactionList
import com.storyteller_f.alphaheart.cloud.core.service.recommendTopics
import com.storyteller_f.alphaheart.cloud.core.service.removeSubscriptionByObject
import com.storyteller_f.alphaheart.cloud.core.service.searchCommunityTopics
import com.storyteller_f.alphaheart.cloud.core.service.searchRoomTopics
import com.storyteller_f.alphaheart.cloud.core.service.searchUserTopics
import com.storyteller_f.alphaheart.cloud.core.service.updateTopicPin
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipalOrNull
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.IdentifiablePagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.ReactionPaginationGenerator
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.alphaheart.cloud.server.common.pagination
import com.storyteller_f.alphaheart.cloud.ws.api.GlobalWsEventPublisher
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.map
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.type.ObjectType
import io.ktor.server.routing.Route
import kotlinx.coroutines.launch

fun Route.bindTopicRoute(backend: Backend) {
    CustomApi.Topics.recommend(handleResult(backend)) {
        usePrincipalOrNull { uid ->
            it.pagination(GeneralOffsetPagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.recommendTopics(uid, f, it)
            }
        }
    }

    CustomApi.Topics.Id.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            backend.getTopic(p.id, uid, q.fillHasCommented)
        }
    }

    CustomApi.Topics.Aid.get(handleResult(backend)) {
        usePrincipalOrNull { uid ->
            backend.getTopicByAid(it.aid, uid, it.fillHasCommented)
        }
    }

    bindSearchTopicRoute(backend)
}

private fun Route.bindSearchTopicRoute(backend: Backend) {
    CustomApi.Topics.Id.Topics.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.getTopicsByParentId(
                    p.id,
                    ObjectType.TOPIC,
                    uid,
                    q.fillHasCommented,
                    f,
                    q.pinType,
                )
            }
        }
    }

    // 用户主题搜索路由
    CustomApi.Topics.Users.Id.search(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.searchUserTopics(p.id, q, f, uid)
            }
        }
    }

    // 房间主题搜索路由
    CustomApi.Topics.Rooms.Id.search(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.searchRoomTopics(p.id, q, f, uid)
            }
        }
    }

    // 社区主题搜索路由
    CustomApi.Topics.Communities.Id.search(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(GeneralOffsetPagingGenerator, { l, p ->
                TopicInfoListResponse(l, p)
            }) { f ->
                backend.searchCommunityTopics(p.id, q, f, uid)
            }
        }
    }

    CustomApi.Topics.Id.Reactions.get(handleResult(backend)) { q, p ->
        usePrincipalOrNull { uid ->
            q.pagination(ReactionPaginationGenerator(backend), { l, p ->
                ReactionInfoListResponse(l, p)
            }) { fetch ->
                backend.reactionList(p.id, uid, q.fillHasReacted, fetch)
            }
        }
    }
}

fun Route.bindProtectedTopicRoute(backend: Backend) {
    CustomApi.Topics.Id.createSnapshot(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.createTopicSnapshot(uid, p.id)
        }
    }

    CustomApi.Topics.add(handleResult(backend)) { api ->
        usePrincipal { uid ->
            backend.createPlainTopic(uid, api.receiveBody()).also { result ->
                val topic = result.getOrNull()
                if (topic != null) {
                    call.application.launch {
                        GlobalWsEventPublisher.publishNewTopic(RoomFrame.NewTopicInfo(topic))
                    }
                }
            }
        }
    }
    bindProtectedTopicReactionRoute(backend)
    bindProtectedTopicPinRoute(backend)
    bindProtectedTopicFavoriteRoute(backend)
    bindProtectedTopicSubscriptionRoute(backend)
}

private fun Route.bindProtectedTopicReactionRoute(backend: Backend) {
    CustomApi.Topics.Id.Reactions.add(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            val emoji = api.receiveBody().emoji
            addReaction(emoji, backend, uid, p)
        }
    }
    CustomApi.Topics.Id.Reactions.delete(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            val deleteReactionBody = api.receiveBody()
            deleteReaction(deleteReactionBody, backend, uid, p)
        }
    }
}

private fun Route.bindProtectedTopicPinRoute(backend: Backend) {
    CustomApi.Topics.Id.pin(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateTopicPin(uid, p.id, true)
        }
    }

    CustomApi.Topics.Id.unpin(handleResult(backend)) { p, api ->
        usePrincipal { uid ->
            backend.updateTopicPin(uid, p.id, false)
        }
    }
}

private fun Route.bindProtectedTopicFavoriteRoute(backend: Backend) {
    CustomApi.Topics.Id.Favorite.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addFavorite(uid, NewFavorite(ObjectType.TOPIC, p.id)).map { }
        }
    }
    CustomApi.Topics.Id.Favorite.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.deleteFavoriteByObject(uid, p.id).map { }
        }
    }
}

private fun Route.bindProtectedTopicSubscriptionRoute(backend: Backend) {
    CustomApi.Topics.Id.Subscription.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addSubscription(uid, NewSubscription(p.id, ObjectType.TOPIC)).map { }
        }
    }
    CustomApi.Topics.Id.Subscription.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.removeSubscriptionByObject(uid, p.id).map { }
        }
    }
}
