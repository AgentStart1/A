/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.isDownloadUrlExpired
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class DownloadUrlTest {
    private val now = Instant.parse("2026-11-05T00:01:00Z")

    @Test
    fun permanentUrlsDoNotRequireRefresh() {
        assertFalse(isDownloadUrlExpired("https://fonts.invalid/font.ttf", now))
    }

    @Test
    fun signaturesAreValidBeforeTheirExpiry() {
        assertFalse(isDownloadUrlExpired(signedUrl("20261105T000000Z", "61"), now))
    }

    @Test
    fun signaturesExpireAtTheDeclaredBoundary() {
        assertTrue(isDownloadUrlExpired(signedUrl("20261105T000000Z", "60"), now))
    }

    @Test
    fun malformedSignatureDatesRequireRefresh() {
        assertTrue(isDownloadUrlExpired(signedUrl("20261305T000000Z", "60"), now))
    }

    @Test
    fun malformedDurationsRequireRefresh() {
        assertTrue(isDownloadUrlExpired(signedUrl("20261105T000000Z", "invalid"), now))
    }

    @Test
    fun missingSignatureDurationRequiresRefresh() {
        assertTrue(isDownloadUrlExpired("https://fonts.invalid/font?X-Amz-Date=20261105T000000Z", now))
    }

    @Test
    fun durationsBeyondTheProtocolLimitRequireRefresh() {
        assertTrue(isDownloadUrlExpired(signedUrl("20261105T000000Z", "604801"), now))
    }

    private fun signedUrl(date: String, seconds: String): String =
        "https://fonts.invalid/font?X-Amz-Date=$date&X-Amz-Expires=$seconds"
}
