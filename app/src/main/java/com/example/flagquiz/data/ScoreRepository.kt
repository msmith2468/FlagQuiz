package com.example.flagquiz.data

import android.content.Context
import com.example.flagquiz.domain.model.GameRegion

class ScoreRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSavedScores(): Map<GameRegion, Int?> {
        return GameRegion.entries.associateWith { region ->
            if (preferences.contains(scoreKey(region))) preferences.getInt(scoreKey(region), 0) else null
        }
    }

    fun saveHighScore(region: GameRegion, percentage: Int) {
        val existingScore = preferences.getInt(scoreKey(region), -1)
        if (percentage > existingScore) {
            preferences.edit().putInt(scoreKey(region), percentage).apply()
        }
    }

    fun resetScores() {
        preferences.edit().clear().apply()
    }

    private fun scoreKey(region: GameRegion): String = "score_${region.name.lowercase()}"

    private companion object {
        const val PREFS_NAME = "flag_quiz_scores"
    }
}
