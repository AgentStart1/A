/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidPublicAvatarTransferTest {
    @Test
    fun publicAvatarSharesOneTransferAcrossConcurrentAndLaterRequests() =
        verifyPublicAvatarTransfers(RuntimeEnvironment.getApplication())
}
