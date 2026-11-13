/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.SimpleMessageWithButton

/**
 * 通用信息表格组件，用于以表格形式展示对象的所有属性.
 * @param items 键值对列表，每个 Pair 代表一行：key 为字段名，value 为字段值
 * @param modifier 应用于表格容器的修饰符
 */
@Composable
fun InfoTable(items: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        items.forEachIndexed { index, (key, value) ->
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (index < items.size - 1) {
                            Modifier.border(
                                width = 0.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RectangleShape,
                            ).border(
                                width = 0.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RectangleShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(0.4f).padding(8.dp),
                    text = key,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                VerticalDivider()
                Box(
                    modifier = Modifier.weight(0.6f).padding(8.dp),
                ) {
                    SimpleMessageWithButton(value, key)
                }
            }
        }
    }
}
