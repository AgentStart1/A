/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.client.compose_core.components.RefCellStateView
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.type.PrimaryKey

@Composable
fun CommunityRefCell(communityId: PrimaryKey, onClick: ((CommunityInfo) -> Unit)? = null) {
    val handler = LocalRefCellHandlerProvider.current.communityHandler(communityId)
    CommunityRefCellInternal(handler, onClick)
}

@Composable
fun CommunityRefCell(communityAid: String, onClick: ((CommunityInfo) -> Unit)? = null) {
    val handler = LocalRefCellHandlerProvider.current.communityHandler(communityAid)
    CommunityRefCellInternal(handler, onClick)
}

@Composable
private fun CommunityRefCellInternal(
    handler: LoadingHandler<CommunityInfo>,
    onClick: ((CommunityInfo) -> Unit)? = null,
) {
    val communityInfo by handler.data.collectAsState()
    val shape = RoundedCornerShape(10.dp)
    val appNavFactory = LocalAppNavFactory.current
    RefCellStateView(
        handler,
        modifier =
        Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, shape)
            .clip(shape)
            .clickable {
                communityInfo?.let { onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoCommunity(it.id, false) }
            }
            .padding(10.dp),
    ) {
        CommunityCell(it, true, onClick)
    }
}
