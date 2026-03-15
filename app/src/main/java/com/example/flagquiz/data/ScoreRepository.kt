package com.example.flagquiz.data

import android.content.Context
import com.example.flagquiz.domain.model.GameDifficulty
import com.example.flagquiz.domain.model.GameMode
import com.example.flagquiz.domain.model.GameRegion

class ScoreRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSavedScores(gameMode: GameMode, difficulty: GameDifficulty): Map<GameRegion, Int?> {
        return GameRegion.entries.associateWith { region ->
            val key = scoreKey(region, gameMode, difficulty)
            if (preferences.contains(key)) preferences.getInt(key, 0) else null
        }
    }

    fun saveHighScore(region: GameRegion, gameMode: GameMode, difficulty: GameDifficulty, percentage: Int) {
        val key = scoreKey(region, gameMode, difficulty)
        val existingScore = preferences.getInt(key, -1)
        if (percentage > existingScore) {
            preferences.edit().putInt(key, percentage).apply()
        }
    }

    fun resetScores() {
        preferences.edit().clear().apply()
    }

    private fun scoreKey(region: GameRegion, gameMode: GameMode, difficulty: GameDifficulty): String =
        "score_${gameMode.name.lowercase()}_${difficulty.name.lowercase()}_${region.name.lowercase()}"

    private companion object {
        const val PREFS_NAME = "flag_quiz_scores"
    }
}
