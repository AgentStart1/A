/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import kotlin.test.Test
import kotlin.test.assertEquals

class RequestLogTest {
    @Test
    fun retainsRequestFieldsOnOneLine() {
        val log =
            formatRequestLog(
                "Url" to "/ping",
                "Status" to "200 OK",
                "HTTP method" to "GET",
                "Query" to "check=single-line",
                "Headers" to "{X-Request-ID=[synthetic-id]}",
                "Ip" to "(192.0.2.1, EX)",
            )
        assertEquals(
            "Url: /ping, Status: 200 OK, HTTP method: GET, Query: check=single-line, " +
                "Headers: {X-Request-ID=[synthetic-id]}, Ip: (192.0.2.1, EX)",
            log,
        )
        assertEquals(1, log.lineSequence().count())
    }
}
