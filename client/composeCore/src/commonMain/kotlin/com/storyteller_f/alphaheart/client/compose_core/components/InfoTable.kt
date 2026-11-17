/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 通用信息表格组件，用于以表格形式展示对象的所有属性.
 * @param items 键值对列表，每个 Pair 代表一行：key 为字段名，value 为字段值
 * @param modifier 应用于表格容器的修饰符
 */
@Composable
fun InfoTable(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    val windowWidth =
        with(androidx.compose.ui.platform.LocalDensity.current) {
            androidx.compose.ui.platform.LocalWindowInfo.current.containerSize.width.toDp()
        }
    val columns = if (windowWidth >= 1400.dp) 2 else 1
    val state = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(30.dp),
    ) {
        itemsIndexed(items, key = { index, _ -> index }) { index, (key, value) ->
            InfoField(key, value, Modifier.fillMaxWidth(), index < (items.size - 1) / columns * columns)
        }
    }
}

@Composable
private fun InfoField(key: String, value: String, modifier: Modifier, showDivider: Boolean) {
    Column(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SimpleMessageWithButton(value, key)
        }
        if (showDivider) androidx.compose.material3.HorizontalDivider()
    }
}
