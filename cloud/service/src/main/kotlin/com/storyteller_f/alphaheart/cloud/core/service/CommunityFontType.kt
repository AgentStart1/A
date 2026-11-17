/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

internal fun isCommunityFontType(contentType: String): Boolean =
    contentType.startsWith("font/") || contentType.startsWith("application/font-") ||
        contentType == "application/x-font-otf" || contentType == "application/x-font-ttf" ||
        contentType == "application/vnd.ms-opentype"
