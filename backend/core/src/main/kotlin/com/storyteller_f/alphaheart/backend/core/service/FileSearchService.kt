/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.types.FileRecord
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.type.PrimaryKey

data class FileDocument(override val id: PrimaryKey, val name: String, val ownerId: PrimaryKey) :
    PrimaryKeyIdentifiable {
    companion object {
        fun fromFileRecord(fileRecord: FileRecord): FileDocument =
            FileDocument(
                fileRecord.id,
                fileRecord.name,
                fileRecord.owner,
            )
    }
}

sealed interface FileDocumentSearch {
    data class Keyword(val word: String, val ownerId: PrimaryKey? = null, val fetch: OffsetFetch) : FileDocumentSearch
}

interface FileSearchService {
    suspend fun saveDocument(documents: List<FileDocument>): CustomResult<Unit, SearchError>

    suspend fun clean(): CustomResult<Unit, SearchError>

    suspend fun searchDocument(
        fileDocumentSearch: FileDocumentSearch,
    ): CustomResult<PaginationResult<FileDocument>, SearchError>
}

interface FileSearchServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): FileSearchService
}
