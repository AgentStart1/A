/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@OptIn(DelicateCoroutinesApi::class)
val uiViewModel by lazy {
    UIViewModel(GlobalScope, AppConfig.WS_SERVER_URL, AppConfig.SERVER_URL).also(::observeResourceTransfers)
}
