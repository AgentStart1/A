/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getUserOverview
import com.storyteller_f.alphaheart.client.core.updateUserStatus
import com.storyteller_f.alphaheart.cloud.core.service.checkRootWritePermission
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.UpdateUserStatusBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class UserStatusTest {
    @Test
    fun `test ReadOnly user cannot create topic`() =
        test(mapOf("DISABLE_TEST_WS" to "true")) {
            val adminTuple = attachPanelSession()
            val userTuple =
                attachSession {
                    // As NORMAL (default), creation should work
                    val response1 = createTopic(ObjectType.USER, it.uid, "Test topic").getOrThrow()
                    assertNotNull(response1)
                }

            // Set to READ_ONLY via Admin
            loginPanelSession(adminTuple) {
                updateUserStatus(userTuple.uid, UpdateUserStatusBody(UserStatus.READ_ONLY)).getOrThrow()

                // Verify status in Overview
                val userOverview = getUserOverview(userTuple.uid).getOrThrow()
                assertEquals(UserStatus.READ_ONLY, userOverview.userInfo.status)
            }

            // As READ_ONLY, creation should be forbidden
            loginSession(userTuple) {
                assertEquals(UserStatus.READ_ONLY, getUserOverview().getOrThrow().userInfo.status)
                val error =
                    assertFailsWith<ServerErrorException> {
                        createTopic(ObjectType.USER, it.uid, "Test topic 2").getOrThrow()
                    }
                assertEquals(HttpStatusCode.Forbidden, error.status)
                assertIs<ForbiddenException>(
                    workerBackend.checkRootWritePermission(ObjectType.USER, it.uid, it.uid).exceptionOrNull(),
                )
            }
        }
}
