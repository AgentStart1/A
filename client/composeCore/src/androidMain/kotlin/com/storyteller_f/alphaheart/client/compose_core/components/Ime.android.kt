/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.ui.Modifier

@OptIn(ExperimentalLayoutApi::class)
actual fun Modifier.imeAnimation(): Modifier = imeNestedScroll()
