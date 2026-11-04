/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared.model

import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class FontSettings(val contentFontId: PrimaryKey? = null, val codeFontId: PrimaryKey? = null)
