package com.canyoucount.timeit.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "player_prefs")

object PlayerNameStore {
    private val KEY = stringPreferencesKey("online_player_name")

    fun getName(context: Context): Flow<String> =
        context.dataStore.data.map { it[KEY] ?: "" }

    suspend fun saveName(context: Context, name: String) {
        context.dataStore.edit { it[KEY] = name }
    }
}
