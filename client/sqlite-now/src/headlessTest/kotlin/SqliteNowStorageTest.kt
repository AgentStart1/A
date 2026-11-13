package com.storyteller_f.alphaheart.client.sqlitenow

import PlatformHeadlessTest
import com.storyteller_f.alphaheart.client.sqlitenow.db.AppDatabase
import com.storyteller_f.alphaheart.client.sqlitenow.db.VersionBasedDatabaseMigrations
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.UserCollection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SqliteNowStorageTest : PlatformHeadlessTest() {

    @Test
    fun testUserInfoStorage() = runTest {
        val appDatabase = AppDatabase(":memory:", VersionBasedDatabaseMigrations())
        appDatabase.open()
        val storage: ModelStorage = SqliteNowModelStorage(appDatabase)

        val userInfo = UserInfo(
            id = 1,
            address = "TestAddress",
            aid = "TestAid",
            nickname = "TestUser",
            avatar = null
        )
        storage.user.saveToDefault(userInfo)

        val documentMap = storage.user.observeDatum("1").first()
        assertNotNull(documentMap)
        assertEquals("TestUser", documentMap.nickname)

        val loadedUser = storage.user.getDocument(UserCollection.Users, "1")
        assertNotNull(loadedUser)
        assertEquals("TestUser", loadedUser.nickname)
    }

    @Test
    fun testAutoOpen() = runTest {
        val appDatabase = AppDatabase(":memory:", VersionBasedDatabaseMigrations())
        // Explicitly NOT calling appDatabase.open()
        val storage: ModelStorage = SqliteNowModelStorage(appDatabase)

        val userInfo = UserInfo(
            id = 1,
            address = "TestAddress",
            aid = "TestAid",
            nickname = "TestUser",
            avatar = null
        )
        // This should not crash now because we call open() internally
        storage.user.saveToDefault(userInfo)

        val documentMap = storage.user.observeDatum("1").first()
        assertNotNull(documentMap)
        assertEquals("TestUser", documentMap.nickname)
    }
}
