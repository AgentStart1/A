/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.community.CommunityComposeInternal
import com.storyteller_f.shared.model.MemberPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CommunityPolicyUiTest {
    @Test
    fun policySelectionMatchesTheValuePassedToTheFormAtBothWidths() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            val policy = mutableStateOf(MemberPolicy.OPEN)
            setContent {
                MaterialTheme {
                    Box(
                        Modifier.wrapContentSize(
                            Alignment.TopStart,
                            unbounded = true,
                        ).requiredSize(width.value, 800.dp),
                    ) {
                        CommunityComposeInternal(
                            memberJoinPolicy = policy.value,
                            onMemberJoinPolicyChange = { policy.value = it },
                        )
                    }
                }
            }
            for (size in listOf(390.dp, 1280.dp)) {
                runOnIdle {
                    width.value = size
                    policy.value = MemberPolicy.OPEN
                }
                onNodeWithText("Open").assertIsDisplayed().performClick()
                onNodeWithText("Invite Only").performClick()
                runOnIdle { assertEquals(MemberPolicy.INVITE_ONLY, policy.value) }
                onNodeWithText("Invite Only").assertIsDisplayed().performClick()
                onNodeWithText("Open").performClick()
                runOnIdle { assertEquals(MemberPolicy.OPEN, policy.value) }
                onNodeWithText("Open").assertIsDisplayed()
            }
        }
}
