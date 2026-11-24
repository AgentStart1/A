/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.file

import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime

internal fun fileBasicInfoItems(info: com.storyteller_f.shared.model.FileInfo): List<Pair<String, String>> =
    buildList {
        add("id" to info.id.toString())
        add("name" to info.name)
        add("fullName" to info.fullName)
        add("url" to info.url)
        add("contentType" to info.contentType)
        add("size" to info.size.toString())
        add("owner" to info.owner.toString())
        add("ownerType" to info.ownerType.name)
        add("lastModified" to info.lastModified.formatDisplayTime())
        val dimension = info.dimension?.takeIf { it.width > 0 && it.height > 0 }
        add("dimension" to (dimension?.let { "${it.width}x${it.height}" } ?: "null"))
        add("readOnly" to info.readOnly.toString())
    }
