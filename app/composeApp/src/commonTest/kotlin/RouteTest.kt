/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.pages.topic.TopicRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RouteTest {
    @Test
    fun testRouteMatch() {
        val string = "/room/a/test"

        val result = TopicRoute.parseRefUri(string)
        assertNotNull(result.first)
        assertEquals("test", result.second["aid"])
    }
}
