/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.storyteller_f.alphaheart.app.components.CommunityPoster
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.theme.LayoutDefaults
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.CommunityInfo
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow as PrimaryTabRow

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
            val tabPadding =
                if (maxWidth <= 600.dp) {
                    PaddingValues(start = 14.dp, top = 20.dp, end = 14.dp)
                } else {
                    PaddingValues(start = 30.dp, top = 30.dp, end = 30.dp)
                }
            PrimaryTabRow(Modifier.fillMaxWidth(), contentPadding = tabPadding) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = pagerState.currentPage == i, onClick = {
                        scope.launch { pagerState.scrollToPage(i) }
                    }) { Text(label) }
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
    CommunityRows(createJoinedCommunitiesViewModel(), onClick)
}

@Composable
fun CommunityRows(
    viewModel: PagingViewModel<CommunityInfo>,
    onClick: ((CommunityInfo) -> Unit)? = null,
    insetTop: Boolean = false,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth <= 600.dp
        val horizontalPadding = if (compact) 14.dp else 30.dp
        val sectionTopPadding = if (compact) 20.dp else 30.dp
        StateView(viewModel, Modifier.fillMaxSize()) { items ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding =
                PaddingValues(
                    start = horizontalPadding,
                    top = if (insetTop) sectionTopPadding else 0.dp,
                    end = horizontalPadding,
                ),
            ) {
                topPrepend(items.loadState, items::retry)
                pagingItems(items, { it.id }) { index ->
                    Column {
                        CommunityCell(items[index], onClick = onClick)
                        if (index < items.itemCount - 1) HorizontalDivider()
                    }
                }
                bottomAppending(items.loadState, items::retry)
            }
        }
    }
}

@Composable
fun CommunityPosterGrid(onClick: ((CommunityInfo) -> Unit)? = null) {
    val posterViewModel = createJoinedCommunitiesWithPosterViewModel()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val posterWidth = 160.dp
        StateView(posterViewModel, modifier = Modifier.fillMaxSize()) { items ->
            LazyVerticalGrid(
                if (maxWidth >= 600.dp) GridCells.FixedSize(posterWidth) else GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = LayoutDefaults.contentPadding,
                horizontalArrangement = LayoutDefaults.pagingHorizontalArrangement,
                verticalArrangement = LayoutDefaults.pagingVerticalArrangement,
            ) {
                topPrepend(items.loadState, items::retry)
                pagingItems(
                    items,
                    { it.id.toString() },
                ) { index ->
                    val info = items[index]
                    if (info != null) {
                        CommunityGrid(info, 0.dp, onClick)
                    }
                }
                bottomAppending(items.loadState, items::retry)
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun CommunityGrid(communityInfo: CommunityInfo?, padding: Dp, onClick: ((CommunityInfo) -> Unit)? = null) {
    val appNavFactory = LocalAppNavFactory.current
    val hazeState = rememberHazeState()
    val dialog = rememberCommonDialogController()
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
                    CommunityIconWithDialog(communityInfo, dialog.shown.value, 30.dp, onClickIcon = dialog::update)
                    Column(Modifier.weight(1f)) {
                        Text(
                            communityInfo.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (communityInfo.aid.isNotBlank()) {
                            Text(CoreStrings.aid(communityInfo.aid), style = MaterialTheme.typography.bodySmall)
                        }
                    }
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
                .clip(shape)
                .clickable(openCommunity != null && (!isCommunityPage || onClick != null)) {
                    openCommunity?.invoke()
                }
                .padding(20.dp)
                .appiumSemantics(
                    text = communityInfo?.name,
                    onClick = openCommunity.takeUnless { isCommunityPage && onClick == null },
                )
        },
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val commonDialogController = rememberCommonDialogController()
        val shown by commonDialogController.shown
        CommunityIconWithDialog(communityInfo, shown, 40.dp, onClickIcon = commonDialogController::update)
        Column(Modifier.weight(1f)) {
            Text(
                communityInfo?.name.orEmpty(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
            )
            communityInfo?.aid?.takeIf { it.isNotBlank() }?.let {
                Text(
                    CoreStrings.aid(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
