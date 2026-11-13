/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.result.CustomResult
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.bindUnauthenticatedRoute(backend: Backend) {
    get("/ping") {
        call.respondText("pong")
    }

    CustomApi.Root.get(handleResult(backend)) {
        CustomResult.success("${backend.customConfig.flavor} ${backend.customConfig.buildType}")
    }
}
