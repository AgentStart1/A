/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.storage.FileCollection
import com.storyteller_f.storage.FileInfoStorage

/** Updates the original account's observed collection after the server has completed an upload. */
internal suspend fun cacheUploadedFiles(files: List<FileInfo>, storage: FileInfoStorage) {
    files.forEach { file ->
        storage.saveToDefault(file)
        storage.saveFirst(FileCollection.FileList(file.owner), file)
    }
}
