/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.result.onFailure
import io.github.aakira.napier.Napier

internal suspend fun Backend.saveUserSearchDocument(document: UserDocument) {
    userSearchService.saveDocument(listOf(document)).onFailure { error ->
        Napier.e(error.cause) { "save user document failed" }
    }
}
