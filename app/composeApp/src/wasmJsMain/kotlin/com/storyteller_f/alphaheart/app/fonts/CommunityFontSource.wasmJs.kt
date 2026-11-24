/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.fonts

import com.storyteller_f.alphaheart.app.UIViewModel

// Community font construction blocks the browser UI thread. Keep the loader available for browser tests.
internal actual fun createCommunityFontStore(viewModel: UIViewModel): CommunityFontStore? = null
