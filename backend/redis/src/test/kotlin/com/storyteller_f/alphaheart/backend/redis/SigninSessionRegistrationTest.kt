/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.redis

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.service.SigninSessionServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.buildSigninSessionService
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class SigninSessionRegistrationTest {
    @Test
    fun discoversProviderAndRequiresEndpoint() {
        val defaults = MergedEnv(emptyList())
        val factories = ServiceLoader.load(SigninSessionServiceFactory::class.java).toList()
        val matched = factories.filter { it.match(defaults) }
        assertEquals(1, matched.size)
        assertTrue(matched.single() is RedisSigninSessionServiceFactory)
        assertFailsWith<IllegalArgumentException> { buildSigninSessionService(defaults, "user") }
        val unsupported = MergedEnv(listOf(mapOf("SIGNIN_SESSION_SERVICE" to "unsupported")))
        assertFailsWith<IllegalStateException> { buildSigninSessionService(unsupported, "user") }
    }
}
