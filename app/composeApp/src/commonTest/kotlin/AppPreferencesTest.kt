/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.storyteller_f.alphaheart.app.utils.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppPreferencesTest {
    @Test
    fun missingStringUsesDefault() =
        runTest {
            val preferences = AppPreferences(FakePreferencesDataStore())
            assertEquals("default", preferences.observeString("key", "default").first())
        }

    @Test
    fun savedStringCanBeObserved() =
        runTest {
            val preferences = AppPreferences(FakePreferencesDataStore())
            preferences.setString("key", "saved")
            assertEquals("saved", preferences.observeString("key", "default").first())
        }

    @Test
    fun writingStringPreservesOtherKeyDefault() =
        runTest {
            val preferences = AppPreferences(FakePreferencesDataStore())
            preferences.setString("key", "saved")
            assertEquals("other-default", preferences.observeString("other", "other-default").first())
        }
}

private class FakePreferencesDataStore(initialValue: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initialValue)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(
            state.value,
        ).also { updatedValue ->
            state.value = updatedValue
        }
}
