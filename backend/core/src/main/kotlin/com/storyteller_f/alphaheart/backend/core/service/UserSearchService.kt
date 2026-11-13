/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.type.PrimaryKey

data class UserDocument(override val id: PrimaryKey, val nickname: String, val aid: String?) :
    PrimaryKeyIdentifiable {
    companion object {
        fun fromUser(user: User): UserDocument = UserDocument(user.id, user.nickname, user.aid)
    }
}

sealed interface UserDocumentSearch {
    data class Keyword(val word: String, val fetch: OffsetFetch) : UserDocumentSearch
}

interface UserSearchService {
    suspend fun saveDocument(documents: List<UserDocument>): CustomResult<Unit, SearchError>

    suspend fun clean(): CustomResult<Unit, SearchError>

    suspend fun searchDocument(
        userDocumentSearch: UserDocumentSearch,
    ): CustomResult<PaginationResult<UserDocument>, SearchError>
}

interface UserSearchServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): UserSearchService
}
