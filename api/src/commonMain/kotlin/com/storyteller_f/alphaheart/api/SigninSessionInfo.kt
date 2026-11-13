/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.api

import com.storyteller_f.shared.model.PanelAccountInfo
import kotlinx.serialization.Serializable

/** Binds a restored panel session to the locally selected account key. */
@Serializable
data class PanelSigninSessionInfo(val account: PanelAccountInfo, val address: String)
