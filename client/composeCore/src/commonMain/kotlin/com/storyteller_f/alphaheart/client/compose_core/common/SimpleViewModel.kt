/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.common

import androidx.lifecycle.ViewModel
import com.storyteller_f.alphaheart.client.core.LoadingHandler

abstract class SimpleViewModel<T : Any> : ViewModel() {
    abstract val handler: LoadingHandler<T>
}
