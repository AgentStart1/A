/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getFileInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.FileInfo
import io.ktor.http.Url
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private const val MAX_SIGNED_URL_SECONDS = 604_800L
private val signedDate = Regex("""(\d{4})(\d{2})(\d{2})T(\d{2})(\d{2})(\d{2})Z""")

/** MinIO uses SigV4 query expiry; filesystem URLs without these parameters have no declared expiry. */
internal fun isDownloadUrlExpired(url: String, now: Instant): Boolean {
    val parameters = Url(url).parameters
    val date = parameters["X-Amz-Date"]
    val expires = parameters["X-Amz-Expires"]
    if (date == null && expires == null) return false
    val duration = expires?.toLongOrNull()?.takeIf { it in 1..MAX_SIGNED_URL_SECONDS } ?: return true
    if (date == null || !signedDate.matches(date)) return true
    val issued = Instant.parseOrNull(signedDate.replace(date, "$1-$2-$3T$4:$5:$6Z")) ?: return true
    return now >= issued + duration.seconds
}

/** Resolve once before transferring bytes, retaining the requested version and existing partial source. */
internal suspend fun UserSessionManager.refreshExpiredDownloadUrl(file: FileInfo): FileInfo {
    if (!isDownloadUrlExpired(file.url, Clock.System.now())) return file
    val latest = getFileInfo(file.id).getOrThrow()
    check(latest.id == file.id && getDownloadRevision(latest) == getDownloadRevision(file)) {
        "Download file changed while refreshing its URL"
    }
    check(!isDownloadUrlExpired(latest.url, Clock.System.now())) { "Refreshed download URL is still expired" }
    return file.copy(url = latest.url)
}
