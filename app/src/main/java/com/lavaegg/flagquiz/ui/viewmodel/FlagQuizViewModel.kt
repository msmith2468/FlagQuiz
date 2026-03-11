package com.lavaegg.flagquiz.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lavaegg.flagquiz.data.FlagDataSource
import com.lavaegg.flagquiz.data.ScoreRepository
import com.lavaegg.flagquiz.domain.model.FlagQuestion
import com.lavaegg.flagquiz.domain.model.GameRegion
import com.lavaegg.flagquiz.navigation.AppScreen
import com.lavaegg.flagquiz.ui.state.FlagQuizUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

class FlagQuizViewModel(application: Application) : AndroidViewModel(application) {
    private val scoreRepository = ScoreRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        FlagQuizUiState(
            savedScores = scoreRepository.loadSavedScores(),
            regionCounts = buildRegionCounts()
        )
    )
    val uiState: StateFlow<FlagQuizUiState> = _uiState.asStateFlow()

    fun openSettings() {
        _uiState.update { it.copy(currentScreen = AppScreen.SETTINGS) }
    }

    fun returnHome() {
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.HOME,
                isGameComplete = false,
                score = 0,
                currentQuestionIndex = 0,
                totalQuestions = 0,
                questions = emptyList(),
                currentQuestion = null,
                selectedAnswer = null
            )
        }
    }

    fun resetScores() {
        scoreRepository.resetScores()
        _uiState.update {
            it.copy(savedScores = scoreRepository.loadSavedScores())
        }
    }

    fun startGame(region: GameRegion) {
        val questions = buildQuestionSet(region)
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            isGameComplete = false,
            selectedRegion = region,
            score = 0,
            currentQuestionIndex = 0,
            totalQuestions = questions.size,
            questions = questions,
            currentQuestion = questions.firstOrNull(),
            selectedAnswer = null
        )
    }

    fun submitAnswer(answer: String) {
        val currentState = _uiState.value
        val currentQuestion = currentState.currentQuestion ?: return
        if (currentState.selectedAnswer != null || currentState.isGameComplete) return

        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                score = if (answer == currentQuestion.correctCountry) it.score + 1 else it.score
            )
        }
    }

    fun loadNextQuestion() {
        val currentState = _uiState.value
        if (currentState.selectedAnswer == null || currentState.isGameComplete) return

        val nextIndex = currentState.currentQuestionIndex + 1
        if (nextIndex >= currentState.questions.size) {
            completeGame(currentState)
            return
        }

        _uiState.update {
            it.copy(
                currentQuestionIndex = nextIndex,
                currentQuestion = it.questions[nextIndex],
                selectedAnswer = null
            )
        }
    }

    fun restartCurrentRegion() {
        startGame(_uiState.value.selectedRegion)
    }

    private fun completeGame(currentState: FlagQuizUiState) {
        val percentage = calculateScorePercentage(currentState.score, currentState.totalQuestions)
        scoreRepository.saveHighScore(currentState.selectedRegion, percentage)
        _uiState.update {
            it.copy(
                isGameComplete = true,
                savedScores = scoreRepository.loadSavedScores()
            )
        }
    }

    private fun buildQuestionSet(region: GameRegion): List<FlagQuestion> {
        val regionFlags = flagsForRegion(region)

        return regionFlags.shuffled().map { correctEntry ->
            val distractors = regionFlags
                .filterNot { it.country == correctEntry.country }
                .shuffled()
                .take(3)
                .map { it.country }

            FlagQuestion(
                flagEmoji = correctEntry.flagEmoji,
                correctCountry = correctEntry.country,
                options = (distractors + correctEntry.country).shuffled()
            )
        }
    }

    private fun flagsForRegion(region: GameRegion) =
        if (region == GameRegion.WORLD) FlagDataSource.flagEntries
        else FlagDataSource.flagEntries.filter { it.region == region }

    private fun buildRegionCounts(): Map<GameRegion, Int> =
        GameRegion.entries.associateWith { region -> flagsForRegion(region).size }

    private fun calculateScorePercentage(score: Int, totalQuestions: Int): Int {
        if (totalQuestions == 0) return 0
        return (score.toFloat() / totalQuestions.toFloat() * 100).roundToInt()
    }
}

