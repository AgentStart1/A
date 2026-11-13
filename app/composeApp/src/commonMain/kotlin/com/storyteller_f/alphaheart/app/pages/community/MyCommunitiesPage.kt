/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.CommunityScreen
import com.storyteller_f.alphaheart.app.common.createJoinedCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createJoinedCommunitiesWithPosterViewModel
import com.storyteller_f.alphaheart.app.common.hasRouteFlow
import com.storyteller_f.alphaheart.app.community_view_all
import com.storyteller_f.alphaheart.app.community_view_poster
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityPoster
import com.storyteller_f.alphaheart.client.compose_core.components.LayoutDefaults
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.CommunityInfo
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyCommunitiesPage() {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val tabs =
                listOf(
                    stringResource(Res.string.community_view_all),
                    stringResource(Res.string.community_view_poster),
                )
            PrimaryTabRow(
                pagerState.currentPage,
                modifier = if (maxWidth >= 640.dp) Modifier.widthIn(max = 320.dp) else Modifier,
            ) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = pagerState.currentPage == i, onClick = {
                        scope.launch { pagerState.scrollToPage(i) }
                    }) { Text(label, modifier = Modifier.padding(vertical = 12.dp)) }
                }
            }
        }
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { index ->
            when (index) {
                0 -> CommunityCellList()
                else -> CommunityPosterGrid()
            }
        }
    }
}

@Composable
fun CommunityCellList(onClick: ((CommunityInfo) -> Unit)? = null) {
    val cellViewModel = createJoinedCommunitiesViewModel()
    CommunityList(cellViewModel, onClick)
}

@Composable
fun CommunityPosterGrid(onClick: ((CommunityInfo) -> Unit)? = null) {
    val posterViewModel = createJoinedCommunitiesWithPosterViewModel()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val posterWidth = if (maxWidth >= 600.dp) 180.dp else 128.dp
        StateView(posterViewModel, modifier = Modifier.fillMaxSize()) { items ->
            LazyVerticalGrid(
                GridCells.Adaptive(minSize = posterWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = LayoutDefaults.contentPadding,
                horizontalArrangement = LayoutDefaults.pagingHorizontalArrangement,
                verticalArrangement = LayoutDefaults.pagingVerticalArrangement,
            ) {
                topPrepend(items.loadState)
                pagingItems(
                    items,
                    { it.id.toString() },
                ) { index ->
                    val info = items[index]
                    if (info != null) {
                        CommunityGrid(info, 0.dp, onClick)
                    }
                }
                bottomAppending(items.loadState)
            }
        }
    }
}

@Composable
fun CommunityList(communitiesViewModel: PagingViewModel<CommunityInfo>, onClick: ((CommunityInfo) -> Unit)? = null) {
    StateView(communitiesViewModel, modifier = Modifier.fillMaxSize()) { items ->
        AdaptiveItemGrid {
            topPrepend(items.loadState)
            pagingItems(items, { it.id }) { index -> CommunityCell(items[index], false, onClick) }
            bottomAppending(items.loadState)
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun CommunityGrid(communityInfo: CommunityInfo?, padding: Dp, onClick: ((CommunityInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    val hazeState = rememberHazeState()
    Box(modifier = Modifier.fillMaxWidth().padding(end = padding)) {
        val shape = RoundedCornerShape(14.dp)
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4)
                .clip(shape)
                .clickable {
                    communityInfo?.let {
                        onClick?.invoke(it) ?: appNavFactory.newAppNav().gotoCommunity(it.id, false)
                    }
                },
        ) {
            Box(modifier = Modifier.hazeSource(hazeState)) {
                CommunityPoster(communityInfo)
            }
            val shape = RoundedCornerShape(10.dp)
            Row(
                modifier =
                Modifier.align(Alignment.BottomStart).padding(4.dp)
                    .clip(shape)
                    .hazeEffect(hazeState, HazeMaterials.ultraThin()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (communityInfo != null) {
                    val commonDialogController = rememberCommonDialogController()
                    val shown by commonDialogController.shown
                    CommunityIconWithDialog(communityInfo, shown, 30.dp, onClickIcon = commonDialogController::update)
                    Text(
                        communityInfo.name,
                        Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityCell(
    communityInfo: CommunityInfo?,
    customBackground: Boolean = false,
    onClick: ((CommunityInfo) -> Unit)? = null,
) {
    val appNavFactory = LocalAppNavFactory.current
    val isCommunityPage =
        appNavFactory.hasRouteFlow<CommunityScreen> {
            it.communityId == communityInfo?.id
        }
    val openCommunity =
        communityInfo?.let { info ->
            {
                onClick?.invoke(info) ?: appNavFactory.newAppNav().gotoCommunity(
                    info.id,
                    false,
                )
            }
        }
    Row(
        modifier =
        if (customBackground) {
            Modifier.appiumSemantics(
                text = communityInfo?.name,
                onClick = openCommunity,
            )
        } else {
            val shape = RoundedCornerShape(10.dp)
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer, shape)
                .clip(shape)
                .clickable(!isCommunityPage) {
                    openCommunity?.invoke()
                }
                .padding(10.dp)
                .appiumSemantics(
                    text = communityInfo?.name,
                    onClick = openCommunity.takeUnless { isCommunityPage },
                )
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val commonDialogController = rememberCommonDialogController()
        val shown by commonDialogController.shown
        CommunityIconWithDialog(communityInfo, shown, 50.dp, onClickIcon = commonDialogController::update)
        Text(
            communityInfo?.name.orEmpty(),
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
