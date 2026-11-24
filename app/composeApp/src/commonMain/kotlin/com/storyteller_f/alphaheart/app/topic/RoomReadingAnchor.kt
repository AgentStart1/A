/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Keeps the latest edge anchored during inserts while history retains its visible message key. */
internal fun LazyListScope.addLatestRoomAnchor() {
    item("latest-room-anchor") { Spacer(Modifier.height(1.dp)) }
}
