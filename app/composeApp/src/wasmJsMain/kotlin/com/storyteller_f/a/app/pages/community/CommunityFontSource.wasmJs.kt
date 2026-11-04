/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import com.storyteller_f.a.app.ClientFileProvider
import com.storyteller_f.a.app.IAccountInstance
import com.storyteller_f.a.app.common.OpfsDownloadFileStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

internal actual fun createCommunityFontStore(
    instance: IAccountInstance,
    provider: ClientFileProvider,
): CommunityFontStore {
    val files = OpfsDownloadFileStore()
    return createDownloadedFontStore(instance, files, onLoaded = {
        try {
            files.retainFont(it)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
            Napier.w { "Unable to update font-cache retention metadata." }
        }
    }) {
        checkNotNull(provider.getDownloader())
    }
}
