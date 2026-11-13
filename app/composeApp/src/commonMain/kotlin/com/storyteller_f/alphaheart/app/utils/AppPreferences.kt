/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class AppPreferences(private val dataStore: DataStore<Preferences>) {
    fun observeString(key: String, defaultValue: String): Flow<String> {
        val preferenceKey = stringPreferencesKey(key)
        return dataStore.data
            .map { preferences -> preferences[preferenceKey] ?: defaultValue }
            .distinctUntilChanged()
    }

    suspend fun setString(key: String, value: String) {
        val preferenceKey = stringPreferencesKey(key)
        dataStore.edit { preferences ->
            preferences[preferenceKey] = value
        }
    }
}

internal val LocalAppPreferences =
    staticCompositionLocalOf<AppPreferences> {
        error("AppPreferences is not provided")
    }

@Composable
internal fun ProvideAppPreferences(dataStore: DataStore<Preferences>, content: @Composable () -> Unit) {
    val preferences =
        remember(dataStore) {
            AppPreferences(dataStore)
        }
    CompositionLocalProvider(LocalAppPreferences provides preferences, content = content)
}

@Composable
internal fun rememberStringPreference(key: String, defaultValue: String): State<String> {
    val preferences = LocalAppPreferences.current
    val values =
        remember(preferences, key, defaultValue) {
            preferences.observeString(key, defaultValue)
        }
    return values.collectAsState(initial = defaultValue)
}
