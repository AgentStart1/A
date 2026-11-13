/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.FileDatabase
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.FileRecord
import com.storyteller_f.alphaheart.backend.core.types.FileRef
import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.firstNotNull
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.FileRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.FileRefs
import com.storyteller_f.alphaheart.backend.exposed.tables.Quotas
import com.storyteller_f.alphaheart.backend.exposed.tables.UploadRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.QuotaInfo
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UploadRecordStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update

class ExposedFileDatabase(val databaseSession: ExposedDatabaseSession) : FileDatabase {
    override suspend fun getUploadRecord(id: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UploadRecords.selectAll().where {
                    UploadRecords.id eq id
                }
            }
            first(UploadRecord::wrapRow)
        }

    override suspend fun getFileRecord(owner: PrimaryKey, name: String) =
        databaseSession.dbSearch {
            search {
                FileRecords.selectAll().where {
                    FileRecords.owner eq owner and (FileRecords.name eq name)
                }
            }
            first(FileRecord::wrapRow)
        }

    override suspend fun getFileRecordByIds(ids: List<PrimaryKey>): CustomResult<List<FileRecord>, DatabaseError> {
        if (ids.isEmpty()) return CustomResult.success(emptyList())
        return databaseSession.dbSearch {
            search {
                FileRecords.selectAll().where {
                    FileRecords.id inList ids
                }
            }
            map(FileRecord::wrapRow)
        }
    }

    override suspend fun getFileRecordListByOwner(owner: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                FileRecords.selectAll().where {
                    FileRecords.owner eq owner
                }.orderBy(FileRecords.id, SortOrder.DESC)
            }
            map(FileRecord::wrapRow)
        }

    override suspend fun getFileRecordByNames(names: List<String>): CustomResult<List<FileRecord>, DatabaseError> {
        if (names.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        return databaseSession.dbSearch {
            search {
                FileRecords.selectAll().where {
                    FileRecords.fullName inList names
                }.orderBy(FileRecords.id, SortOrder.DESC)
            }
            map(FileRecord::wrapRow)
        }
    }

    override suspend fun insertFileRefs(fileRefs: List<FileRef>) =
        databaseSession.dbQuery {
            check(
                FileRefs.batchInsert(fileRefs) {
                    this[FileRefs.id] = it.id
                    this[FileRefs.createdTime] = it.createdTime
                    this[FileRefs.objectId] = it.objectId
                    this[FileRefs.objectType] = it.objectType
                    this[FileRefs.mediaName] = it.mediaName
                    this[FileRefs.author] = it.author
                    this[FileRefs.fileId] = it.fileId
                }.size == fileRefs.size,
            ) {
                "insert file refs failed"
            }
        }

    override suspend fun getFileRecordPaginationList(uid: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            getFileRecordListByPredicate {
                where { FileRecords.owner eq uid }.bindPaginationQuery(FileRecords, primaryKeyFetch)
            },
            getFileRecordCountByPredicate { where { FileRecords.owner eq uid } },
        )

    override suspend fun getAllFileRecordPaginationList(primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            getFileRecordListByPredicate { bindPaginationQuery(FileRecords, primaryKeyFetch) },
            getFileRecordCountByPredicate { this },
        )

    override suspend fun insertFileRecord(
        fileRecordList: List<FileRecord>,
        ownerId: PrimaryKey,
        ownerType: ObjectType,
    ) = databaseSession.dbQuery {
        insertFileRecords(fileRecordList)
    }

    private suspend fun insertFileRecords(fileRecordList: List<FileRecord>) {
        check(
            FileRecords.batchInsert(fileRecordList) { e ->
                this[FileRecords.id] = e.id
                this[FileRecords.createdTime] = e.createdTime
                this[FileRecords.name] = e.name
                this[FileRecords.duration] = 0
                this[FileRecords.width] = e.width
                this[FileRecords.height] = e.height
                this[FileRecords.owner] = e.owner
                this[FileRecords.ownerType] = e.ownerType
                this[FileRecords.contentType] = e.contentType
                this[FileRecords.size] = e.size
                this[FileRecords.fullName] = e.fullName
                this[FileRecords.sha256] = e.sha256
                this[FileRecords.status] = e.status
            }.size == fileRecordList.size,
        ) {
            "insert file record failed"
        }
    }

    override suspend fun lockFileQuota(quotaInfo: QuotaInfo, operationId: PrimaryKey) =
        databaseSession.dbQuery {
            check(
                Quotas.update({
                    Quotas.ownerId eq quotaInfo.ownerId and
                        (Quotas.quotaType eq QuotaType.FILE) and
                        (Quotas.lockId eq null) and (Quotas.used eq quotaInfo.used)
                }) { it[lockId] = operationId } > 0,
            ) { "lock quota failed" }
        }

    override suspend fun finishFileQuota(
        quotaInfo: QuotaInfo,
        operationId: PrimaryKey,
        fileRecords: List<FileRecord>,
    ) = databaseSession.dbQuery {
        check(
            Quotas.update({
                Quotas.ownerId eq quotaInfo.ownerId and
                    (Quotas.quotaType eq QuotaType.FILE) and
                    (Quotas.lockId eq operationId) and (Quotas.used eq quotaInfo.used)
            }) {
                it[lockId] = null
                it[used] = quotaInfo.used + fileRecords.sumOf { file -> file.size }
            } > 0,
        ) { "unlock quota failed" }
        insertFileRecords(fileRecords)
        fileRecords
    }

    override suspend fun insertUploadRecord(record: UploadRecord): CustomResult<UploadRecord, DatabaseError> =
        databaseSession.dbQuery {
            check(
                UploadRecords.insert {
                    it[UploadRecords.id] = record.id
                    it[UploadRecords.createdTime] = record.createdTime
                    it[UploadRecords.objectId] = record.objectId
                    it[UploadRecords.objectType] = record.objectType
                    it[UploadRecords.status] = record.status
                    it[UploadRecords.total] = record.total
                    it[UploadRecords.progress] = record.progress
                    it[UploadRecords.name] = record.name
                    it[UploadRecords.chunkSize] = record.chunkSize
                    it[UploadRecords.sha256] = record.sha256
                    it[UploadRecords.crc32c] = record.crc32c
                }.insertedCount > 0,
            ) {
                "insert upload record failed"
            }
            check(
                Quotas.update({
                    Quotas.ownerId eq record.objectId and
                        (Quotas.quotaType eq QuotaType.FILE) and
                        (Quotas.lockId eq null)
                }) {
                    it[this.lockId] = record.id
                } > 0,
            ) {
                "lock quota failed"
            }
            record
        }

    override suspend fun updateUploadRecordStatus(
        quotaInfo: QuotaInfo,
        record: UploadRecord,
        fileRecordList: List<FileRecord>,
    ): CustomResult<List<FileRecord>, DatabaseError> {
        if (record.status == UploadRecordStatus.PENDING) {
            return CustomResult.Failure(DatabaseError.from(CustomBadRequestException("can't update pending record")))
        }
        return databaseSession.dbQuery {
            check(
                UploadRecords.update({
                    UploadRecords.id eq record.id
                }) {
                    it[UploadRecords.status] = record.status
                } > 0,
            ) {
                "delete upload record failed"
            }
            check(
                Quotas.update({
                    Quotas.ownerId eq quotaInfo.ownerId and
                        (Quotas.quotaType eq QuotaType.FILE) and
                        (Quotas.lockId eq record.id) and
                        (Quotas.used eq quotaInfo.used)
                }) {
                    it[Quotas.lockId] = null
                    if (record.status != UploadRecordStatus.SUCCESS) {
                        it[Quotas.used] = quotaInfo.used
                    } else {
                        it[Quotas.used] = quotaInfo.used + record.total
                    }
                } > 0,
            ) {
                "unlock quota failed"
            }
            insertFileRecords(fileRecordList)
            fileRecordList
        }
    }

    override suspend fun getFileCount() =
        databaseSession.dbSearch {
            search {
                FileRecords.selectAll()
            }
            count()
        }

    override suspend fun getFileVolume() =
        databaseSession.dbSearch {
            val sumColumn = FileRecords.size.sum()
            search {
                FileRecords.select(sumColumn)
            }
            firstNotNull {
                it[sumColumn] ?: 0
            }
        }

    override suspend fun updateFileStatus(id: PrimaryKey, status: ObjectStatus) =
        databaseSession.dbQuery {
            FileRecords.update({ FileRecords.id eq id }) {
                it[FileRecords.status] = status
            } > 0
        }

    override suspend fun getUploadRecordPaginationList(uid: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            getUploadRecordListByPredicate {
                where {
                    UploadRecords.objectId eq uid and (UploadRecords.objectType eq ObjectType.USER)
                }.bindPaginationQuery(UploadRecords, primaryKeyFetch)
            },
            getUploadRecordCountByPredicate {
                where {
                    UploadRecords.objectId eq uid and (UploadRecords.objectType eq ObjectType.USER)
                }
            },
        )

    private suspend fun getFileRecordListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                FileRecords.selectAll().queryBuilder()
            }
            map(FileRecord::wrapRow)
        }

    private suspend fun getFileRecordCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                FileRecords.selectAll().queryBuilder()
            }
            count()
        }

    private suspend fun getUploadRecordListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                UploadRecords.selectAll().queryBuilder()
            }
            map(UploadRecord::wrapRow)
        }

    private suspend fun getUploadRecordCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                UploadRecords.selectAll().queryBuilder()
            }
            count()
        }

    override suspend fun getFileRefsByFileId(fileId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            getFileRefListByPredicate {
                where {
                    FileRefs.fileId eq fileId
                }.bindPaginationQuery(FileRefs, primaryKeyFetch)
            },
            getFileRefCountByPredicate {
                where {
                    FileRefs.fileId eq fileId
                }
            },
        )

    private suspend fun getFileRefListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                FileRefs.selectAll().queryBuilder()
            }
            map(FileRef::wrapRow)
        }

    private suspend fun getFileRefCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                FileRefs.selectAll().queryBuilder()
            }
            count()
        }
}
