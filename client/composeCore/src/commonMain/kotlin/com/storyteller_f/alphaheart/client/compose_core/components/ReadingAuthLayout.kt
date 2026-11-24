/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReadingAuthLayout(title: String, navigation: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) {
        val sidePadding =
            when {
                maxWidth <= 600.dp -> 16.dp
                maxWidth <= 820.dp -> 22.dp
                else -> 30.dp
            }
        Column(
            Modifier.padding(horizontal = sidePadding).widthIn(max = 400.dp).fillMaxWidth()
                .align(Alignment.Center).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.align(Alignment.CenterStart)) { navigation?.invoke() }
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("α", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Column {
                        Text("Alpha Heart", style = MaterialTheme.typography.titleMedium)
                        Text(title, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            content()
        }
    }
}
