/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.shared.model.FileInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class FilePreviewNavigationTest {
    private val file = FileInfo.EMPTY.copy(id = 42)

    @Test
    fun guestPreviewRequestsConfirmationWithoutNavigation() {
        val fixture = PreviewNavigationFixture(signedIn = false)

        fixture.nav.gotoMedia(file)

        assertEquals(1, fixture.promptCount)
        assertEquals(listOf(HomeScreen, TopicScreen(1)), fixture.stack.toList())
    }

    @Test
    fun signedInPreviewOpensFileDetails() {
        val fixture = PreviewNavigationFixture(signedIn = true)

        fixture.nav.gotoMedia(file)

        assertEquals(listOf(HomeScreen, TopicScreen(1), FileInfoScreen(file.id)), fixture.stack.toList())
        assertEquals(0, fixture.promptCount)
    }

    @Test
    fun previewChecksTheLatestSigninState() {
        val fixture = PreviewNavigationFixture(signedIn = false)
        fixture.updateSignin(true)

        fixture.nav.gotoMedia(file)

        assertEquals(listOf(HomeScreen, TopicScreen(1), FileInfoScreen(file.id)), fixture.stack.toList())
        assertEquals(0, fixture.promptCount)
    }

    @Test
    fun signedOutPreviewRequiresConfirmationAgain() {
        val fixture = PreviewNavigationFixture(signedIn = true)
        fixture.updateSignin(false)

        fixture.nav.gotoMedia(file)

        assertEquals(1, fixture.promptCount)
        assertEquals(listOf(HomeScreen, TopicScreen(1)), fixture.stack.toList())
    }

    @Test
    fun directSigninSkipsConfirmation() {
        val fixture = PreviewNavigationFixture(signedIn = false)

        fixture.nav.gotoSignIn()

        assertEquals(listOf(HomeScreen, TopicScreen(1), SignSessionScreen), fixture.stack.toList())
        assertEquals(0, fixture.promptCount)
    }

    @Test
    fun sharedSigninRequestDoesNotChangeTheRoute() {
        val fixture = PreviewNavigationFixture(signedIn = false)

        fixture.nav.requestSignIn()

        assertEquals(1, fixture.promptCount)
        assertEquals(listOf(HomeScreen, TopicScreen(1)), fixture.stack.toList())
    }

    @Test
    fun backFromSigninReturnsToTheOriginalTopic() {
        val fixture = PreviewNavigationFixture(signedIn = false)
        fixture.nav.gotoSignIn()

        fixture.nav.back()

        assertEquals(listOf(HomeScreen, TopicScreen(1)), fixture.stack.toList())
    }
}

private class PreviewNavigationFixture(signedIn: Boolean) {
    private var signedIn = signedIn
    val stack = NavBackStack<NavKey>(HomeScreen, TopicScreen(1))
    var promptCount = 0
        private set
    val nav = newAppNav(stack, isSignedIn = { this.signedIn }, onSignInRequired = { promptCount++ })

    fun updateSignin(value: Boolean) {
        signedIn = value
    }
}
