/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun SettingOptionView(title: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Column {
        val shape = RoundedCornerShape(8.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier.clip(shape).clickable {
                onClick()
            }.padding(18.dp),
        ) {
            Text(title)
            Spacer(modifier = Modifier.weight(1f))
            content()
        }
        HorizontalDivider()
    }
}

@Composable
fun SettingOptionResettableView(
    title: String,
    supportReset: Boolean,
    onClick: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column {
        val shape = RoundedCornerShape(8.dp)
        var expended by remember {
            mutableStateOf(false)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier.clip(shape).clickable {
                if (supportReset) {
                    expended = true
                } else {
                    onClick(false)
                }
            }.padding(18.dp),
        ) {
            Text(title)
            Spacer(modifier = Modifier.weight(1f))
            content()
            DropdownMenu(expended, {
                expended = false
            }) {
                DropdownMenuItem({
                    expended = false
                    onClick(true)
                }) {
                    Text("Reset")
                }
                DropdownMenuItem({
                    onClick(false)
                    expended = false
                }) {
                    Text("Edit")
                }
            }
        }
        HorizontalDivider()
    }
}
