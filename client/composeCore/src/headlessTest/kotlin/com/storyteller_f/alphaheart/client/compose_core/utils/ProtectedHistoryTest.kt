/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ProtectedHistoryTest {
    @Test
    fun restoresSavedPrivateKeys() =
        runTest {
            var protected: String? = null
            val manager = ProtectedSessionHistoryManager("test", { protected }, { _, value -> protected = value })
            assertTrue(manager.getSavedSession().alias.isEmpty())
            manager.addSession(ACCOUNT.toRawUserPassInfo())
            val restored = ProtectedSessionHistoryManager("test", { protected }, { _, value -> protected = value })
            assertEquals(listOf("account"), restored.getSavedSession().alias)
            assertEquals(
                "private",
                (assertNotNull(restored.buildSession("account")) as RawUserPass).rawUSerPass.authKey.pemPrivateKey,
            )
        }

    @Test
    fun logoutClearsCurrentAndRetainsLastAccount() =
        runTest {
            var protected: String? = null
            val manager = ProtectedSessionHistoryManager("test", { protected }, { _, value -> protected = value })
            manager.addSession(ACCOUNT.toRawUserPassInfo())
            manager.exitSession("account")
            assertEquals(null, manager.getSavedSession().history?.current)
            assertEquals("account", manager.getSavedSession().history?.last)
        }

    @Test
    fun removeClearsSavedAccount() =
        runTest {
            var protected: String? = null
            val manager = ProtectedSessionHistoryManager("test", { protected }, { _, value -> protected = value })
            manager.addSession(ACCOUNT.toRawUserPassInfo())
            manager.removeSession("account")
            assertTrue(manager.getSavedSession().alias.isEmpty())
        }

    @Test
    fun failedWritePreservesSavedHistory() =
        runTest {
            val protected =
                Json.encodeToString(
                    ProtectedSessionHistory(listOf(ACCOUNT), SessionHistory("account", "account")),
                )
            val manager = ProtectedSessionHistoryManager("test", { protected }, { _, _ -> error("Unavailable") })
            assertFailsWith<IllegalStateException> { manager.exitSession("account") }
            assertEquals("account", manager.getSavedSession().history?.current)
        }

    @Test
    fun unverifiedWritePreservesSavedHistory() =
        runTest {
            val protected =
                Json.encodeToString(
                    ProtectedSessionHistory(listOf(ACCOUNT), SessionHistory("account", "account")),
                )
            val unverified = ProtectedSessionHistoryManager("test", { protected }, { _, _ -> })
            assertFailsWith<IllegalStateException> { unverified.exitSession("account") }
            assertEquals("account", unverified.getSavedSession().history?.current)
        }

    companion object {
        private val ACCOUNT = ConvertedRawUserPassInfo(AlgoType.P256, "account", "private", "private-der", "public-der")
    }
}
