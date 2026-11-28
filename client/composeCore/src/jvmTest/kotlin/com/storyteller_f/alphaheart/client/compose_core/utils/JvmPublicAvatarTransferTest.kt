/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import coil3.PlatformContext
import kotlin.test.Test

class JvmPublicAvatarTransferTest {
    @Test
    fun publicAvatarSharesOneTransferAcrossConcurrentAndLaterRequests() =
        verifyPublicAvatarTransfers(PlatformContext.INSTANCE)
}
