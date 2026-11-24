/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.libraries

import kotlinx.coroutines.Dispatchers

internal val libraryCoordination = Dispatchers.Default.limitedParallelism(1)
