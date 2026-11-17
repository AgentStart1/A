/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.reference_type_community
import com.storyteller_f.alphaheart.app.reference_type_file
import com.storyteller_f.alphaheart.app.reference_type_panel
import com.storyteller_f.alphaheart.app.reference_type_room
import com.storyteller_f.alphaheart.app.reference_type_title
import com.storyteller_f.alphaheart.app.reference_type_topic
import com.storyteller_f.alphaheart.app.reference_type_user
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun referenceTypeLabel(type: ObjectType): String =
    stringResource(
        when (type) {
            ObjectType.COMMUNITY -> Res.string.reference_type_community
            ObjectType.ROOM -> Res.string.reference_type_room
            ObjectType.TOPIC -> Res.string.reference_type_topic
            ObjectType.USER -> Res.string.reference_type_user
            ObjectType.TITLE -> Res.string.reference_type_title
            ObjectType.FILE -> Res.string.reference_type_file
            ObjectType.PANEL_ACCOUNT -> Res.string.reference_type_panel
        },
    )
