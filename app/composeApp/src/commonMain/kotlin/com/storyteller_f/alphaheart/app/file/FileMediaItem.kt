/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.file

import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.shared.model.FileInfo

internal fun FileInfo.mediaItem() = RemoteMediaItem(id.toString(), url, contentType, name)
