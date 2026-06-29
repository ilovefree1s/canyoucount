package com.canyoucount.timeit.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.soloDataStore by preferencesDataStore(name = "solo_leaderboard")

data class SoloScoreEntry(
    val mode: String,        // "standard" or "timebank"
    val avgError: Double,    // standard: avg abs delta; timebank: avg abs delta before dying
    val rounds: Int,         // standard: round count setting; timebank: rounds survived
    val startBank: Double,   // timebank only, 0.0 for standard
    val timestamp: Long = System.currentTimeMillis()
)

private fun SoloScoreEntry.serialize(): String =
    "$mode|$avgError|$rounds|$startBank|$timestamp"

private fun String.deserializeEntry(): SoloScoreEntry? = runCatching {
    val p = split("|")
    SoloScoreEntry(p[0], p[1].toDouble(), p[2].toInt(), p[3].toDouble(), p[4].toLong())
}.getOrNull()

object SoloLeaderboardStore {
    private val STANDARD_KEY = stringPreferencesKey("standard_scores")
    private val TIMEBANK_KEY = stringPreferencesKey("timebank_scores")
    private const val MAX_ENTRIES = 10

    fun standardScores(context: Context): Flow<List<SoloScoreEntry>> =
        context.soloDataStore.data.map { prefs ->
            prefs[STANDARD_KEY]?.split("\n")
                ?.mapNotNull { it.deserializeEntry() }
                ?.sortedBy { it.avgError }
                ?: emptyList()
        }

    fun timeBankScores(context: Context): Flow<List<SoloScoreEntry>> =
        context.soloDataStore.data.map { prefs ->
            prefs[TIMEBANK_KEY]?.split("\n")
                ?.mapNotNull { it.deserializeEntry() }
                ?.sortedByDescending { it.rounds }
                ?: emptyList()
        }

    suspend fun saveStandardScore(context: Context, entry: SoloScoreEntry) {
        context.soloDataStore.edit { prefs ->
            val existing = prefs[STANDARD_KEY]?.split("\n")
                ?.mapNotNull { it.deserializeEntry() } ?: emptyList()
            val updated = (existing + entry)
                .sortedBy { it.avgError }
                .take(MAX_ENTRIES)
            prefs[STANDARD_KEY] = updated.joinToString("\n") { it.serialize() }
        }
    }

    suspend fun saveTimeBankScore(context: Context, entry: SoloScoreEntry) {
        context.soloDataStore.edit { prefs ->
            val existing = prefs[TIMEBANK_KEY]?.split("\n")
                ?.mapNotNull { it.deserializeEntry() } ?: emptyList()
            val updated = (existing + entry)
                .sortedByDescending { it.rounds }
                .take(MAX_ENTRIES)
            prefs[TIMEBANK_KEY] = updated.joinToString("\n") { it.serialize() }
        }
    }
}
