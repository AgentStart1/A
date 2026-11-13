/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.minio

import kotlin.test.Test
import kotlin.test.assertEquals

class UrlTest {
    @Test
    fun `test url replace`() {
        val host = "https://test.com:9090"
        val url = "http://test.com:909/test?a=1&b=2"
        val newUrl = replaceUrl(host, url)
        assertEquals("https://test.com:9090/test?a=1&b=2", newUrl)
    }

    @Test
    fun `test url replace 443`() {
        val host = "https://test.com"
        val url = "http://test.com:909/test?a=1&b=2"
        val newUrl = replaceUrl(host, url)
        assertEquals("https://test.com:443/test?a=1&b=2", newUrl)
    }

    @Test
    fun `test url replace 80`() {
        val host = "http://test.com"
        val url = "http://test.com:909/test?a=1&b=2"
        val newUrl = replaceUrl(host, url)
        assertEquals("http://test.com:80/test?a=1&b=2", newUrl)
    }
}
