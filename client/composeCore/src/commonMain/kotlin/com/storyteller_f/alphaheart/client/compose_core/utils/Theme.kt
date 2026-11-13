/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun getAppDynamicColorScheme(dynamicColor: Boolean, darkTheme: Boolean): ColorScheme?
