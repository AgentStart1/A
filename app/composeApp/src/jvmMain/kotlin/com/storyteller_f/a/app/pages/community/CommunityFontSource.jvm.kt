/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import com.storyteller_f.a.app.ClientFileProvider
import com.storyteller_f.a.app.IAccountInstance
import com.storyteller_f.a.app.common.createDownloadFileStore

internal actual fun createCommunityFontStore(
    instance: IAccountInstance,
    provider: ClientFileProvider,
): CommunityFontStore =
    createDownloadedFontStore(instance, createDownloadFileStore()) { checkNotNull(provider.getDownloader()) }
