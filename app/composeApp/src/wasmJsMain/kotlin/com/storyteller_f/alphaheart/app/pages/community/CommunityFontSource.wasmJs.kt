/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.OpfsFileEngine
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException

internal actual fun createCommunityFontStore(viewModel: UIViewModel): CommunityFontStore =
    createDownloadedFontStore(viewModel, onLoaded = {
        try {
            (viewModel.resourceFiles as OpfsFileEngine).retainFont(it)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IllegalStateException) {
            Napier.w { "Unable to update font-cache retention metadata." }
        }
    })
