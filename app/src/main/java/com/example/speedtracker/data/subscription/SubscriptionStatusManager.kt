package com.example.speedtracker.data.subscription

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "subscription_prefs")

class SubscriptionStatusManager(private val context: Context) {
    private val IS_SUBSCRIBED_KEY = booleanPreferencesKey("is_subscribed")

    val isSubscribed: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_SUBSCRIBED_KEY] ?: false
    }

    suspend fun setSubscribed(subscribed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_SUBSCRIBED_KEY] = subscribed
        }
    }
}
