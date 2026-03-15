package com.example.flagquiz.ui.state

import com.example.flagquiz.domain.model.FlagQuestion
import com.example.flagquiz.domain.model.GameDifficulty
import com.example.flagquiz.domain.model.GameMode
import com.example.flagquiz.domain.model.GameRegion
import com.example.flagquiz.navigation.AppScreen

data class FlagQuizUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val isGameComplete: Boolean = false,
    val selectedRegion: GameRegion = GameRegion.WORLD,
    val selectedGameMode: GameMode = GameMode.FLAG,
    val selectedDifficulty: GameDifficulty = GameDifficulty.NORMAL,
    val score: Int = 0,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<FlagQuestion> = emptyList(),
    val currentQuestion: FlagQuestion? = null,
    val selectedAnswer: String? = null,
    val savedScores: Map<GameRegion, Int?> = emptyMap(),
    val regionCounts: Map<GameRegion, Int> = emptyMap()
)
