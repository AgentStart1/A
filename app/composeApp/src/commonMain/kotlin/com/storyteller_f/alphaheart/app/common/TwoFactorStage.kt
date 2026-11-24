/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.disableTwoFactor
import com.storyteller_f.alphaheart.client.core.setupTotp
import com.storyteller_f.shared.model.TotpSetupInfo

internal sealed interface TwoFactorStage {
    data object Loading : TwoFactorStage
    data object LoadFailed : TwoFactorStage
    data object Disabled : TwoFactorStage
    data object Enabled : TwoFactorStage
    data class Scan(val setup: TotpSetupInfo) : TwoFactorStage {
        override fun toString() = "Scan"
    }

    data class Verify(val setup: TotpSetupInfo) : TwoFactorStage {
        override fun toString() = "Verify"
    }

    data class Recovery(val codes: List<String>?) : TwoFactorStage {
        override fun toString() = "Recovery"
    }
}

internal suspend fun AppGlobalDialogController.beginTwoFactorSetup(): TwoFactorStage? =
    useResult { request { setupTotp() } }.getOrNull()?.let { TwoFactorStage.Scan(it) }

internal suspend fun AppGlobalDialogController.turnOffTwoFactor(): TwoFactorStage? =
    useResult { request { disableTwoFactor() } }.getOrNull()?.let { TwoFactorStage.Disabled }
