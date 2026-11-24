/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("com.storyteller_f.alphaheart.app.pages.file.FileExplorerRoute")
@Serializable
sealed interface FileExplorerRoute : NavKey {
    @SerialName("com.storyteller_f.alphaheart.app.pages.file.FileExplorerRoute.Uploaded")
    @Serializable
    data object Uploaded : FileExplorerRoute {
        override fun toString(): String = "/uploaded"
    }

    @SerialName("com.storyteller_f.alphaheart.app.pages.file.FileExplorerRoute.UploadRecord")
    @Serializable
    data object UploadRecord : FileExplorerRoute {
        override fun toString(): String = "/upload-record"
    }

    @SerialName("com.storyteller_f.alphaheart.app.pages.file.FileExplorerRoute.DownloadRecord")
    @Serializable
    data object DownloadRecord : FileExplorerRoute {
        override fun toString(): String = "/download-record"
    }
}
