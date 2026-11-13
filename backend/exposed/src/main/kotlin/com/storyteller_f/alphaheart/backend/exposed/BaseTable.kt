/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed

import com.storyteller_f.alphaheart.backend.core.AdminDatabase
import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.CommunityDatabase
import com.storyteller_f.alphaheart.backend.core.ContainerDatabase
import com.storyteller_f.alphaheart.backend.core.DatabaseConnection
import com.storyteller_f.alphaheart.backend.core.FavoriteDatabase
import com.storyteller_f.alphaheart.backend.core.FileDatabase
import com.storyteller_f.alphaheart.backend.core.PanelAccountDatabase
import com.storyteller_f.alphaheart.backend.core.ReactionDatabase
import com.storyteller_f.alphaheart.backend.core.RoomDatabase
import com.storyteller_f.alphaheart.backend.core.SubscriptionDatabase
import com.storyteller_f.alphaheart.backend.core.TitleDatabase
import com.storyteller_f.alphaheart.backend.core.TopicDatabase
import com.storyteller_f.alphaheart.backend.core.UserDatabase
import com.storyteller_f.alphaheart.backend.core.WorkerTaskDatabase
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedAdminDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedCommunityDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedContainerDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedFavoriteDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedFileDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedPanelAccountDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedReactionDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedRoomDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedSubscriptionDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedTitleDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedTopicDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedUserDatabase
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedWorkerTaskDatabase
import com.storyteller_f.alphaheart.backend.exposed.tables.BackendConfigs
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.AssetType
import com.storyteller_f.shared.model.MemberPolicy
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import io.github.aakira.napier.Napier
import io.r2dbc.spi.R2dbcDataIntegrityViolationException
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert
import java.sql.SQLIntegrityConstraintViolationException

open class BaseTable : Table() {
    val id = customPrimaryKey("id")
    val createdTime = datetime("created_time")

    override val primaryKey = PrimaryKey(id)
}

fun Throwable.isDup(): Boolean {
    if (this is SQLIntegrityConstraintViolationException) return true
    if (this is R2dbcDataIntegrityViolationException) return true
    if (this is ExposedR2dbcException) {
        val throwable = cause
        if (throwable != null) {
            return throwable.isDup()
        }
    }
    return false
}

fun Table.customPrimaryKey(name: String) = long(name)

fun Table.objectType(name: String) = enumerationByName<ObjectType>(name, 20)
fun Table.objectStatus(name: String) = enumerationByName<ObjectStatus>(name, 20).default(ObjectStatus.NORMAL)
fun Table.memberStatus(name: String) = enumerationByName<MemberStatus>(name, 20)

fun Table.algoType(name: String) = enumerationByName<AlgoType>(name, 20).default(AlgoType.P256)
fun Table.passType(name: String) = enumerationByName<PassType>(name, 20).default(PassType.RAW)
fun Table.titleType(name: String) = enumerationByName<TitleType>(name, 10)

fun Table.titleStatus(name: String) = enumerationByName<TitleWorkStatus>(name, 10)
fun Table.assetType(name: String) = enumerationByName<AssetType>(name, 20)
fun Table.quotaType(name: String) = enumerationByName<QuotaType>(name, 20)

fun Table.taskRecordType(name: String) = enumerationByName<TaskRecordType>(name, 20)
fun Table.memberPolicy(name: String) = enumerationByName<MemberPolicy>(name, 20)
fun Table.uploadRecordStatus(name: String) = enumerationByName<UploadRecordStatus>(name, 20)

fun <T : Table> T.emoji() = varchar("emoji", 20)

class ExposedDatabase(val databaseSession: ExposedDatabaseSession) : CombinedDatabase {
    override val user: UserDatabase
        get() = ExposedUserDatabase(databaseSession, this)
    override val topic: TopicDatabase
        get() = ExposedTopicDatabase(databaseSession, this)
    override val title: TitleDatabase
        get() = ExposedTitleDatabase(databaseSession)
    override val container: ContainerDatabase
        get() = ExposedContainerDatabase(databaseSession, this)
    override val community: CommunityDatabase
        get() = ExposedCommunityDatabase(databaseSession, container)
    override val room: RoomDatabase
        get() = ExposedRoomDatabase(databaseSession, container)
    override val file: FileDatabase
        get() = ExposedFileDatabase(databaseSession)
    override val admin: AdminDatabase
        get() = ExposedAdminDatabase(databaseSession)
    override val panelAccount: PanelAccountDatabase
        get() = ExposedPanelAccountDatabase(databaseSession)

    override val reaction: ReactionDatabase
        get() = ExposedReactionDatabase(databaseSession)

    override val favorite: FavoriteDatabase
        get() = ExposedFavoriteDatabase(databaseSession)

    override val subscription: SubscriptionDatabase
        get() = ExposedSubscriptionDatabase(databaseSession)

    override val workerTask: WorkerTaskDatabase
        get() = ExposedWorkerTaskDatabase(databaseSession)

    override suspend fun init() {
        ExposedDatabaseFactory.init(databaseSession.database)
    }

    override suspend fun clean() {
        ExposedDatabaseFactory.clean(databaseSession.database)
    }

    override suspend fun getBackendConfig(key: String): CustomResult<String?, DatabaseError> {
        val configResult =
            databaseSession.dbSearch {
                search {
                    BackendConfigs.selectAll().where { BackendConfigs.key eq key }
                }
                first { row -> row[BackendConfigs.value] }
            }
        return configResult
    }

    override suspend fun upsertBackendConfig(key: String, value: String): CustomResult<Unit, DatabaseError> {
        val upsertResult: CustomResult<Unit, DatabaseError> =
            databaseSession.dbQuery {
                BackendConfigs.upsert(BackendConfigs.key) { statement ->
                    statement[BackendConfigs.key] = key
                    statement[BackendConfigs.value] = value
                }
            }
        return upsertResult
    }

    override suspend fun migration() {
        ExposedDatabaseFactory.migration(databaseSession.database)
    }
}

fun buildExposedDatabase(databaseConnection: DatabaseConnection): ExposedDatabase {
    Napier.i {
        "buildExposedDatabase $databaseConnection"
    }
    val database = ExposedDatabaseFactory.connect(databaseConnection)
    val databaseSession = ExposedDatabaseSession(database, null)
    return ExposedDatabase(databaseSession)
}
