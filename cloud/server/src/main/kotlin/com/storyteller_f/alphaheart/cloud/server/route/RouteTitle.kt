/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.cloud.core.service.addFavorite
import com.storyteller_f.alphaheart.cloud.core.service.addSubscription
import com.storyteller_f.alphaheart.cloud.core.service.createTitle
import com.storyteller_f.alphaheart.cloud.core.service.deleteFavoriteByObject
import com.storyteller_f.alphaheart.cloud.core.service.removeSubscriptionByObject
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.map
import com.storyteller_f.shared.type.ObjectType
import io.ktor.server.routing.Route

fun Route.bindProtectedTitleRoute(backend: Backend) {
    CustomApi.Titles.add(handleResult(backend)) {
        usePrincipal { uid ->
            val title = it.receiveBody()
            createTitle(title, backend, uid)
        }
    }

    CustomApi.Titles.Id.Favorite.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addFavorite(uid, NewFavorite(ObjectType.TITLE, p.id)).map { }
        }
    }

    CustomApi.Titles.Id.Favorite.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.deleteFavoriteByObject(uid, p.id).map { }
        }
    }

    CustomApi.Titles.Id.Subscription.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addSubscription(uid, NewSubscription(p.id, ObjectType.TITLE)).map { }
        }
    }

    CustomApi.Titles.Id.Subscription.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.removeSubscriptionByObject(uid, p.id).map { }
        }
    }
}
