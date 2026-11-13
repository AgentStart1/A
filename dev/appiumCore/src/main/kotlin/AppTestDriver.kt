/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

interface AppTestDriver {
    suspend fun clickByDescription(description: String)

    suspend fun clickByText(text: String)
    suspend fun clickByTextContaining(text: String)
    suspend fun inputText(text: String)
    suspend fun assertVisibleByDescription(description: String)
    suspend fun assertNotVisibleByDescription(description: String, timeoutSeconds: Long = 5)
    suspend fun assertVisibleByText(text: String)

    /** Verifies that visible UI text contains [text]. */
    suspend fun assertVisibleByTextContaining(text: String)

    suspend fun assertNotVisibleByText(text: String, timeoutSeconds: Long = 5)

    /** Dismisses an open modal using the platform back/Escape action. */
    suspend fun dismissDialog()

    /** Hides the software keyboard before page navigation; desktop has no software keyboard. */
    suspend fun dismissKeyboard() = Unit

    /** Uses the platform's visible refresh control or pull gesture. */
    suspend fun refreshList() = clickByText("Refresh")

    suspend fun navigateBack()
    suspend fun saveSnapshot(name: String)
}
