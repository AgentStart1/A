/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.shared.model.RoomInfo

class RoomCellPreviewProvider : PreviewParameterProvider<RoomInfo> {
    override val values: Sequence<RoomInfo>
        get() =
            sequenceOf(
                RoomInfo.EMPTY.copy(name = "Room Name", latestTopic = 1, lastRead = 0),
            )
}
