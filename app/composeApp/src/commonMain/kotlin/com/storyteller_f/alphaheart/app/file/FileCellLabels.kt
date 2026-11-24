/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.file

internal data class FileCellLabels(
    val modified: String,
    val size: String,
    val dimension: String?,
    val warning: String?,
) {
    fun describe(name: String, type: String) =
        listOfNotNull(
            name,
            type,
            modified,
            size,
            dimension,
            warning,
        ).joinToString(", ")
}
