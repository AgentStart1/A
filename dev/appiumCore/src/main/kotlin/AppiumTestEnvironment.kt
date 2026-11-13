/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.BackendFixture
import com.storyteller_f.alphaheart.test.useBackendFixture

/** Starts the backend topology shared by desktop and browser Appium tests. */
internal suspend fun runAppiumTestEnvironment(
    report: AppiumTestReport,
    allowedOrigins: String = "",
    filesystemPublicUrl: String? = null,
    block: suspend (AppiumPorts) -> Unit,
) {
    useBackendFixture(
        report.directory.resolve("logs"),
        environment = {
            buildMap {
                put("SIGNIN_ALLOWED_ORIGINS", allowedOrigins)
                filesystemPublicUrl?.let { put("FILESYSTEM_PUBLIC_URL", it) }
            }
        },
    ) { backend ->
        block(createAppiumPorts(backend))
    }
}

/** SQL is confined to the isolated fixture; business setup belongs to each caller. */
internal fun createAppiumPorts(backend: BackendFixture): AppiumPorts =
    AppiumPorts(backend.serverPort, backend.wsPort, expireUserSessions = {
        val result = backend.services.redis.execInContainer("redis-cli", "EVAL", EXPIRE_USER_SESSIONS, "0")
        check(result.exitCode == 0) { "Test session expiry failed" }
    }, queryFixtureDatabase = { statement ->
        val postgres = backend.services.postgres
        val result =
            postgres.execInContainer(
                "psql",
                "-U",
                postgres.username,
                "-d",
                postgres.databaseName,
                "-At",
                "-c",
                statement,
            )
        check(result.exitCode == 0) { "Fixture database query failed" }
        result.stdout.trim()
    }, filesystemHttp = backend.services.filesystemHttpPort)

// Move the isolated fixture's stored deadline into the past; retain the Cookie and Redis key.
private const val EXPIRE_USER_SESSIONS = """
    for _, key in ipairs(redis.call('KEYS', 'signin:user:*')) do
        if redis.call('TYPE', key).ok == 'string' then
            local record = cjson.decode(redis.call('GET', key))
            record.absolute = 0
            redis.call('SET', key, cjson.encode(record), 'KEEPTTL')
        end
    end
    return 1
"""
