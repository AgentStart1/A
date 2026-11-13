/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.WorkerTaskDatabase
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.tables.WorkerTasks
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert

/** Exposed-backed implementation of [WorkerTaskDatabase]. */
internal class ExposedWorkerTaskDatabase(
    /** Database session used for executing queries. */
    val databaseSession: ExposedDatabaseSession,
) : WorkerTaskDatabase {
    override suspend fun getWorkerTask(type: TaskRecordType): CustomResult<WorkerTask?, DatabaseError> =
        databaseSession.dbSearch {
            search {
                WorkerTasks.selectAll().where { WorkerTasks.type eq type }
            }
            first(WorkerTask::wrapRow)
        }

    override suspend fun getWorkerTasks(): CustomResult<List<WorkerTask>, DatabaseError> =
        databaseSession.dbSearch {
            search {
                WorkerTasks.selectAll().orderBy(WorkerTasks.type, SortOrder.ASC)
            }
            map(WorkerTask::wrapRow)
        }

    override suspend fun upsertWorkerTasks(configs: List<WorkerTask>): CustomResult<Unit, DatabaseError> =
        databaseSession.dbQuery {
            configs.forEach { config ->
                WorkerTasks.upsert(WorkerTasks.type) { statement ->
                    statement[WorkerTasks.type] = config.type
                    statement[WorkerTasks.enabled] = config.isEnabled
                    statement[WorkerTasks.fetchSize] = config.fetchSize
                    statement[WorkerTasks.waitDurationMillis] = config.waitDurationMillis
                }
            }
        }
}
