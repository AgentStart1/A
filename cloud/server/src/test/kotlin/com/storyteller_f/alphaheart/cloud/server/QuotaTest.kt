/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.client.core.getQuotaInfo
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals

class QuotaTest {
    @Test
    fun `test default user file quota`() =
        test {
            attachSession {
                val tuple = ObjectTuple(it.uid, ObjectType.USER)
                val quota = getQuotaInfo(tuple, QuotaType.FILE).getOrThrow()
                assertEquals(it.uid, quota.ownerId)
                assertEquals(ObjectType.USER, quota.ownerType)
                assertEquals(QuotaType.FILE, quota.quotaType)
                assertEquals(1024L * 1024 * 1024, quota.total) // 默认 1GB
                assertEquals(0L, quota.used)
                assertEquals(null, quota.lockId)
            }
        }

    @Test
    fun `test used increases after upload and not locking`() =
        test {
            attachSession {
                val mediaTarget = it.uid ob ObjectType.USER
                val before = getQuotaInfo(mediaTarget).getOrThrow()

                val data = getUploadDataFromText("hello")
                val uploadSize = data.size
                upload(mediaTarget, data).getOrThrow()

                val after = getQuotaInfo(mediaTarget).getOrThrow()
                assertEquals(before.total, after.total)
                assertEquals(before.used + uploadSize, after.used)
                assertEquals(null, after.lockId)
            }
        }
}
