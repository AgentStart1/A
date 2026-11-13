/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.alphaheart.backend.core.service.SigninSessionService
import io.ktor.server.sessions.SessionStorage

/** Keeps Ktor's transport contract out of the backend storage services. */
internal class SigninSessionStorage(private val service: SigninSessionService) : SessionStorage {
    override suspend fun read(id: String): String = service.read(id)
    override suspend fun write(id: String, value: String) = service.write(id, value)
    override suspend fun invalidate(id: String) = service.invalidate(id)
}
