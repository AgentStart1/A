/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.preview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.storyteller_f.alphaheart.app.ProvideFontIcon
import com.storyteller_f.alphaheart.app.pages.user.SelfDialog
import com.storyteller_f.alphaheart.app.pages.user.UserDialog
import com.storyteller_f.alphaheart.client.core.FixedLoadingHandler
import com.storyteller_f.shared.model.UserInfo
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@PreviewLightDark
@Composable
fun UserDialogPreview() {
    PreviewContextConfigurationEffect()
    ProvideFontIcon {
        UserDialog(UserInfo.EMPTY, true) { }
    }
}

@Preview
@Composable
fun SelfDialogPreview() {
    PreviewContextConfigurationEffect()
    ProvideFontIcon {
        SelfDialog(
            UserInfo.EMPTY.copy(nickname = "Nickname", aid = "aid"),
            true,
            FixedLoadingHandler(),
            {},
        ) { }
    }
}
