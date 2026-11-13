/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.paging.PagingSource
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy.Companion.REPLACE
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import kotlinx.coroutines.flow.Flow

@Database(entities = [CommonEntity::class, UploadEntity::class, DownloadEntity::class], version = 2)
@ConstructedBy(AppDatabaseConstructor::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getCommonDao(): CommonDao
    abstract fun getUploadDao(): UploadDao
    abstract fun getDownloadDao(): DownloadDao
}

// The Room compiler generates the `actual` implementations.
@Suppress(
    "NO_ACTUAL_FOR_EXPECT",
    "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING",
    "KotlinNoActualForExpect",
    "RedundantSuppression",
)
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

expect fun getRoomDatabase(scope: String): AppDatabase

@Dao
interface CommonDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(item: CommonEntity)

    @Insert(onConflict = REPLACE)
    suspend fun insertAll(items: List<CommonEntity>)

    @Transaction
    suspend fun replaceCollection(collection: String, items: List<CommonEntity>, details: List<CommonEntity>) {
        clean(collection)
        insertAll(details)
        insertAll(items)
    }

    @Query("delete from CommonEntity where collection = :collection and id = :id")
    suspend fun delete(collection: String, id: String)

    @Query("select * from CommonEntity where collection = :collection and id = :id")
    suspend fun get(collection: String, id: String): CommonEntity?

    @Query("SELECT * FROM CommonEntity where collection = :collection and id = :id")
    fun getAsFlow(collection: String, id: String): Flow<CommonEntity?>

    @Query("select * from CommonEntity where collection = :collection order by seq asc")
    fun getAsSource(collection: String): PagingSource<Int, CommonEntity>

    @Query("delete from CommonEntity where collection = :collection")
    suspend fun clean(collection: String)

    @Query("update CommonEntity set data = :data where collection = :collection and id = :id")
    suspend fun updateData(collection: String, id: String, data: String)

    @Query("select COALESCE(MAX(seq), -1) from CommonEntity where collection = :collection")
    suspend fun getMaxSeq(collection: String): Long

    @Query("select COALESCE(MIN(seq), 1) from CommonEntity where collection = :collection")
    suspend fun getMinSeq(collection: String): Long

    suspend fun saveLast(item: CommonEntity) {
        val maxSeq = getMaxSeq(item.collection)
        insert(item.copy(seq = maxSeq + 1))
    }

    suspend fun saveFirst(item: CommonEntity) {
        val minSeq = getMinSeq(item.collection)
        insert(item.copy(seq = minSeq - 1))
    }
}

@Entity(primaryKeys = ["collection", "id"], indices = [Index("collection", "seq", unique = false)])
data class CommonEntity(val id: String, val collection: String, val data: String, val seq: Long = 0) {
    constructor(id: Long, collection: String, data: String, seq: Long = 0) : this(id.toString(), collection, data, seq)
}

@Dao
interface UploadDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(item: UploadEntity)

    @Query("select * from UploadEntity where collection = :collection and pathHash = :pathHash")
    suspend fun get(collection: String, pathHash: String): UploadEntity?

    @Query("select * from UploadEntity where collection = :collection order by id desc")
    fun getAsSource(collection: String): PagingSource<Int, UploadEntity>

    @Query("select * from UploadEntity where collection = :collection and pathHash = :pathHash")
    fun getAsFlow(collection: String, pathHash: String): Flow<UploadEntity?>

    @Query("delete from UploadEntity where collection = :collection and pathHash = :pathHash")
    suspend fun delete(collection: String, pathHash: String)

    @Query("delete from UploadEntity where collection = :collection")
    suspend fun clean(collection: String)

    @Query("update UploadEntity set data = :data where collection = :collection and id = :id")
    suspend fun updateData(collection: String, id: String, data: String)
}

@Entity(primaryKeys = ["collection", "id"], indices = [Index("collection", "pathHash", unique = true)])
data class UploadEntity(val id: String, val collection: String, val data: String, val pathHash: String)

@Dao
interface DownloadDao {
    @Insert(onConflict = REPLACE)
    suspend fun insert(item: DownloadEntity)

    @Query("select * from DownloadEntity where collection = :collection and id = :id")
    suspend fun getById(collection: String, id: String): DownloadEntity?

    @Query("select * from DownloadEntity where collection = :collection and fileId = :fileId")
    suspend fun getByFileId(collection: String, fileId: Long): DownloadEntity?

    @Query("SELECT * FROM DownloadEntity where collection = :collection and id = :id")
    fun getByIdAsFlow(collection: String, id: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM DownloadEntity where collection = :collection and fileId = :fileId")
    fun getByFileIdAsFlow(collection: String, fileId: Long): Flow<DownloadEntity?>

    @Query("select * from DownloadEntity where collection = :collection order by id desc")
    fun getAsSource(collection: String): PagingSource<Int, DownloadEntity>

    @Query("delete from DownloadEntity where collection = :collection")
    suspend fun clean(collection: String)
}

@Entity(primaryKeys = ["collection", "id"], indices = [Index("collection", "fileId", unique = true)])
data class DownloadEntity(val id: String, val collection: String, val fileId: Long, val data: String)
