package com.example.flagquiz.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.flagquiz.data.FlagDataSource
import com.example.flagquiz.data.ScoreRepository
import com.example.flagquiz.domain.model.FlagQuestion
import com.example.flagquiz.domain.model.GameDifficulty
import com.example.flagquiz.domain.model.GameMode
import com.example.flagquiz.domain.model.GameRegion
import com.example.flagquiz.navigation.AppScreen
import com.example.flagquiz.ui.screen.CountryOutlineCache
import com.example.flagquiz.ui.state.FlagQuizUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.ceil
import kotlin.math.roundToInt

class FlagQuizViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val scoreRepository = ScoreRepository(appContext)

    private val _uiState = MutableStateFlow(
        FlagQuizUiState(
            savedScores = scoreRepository.loadSavedScores(GameMode.FLAG, GameDifficulty.NORMAL),
            regionCounts = buildRegionCounts(GameMode.FLAG, GameDifficulty.NORMAL)
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
                selectedAnswer = null,
                savedScores = scoreRepository.loadSavedScores(it.selectedGameMode, it.selectedDifficulty),
                regionCounts = buildRegionCounts(it.selectedGameMode, it.selectedDifficulty)
            )
        }
    }

    fun selectGameMode(gameMode: GameMode) {
        _uiState.update {
            it.copy(
                selectedGameMode = gameMode,
                savedScores = scoreRepository.loadSavedScores(gameMode, it.selectedDifficulty),
                regionCounts = buildRegionCounts(gameMode, it.selectedDifficulty)
            )
        }
    }

    fun selectDifficulty(difficulty: GameDifficulty) {
        _uiState.update {
            it.copy(
                selectedDifficulty = difficulty,
                savedScores = scoreRepository.loadSavedScores(it.selectedGameMode, difficulty),
                regionCounts = buildRegionCounts(it.selectedGameMode, difficulty)
            )
        }
    }

    fun resetScores() {
        scoreRepository.resetScores()
        _uiState.update {
            it.copy(savedScores = scoreRepository.loadSavedScores(it.selectedGameMode, it.selectedDifficulty))
        }
    }

    fun startGame(region: GameRegion) {
        val currentMode = _uiState.value.selectedGameMode
        val currentDifficulty = _uiState.value.selectedDifficulty
        val questions = buildQuestionSet(region, currentMode, currentDifficulty)
        _uiState.value = _uiState.value.copy(
            currentScreen = AppScreen.GAME,
            isGameComplete = false,
            selectedRegion = region,
            selectedGameMode = currentMode,
            selectedDifficulty = currentDifficulty,
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
        scoreRepository.saveHighScore(
            region = currentState.selectedRegion,
            gameMode = currentState.selectedGameMode,
            difficulty = currentState.selectedDifficulty,
            percentage = percentage
        )
        _uiState.update {
            it.copy(
                isGameComplete = true,
                savedScores = scoreRepository.loadSavedScores(it.selectedGameMode, it.selectedDifficulty)
            )
        }
    }

    private fun buildQuestionSet(
        region: GameRegion,
        gameMode: GameMode,
        difficulty: GameDifficulty
    ): List<FlagQuestion> {
        val regionFlags = flagsForRegion(region, gameMode, difficulty)

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

    private fun flagsForRegion(
        region: GameRegion,
        gameMode: GameMode,
        difficulty: GameDifficulty
    ) = applyDifficultyFilter(
        entries = when (gameMode) {
            GameMode.FLAG -> allFlagsForRegion(region)
            GameMode.OUTLINE -> allFlagsForRegion(region)
                .filter { entry -> CountryOutlineCache.hasGeometry(appContext, entry.country) }
        },
        difficulty = difficulty
    )

    private fun applyDifficultyFilter(
        entries: List<com.example.flagquiz.domain.model.FlagEntry>,
        difficulty: GameDifficulty
    ): List<com.example.flagquiz.domain.model.FlagEntry> {
        if (difficulty == GameDifficulty.EXPERT || entries.size <= 4) return entries

        val rankedEntries = entries.sortedByDescending { entry ->
            CountryOutlineCache.areaScore(appContext, entry.country)
        }
        val desiredSize = ceil(rankedEntries.size * difficulty.poolFraction).toInt()
            .coerceAtLeast(4)
            .coerceAtMost(rankedEntries.size)

        return rankedEntries.take(desiredSize)
    }

    private fun allFlagsForRegion(region: GameRegion) =
        if (region == GameRegion.WORLD) FlagDataSource.flagEntries
        else FlagDataSource.flagEntries.filter { it.region == region }

    private fun buildRegionCounts(gameMode: GameMode, difficulty: GameDifficulty): Map<GameRegion, Int> =
        GameRegion.entries.associateWith { region -> flagsForRegion(region, gameMode, difficulty).size }

    private fun calculateScorePercentage(score: Int, totalQuestions: Int): Int {
        if (totalQuestions == 0) return 0
        return (score.toFloat() / totalQuestions.toFloat() * 100).roundToInt()
    }
}
