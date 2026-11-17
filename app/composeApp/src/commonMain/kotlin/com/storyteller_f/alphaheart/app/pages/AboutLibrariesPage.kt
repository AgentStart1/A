/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.style.LibraryActionBadges
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.AboutLibrariesHost
import com.storyteller_f.alphaheart.app.common.LibrariesState
import com.storyteller_f.alphaheart.app.library_count
import com.storyteller_f.alphaheart.app.library_sponsor
import com.storyteller_f.alphaheart.app.library_view_license
import com.storyteller_f.alphaheart.app.library_website
import com.storyteller_f.alphaheart.app.open_source_libraries
import com.storyteller_f.alphaheart.app.source_repository
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.ExceptionCell
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

private val libraryCoordination = Dispatchers.Default.limitedParallelism(1)

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun AboutLibrariesPage() {
    val scope = rememberCoroutineScope()
    val host =
        remember(scope) {
            AboutLibrariesHost(scope, libraryCoordination, Dispatchers.Default) {
                Libs.Builder().withJson(Res.readBytes("files/aboutlibraries.json").decodeToString()).build()
            }
        }
    DisposableEffect(host) { onDispose { host.close() } }
    val state by host.state.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize()) {
            ReadingPageToolbar(stringResource(Res.string.open_source_libraries))
            Box(Modifier.weight(1f)) { LibrariesContent(state, host::refresh) }
        }
    }
}

@Composable
internal fun LibrariesContent(state: LibrariesState, refresh: () -> Unit) {
    when (val current = state) {
        LibrariesState.Loading -> CenterBox { CircularProgressIndicator() }

        is LibrariesState.Failed -> CenterBox { ExceptionCell(current.cause, refresh) }

        is LibrariesState.Ready ->
            LibrariesContainer(
                libraries = current.libraries,
                modifier = Modifier.fillMaxSize(),
                actionLabels =
                LibraryActionBadges(
                    source = stringResource(Res.string.source_repository),
                    website = stringResource(Res.string.library_website),
                    sponsor = stringResource(Res.string.library_sponsor),
                    viewLicense = stringResource(Res.string.library_view_license),
                ),
                licenseDialogConfirmText = CoreStrings.ok(),
                header = {
                    item {
                        val count = stringResource(Res.string.library_count, current.libraries.libraries.size)
                        Text(count, Modifier.padding(16.dp).appiumSemantics(text = count))
                    }
                },
            )
    }
}
