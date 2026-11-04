/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.common

import kotlinx.coroutines.Dispatchers

internal actual fun createDownloadFileStore(): DownloadFileStore = DiskDownloadFileStore(Dispatchers.IO)
