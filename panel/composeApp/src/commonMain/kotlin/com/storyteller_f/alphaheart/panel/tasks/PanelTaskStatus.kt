/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.tasks

internal fun failureTypeForStatus(status: Boolean?, failureType: String?): String? =
    failureType.takeIf { status == false }
