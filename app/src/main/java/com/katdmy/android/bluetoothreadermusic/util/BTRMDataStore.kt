package com.katdmy.android.bluetoothreadermusic.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

object BTRMDataStore {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    internal suspend fun <T> saveValue(
        value: T,
        key: Preferences.Key<T>,
        context: Context
    ) {
        context.dataStore.edit { saveData -> saveData[key] = value }
    }

    internal fun <T> getValueFlow(
        key: Preferences.Key<T>,
        context: Context
    ): Flow<T?> =
        context.dataStore.data
            .catch { exception ->
                when (exception) {
                    is IOException -> emit(emptyPreferences())
                    else -> throw exception
                }
            }.map { readData -> readData[key] }
}