/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.input

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

private val panelInputCoordination = Dispatchers.Default.limitedParallelism(1)

class PanelInputViewModel : ViewModel() {
    internal val host = PanelInputHost(viewModelScope, panelInputCoordination, Dispatchers.Default)

    override fun onCleared() {
        host.close()
    }
}
