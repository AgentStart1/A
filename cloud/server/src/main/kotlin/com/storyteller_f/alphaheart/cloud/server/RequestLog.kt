/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

/** Joins request fields without introducing line breaks. */
internal fun formatRequestLog(vararg fields: Pair<String, String>): String =
    fields.joinToString(", ") { (name, value) -> "$name: $value" }
