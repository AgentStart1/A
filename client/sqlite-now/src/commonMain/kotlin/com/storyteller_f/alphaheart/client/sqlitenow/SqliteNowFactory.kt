package com.storyteller_f.alphaheart.client.sqlitenow

import com.storyteller_f.alphaheart.client.sqlitenow.db.AppDatabase
import com.storyteller_f.storage.*
import dev.goquick.sqlitenow.core.DatabaseMigrations
import dev.goquick.sqlitenow.core.SafeSQLiteConnection

class SqliteNowModelStorage(val appDatabase: AppDatabase) : ModelStorage {
    override val user: UserInfoStorage = SqliteNowUserInfoStorage(appDatabase)
    override val community: CommunityInfoStorage = SqliteNowCommunityInfoStorage(appDatabase)
    override val topic: TopicInfoStorage = SqliteNowTopicInfoStorage(appDatabase)
    override val title: TitleInfoStorage = SqliteNowTitleInfoStorage(appDatabase)
    override val room: RoomInfoStorage = SqliteNowRoomInfoStorage(appDatabase)
    override val member: MemberInfoStorage = SqliteNowMemberInfoStorage(appDatabase)
    override val remoteKey: RemoteKeyStorage = RemoteKeySqliteNowStorage(appDatabase)
    override val reaction: ReactionInfoStorage = SqliteNowReactionInfoStorage(appDatabase)
    override val childAccount: ChildAccountStorage = SqliteNowChildAccountStorage(appDatabase)
    override val fileInfo: FileInfoStorage = SqliteNowFileInfoStorage(appDatabase)
    override val download: DownloadInfoStorage = SqliteNowDownloadInfoStorage(appDatabase)
    override val upload: UploadInfoStorage = SqliteNowUploadInfoStorage(appDatabase)
    override val overview: OverviewStorage = SqliteNowOverviewStorage(appDatabase)
    override val userOverview: UserOverviewStorage = SqliteNowUserOverviewStorage(appDatabase)
    override val favorite: UserFavoriteStorage = SqliteNowUserFavoriteStorage(appDatabase)
    override val subscription: UserSubscriptionStorage = SqliteNowUserSubscriptionStorage(appDatabase)
    override val userReactionRecord: UserReactionRecordStorage = SqliteNowUserReactionRecordStorage(appDatabase)
    override val userLog: UserLogInfoStorage = SqliteNowUserLogInfoStorage(appDatabase)
    override val uploadRecord: UploadRecordInfoStorage = SqliteNowUploadRecordInfoStorage(appDatabase)
    override val fileRef: FileRefInfoStorage = SqliteNowFileRefInfoStorage(appDatabase)
    override val panelLog: PanelLogInfoStorage = SqliteNowPanelLogInfoStorage(appDatabase)
    override val taskRecord: TaskRecordInfoStorage = SqliteNowTaskRecordInfoStorage(appDatabase)
}

fun getSqliteNowModelStorage(name: String): SqliteNowModelStorage {
    val db = AppDatabase(
        dbName = "$name.db",
        migration = object : DatabaseMigrations {
            override suspend fun applyMigration(conn: SafeSQLiteConnection, currentVersion: Int): Int {
                return currentVersion
            }
        },
        debug = true
    )
    return SqliteNowModelStorage(db)
}
