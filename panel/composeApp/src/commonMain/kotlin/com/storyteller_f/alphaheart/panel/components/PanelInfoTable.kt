/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.presentation.panelFieldLabels
import com.storyteller_f.alphaheart.panel.presentation.panelValueFields
import com.storyteller_f.alphaheart.panel.presentation.panelValueLabels
import com.storyteller_f.alphaheart.panel.review_not_set
import org.jetbrains.compose.resources.stringResource

/** Localizes presentation labels while preserving stored identifiers and full-value inspection. */
@Composable
internal fun PanelInfoTable(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    InfoTable(
        items.map { (key, value) ->
            val label = panelFieldLabels[key]?.let { stringResource(it) } ?: key
            val display = if (key in panelValueFields) panelValueText(value) else value
            label to display
        },
        modifier,
    )
}

@Composable
internal fun panelValueText(value: String): String = panelValueLabels[value]?.let { stringResource(it) } ?: value

/** Only an absent value is replaced; literal identifiers and diagnostic text remain intact. */
@Composable
internal fun panelOptionalValueText(value: String?): String = value ?: stringResource(Res.string.review_not_set)
