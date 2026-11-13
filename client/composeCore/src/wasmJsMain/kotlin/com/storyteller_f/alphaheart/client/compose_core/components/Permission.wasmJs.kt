/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
actual fun isPermissionGranted(permission: Permission): MutableState<Boolean> = remember { mutableStateOf(true) }

actual fun requestPermission(permission: Permission) = Unit
