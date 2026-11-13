/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.type.PrimaryKey

data class UserDevice(val uid: PrimaryKey, val endpointUrl: String) {
    companion object
}
