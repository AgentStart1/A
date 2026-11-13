/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlin.test.Test
import kotlin.test.assertFalse

internal class PanelClientLifetimeTest {
    @Test
    fun cancellationClosesExistingClient() {
        val owner = Job()
        HttpClient().use { client ->
            closePanelClientWithScope(CoroutineScope(Job(owner)), client)
            owner.cancel()
            assertFalse(client.coroutineContext.job.isActive)
        }
    }

    @Test
    fun cancellationDuringCreationClosesClient() {
        val owner = Job()
        val scope = CoroutineScope(Job(owner))
        HttpClient().use { client ->
            owner.cancel()
            closePanelClientWithScope(scope, client)
            assertFalse(client.coroutineContext.job.isActive)
        }
    }

    @Test
    fun cancelledOwnerClosesLateClient() {
        val owner = Job()
        owner.cancel()
        HttpClient().use { client ->
            closePanelClientWithScope(CoroutineScope(Job(owner)), client)
            assertFalse(client.coroutineContext.job.isActive)
        }
    }
}
