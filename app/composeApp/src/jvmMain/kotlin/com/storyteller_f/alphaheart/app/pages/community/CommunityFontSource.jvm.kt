/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import com.storyteller_f.alphaheart.app.UIViewModel

internal actual fun createCommunityFontStore(viewModel: UIViewModel): CommunityFontStore =
    createDownloadedFontStore(viewModel)
