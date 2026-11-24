/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayHostState
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayTrigger

internal val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState?> { null }

internal val LocalOverlayTrigger = staticCompositionLocalOf<OverlayTrigger?> { null }

internal val LocalAdaptiveOverlay = staticCompositionLocalOf { false }
