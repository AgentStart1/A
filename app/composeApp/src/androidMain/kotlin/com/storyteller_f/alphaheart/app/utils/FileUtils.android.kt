/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun saveTextToFile(suggestedName: String, extension: String, extensions: Set<String>, content: String) {
    val file = FileKit.openFileSaver(suggestedName, extension, extensions)
    file?.write(content.encodeToByteArray())
}
