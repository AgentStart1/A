/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import com.storyteller_f.a.app.UIViewModel

internal actual fun createCommunityFontStore(viewModel: UIViewModel): CommunityFontStore =
    createDownloadedFontStore(viewModel)
