/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import java.util.*

actual fun getCurrentLanguage(): String = Locale.getDefault().getDisplayLanguage(Locale.CHINESE)
