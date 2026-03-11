package com.example.flagquiz

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

enum class AppScreen {
    HOME,
    SETTINGS,
    GAME
}

enum class GameRegion(
    val title: String,
    val subtitle: String
) {
    WORLD("World", "A mix of flags from every continent"),
    AFRICA("Africa", "Explore flags across the African continent"),
    ASIA("Asia", "Play with countries across Asia"),
    EUROPE("Europe", "Guess flags from European countries"),
    NORTH_AMERICA("North America", "Play with North American flags"),
    SOUTH_AMERICA("South America", "Guess flags from South America"),
    OCEANIA("Oceania", "Play with island nations and Australia/New Zealand")
}

data class FlagQuestion(
    val flagEmoji: String,
    val correctCountry: String,
    val options: List<String>
)

data class FlagQuizUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val isGameComplete: Boolean = false,
    val selectedRegion: GameRegion = GameRegion.WORLD,
    val score: Int = 0,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val questions: List<FlagQuestion> = emptyList(),
    val currentQuestion: FlagQuestion? = null,
    val selectedAnswer: String? = null,
    val savedScores: Map<GameRegion, Int?> = emptyMap(),
    val regionCounts: Map<GameRegion, Int> = emptyMap()
)

class FlagQuizViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        FlagQuizUiState(
            savedScores = loadSavedScores(),
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
        preferences.edit().clear().apply()
        _uiState.update {
            it.copy(savedScores = loadSavedScores())
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
        val percentage = if (currentState.totalQuestions == 0) {
            0
        } else {
            (currentState.score.toFloat() / currentState.totalQuestions.toFloat() * 100).roundToInt()
        }
        saveScore(currentState.selectedRegion, percentage)
        _uiState.update {
            it.copy(
                isGameComplete = true,
                savedScores = loadSavedScores()
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

    private fun flagsForRegion(region: GameRegion): List<FlagEntry> {
        return if (region == GameRegion.WORLD) flagEntries else flagEntries.filter { it.region == region }
    }

    private fun buildRegionCounts(): Map<GameRegion, Int> {
        return GameRegion.entries.associateWith { region -> flagsForRegion(region).size }
    }

    private fun loadSavedScores(): Map<GameRegion, Int?> {
        return GameRegion.entries.associateWith { region ->
            if (preferences.contains(scoreKey(region))) preferences.getInt(scoreKey(region), 0) else null
        }
    }

    private fun saveScore(region: GameRegion, percentage: Int) {
        preferences.edit().putInt(scoreKey(region), percentage).apply()
    }

    private fun scoreKey(region: GameRegion): String = "score_${region.name.lowercase()}"

    private companion object {
        const val PREFS_NAME = "flag_quiz_scores"
    }
}

private data class FlagEntry(
    val country: String,
    val flagEmoji: String,
    val region: GameRegion
)

private val flagEntries = listOf(
    FlagEntry("Algeria", "\uD83C\uDDE9\uD83C\uDDFF", GameRegion.AFRICA),
    FlagEntry("Argentina", "\uD83C\uDDE6\uD83C\uDDF7", GameRegion.SOUTH_AMERICA),
    FlagEntry("Australia", "\uD83C\uDDE6\uD83C\uDDFA", GameRegion.OCEANIA),
    FlagEntry("Austria", "\uD83C\uDDE6\uD83C\uDDF9", GameRegion.EUROPE),
    FlagEntry("Bahamas", "\uD83C\uDDE7\uD83C\uDDF8", GameRegion.NORTH_AMERICA),
    FlagEntry("Bangladesh", "\uD83C\uDDE7\uD83C\uDDE9", GameRegion.ASIA),
    FlagEntry("Belgium", "\uD83C\uDDE7\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Bolivia", "\uD83C\uDDE7\uD83C\uDDF4", GameRegion.SOUTH_AMERICA),
    FlagEntry("Brazil", "\uD83C\uDDE7\uD83C\uDDF7", GameRegion.SOUTH_AMERICA),
    FlagEntry("Bulgaria", "\uD83C\uDDE7\uD83C\uDDEC", GameRegion.EUROPE),
    FlagEntry("Cameroon", "\uD83C\uDDE8\uD83C\uDDF2", GameRegion.AFRICA),
    FlagEntry("Cambodia", "\uD83C\uDDF0\uD83C\uDDED", GameRegion.ASIA),
    FlagEntry("Canada", "\uD83C\uDDE8\uD83C\uDDE6", GameRegion.NORTH_AMERICA),
    FlagEntry("Chile", "\uD83C\uDDE8\uD83C\uDDF1", GameRegion.SOUTH_AMERICA),
    FlagEntry("China", "\uD83C\uDDE8\uD83C\uDDF3", GameRegion.ASIA),
    FlagEntry("Colombia", "\uD83C\uDDE8\uD83C\uDDF4", GameRegion.SOUTH_AMERICA),
    FlagEntry("Congo - Kinshasa", "\uD83C\uDDE8\uD83C\uDDE9", GameRegion.AFRICA),
    FlagEntry("Costa Rica", "\uD83C\uDDE8\uD83C\uDDF7", GameRegion.NORTH_AMERICA),
    FlagEntry("Croatia", "\uD83C\uDDED\uD83C\uDDF7", GameRegion.EUROPE),
    FlagEntry("Cuba", "\uD83C\uDDE8\uD83C\uDDFA", GameRegion.NORTH_AMERICA),
    FlagEntry("Czechia", "\uD83C\uDDE8\uD83C\uDDFF", GameRegion.EUROPE),
    FlagEntry("Denmark", "\uD83C\uDDE9\uD83C\uDDF0", GameRegion.EUROPE),
    FlagEntry("Dominican Republic", "\uD83C\uDDE9\uD83C\uDDF4", GameRegion.NORTH_AMERICA),
    FlagEntry("Ecuador", "\uD83C\uDDEA\uD83C\uDDE8", GameRegion.SOUTH_AMERICA),
    FlagEntry("Egypt", "\uD83C\uDDEA\uD83C\uDDEC", GameRegion.AFRICA),
    FlagEntry("El Salvador", "\uD83C\uDDF8\uD83C\uDDFB", GameRegion.NORTH_AMERICA),
    FlagEntry("Ethiopia", "\uD83C\uDDEA\uD83C\uDDF9", GameRegion.AFRICA),
    FlagEntry("Fiji", "\uD83C\uDDEB\uD83C\uDDEF", GameRegion.OCEANIA),
    FlagEntry("Finland", "\uD83C\uDDEB\uD83C\uDDEE", GameRegion.EUROPE),
    FlagEntry("France", "\uD83C\uDDEB\uD83C\uDDF7", GameRegion.EUROPE),
    FlagEntry("Germany", "\uD83C\uDDE9\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Ghana", "\uD83C\uDDEC\uD83C\uDDED", GameRegion.AFRICA),
    FlagEntry("Greece", "\uD83C\uDDEC\uD83C\uDDF7", GameRegion.EUROPE),
    FlagEntry("Guatemala", "\uD83C\uDDEC\uD83C\uDDF9", GameRegion.NORTH_AMERICA),
    FlagEntry("Guyana", "\uD83C\uDDEC\uD83C\uDDFE", GameRegion.SOUTH_AMERICA),
    FlagEntry("Honduras", "\uD83C\uDDED\uD83C\uDDF3", GameRegion.NORTH_AMERICA),
    FlagEntry("Hungary", "\uD83C\uDDED\uD83C\uDDFA", GameRegion.EUROPE),
    FlagEntry("Iceland", "\uD83C\uDDEE\uD83C\uDDF8", GameRegion.EUROPE),
    FlagEntry("India", "\uD83C\uDDEE\uD83C\uDDF3", GameRegion.ASIA),
    FlagEntry("Indonesia", "\uD83C\uDDEE\uD83C\uDDE9", GameRegion.ASIA),
    FlagEntry("Ireland", "\uD83C\uDDEE\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Italy", "\uD83C\uDDEE\uD83C\uDDF9", GameRegion.EUROPE),
    FlagEntry("Jamaica", "\uD83C\uDDEF\uD83C\uDDF2", GameRegion.NORTH_AMERICA),
    FlagEntry("Japan", "\uD83C\uDDEF\uD83C\uDDF5", GameRegion.ASIA),
    FlagEntry("Jordan", "\uD83C\uDDEF\uD83C\uDDF4", GameRegion.ASIA),
    FlagEntry("Kenya", "\uD83C\uDDF0\uD83C\uDDEA", GameRegion.AFRICA),
    FlagEntry("Laos", "\uD83C\uDDF1\uD83C\uDDE6", GameRegion.ASIA),
    FlagEntry("Lebanon", "\uD83C\uDDF1\uD83C\uDDE7", GameRegion.ASIA),
    FlagEntry("Luxembourg", "\uD83C\uDDF1\uD83C\uDDFA", GameRegion.EUROPE),
    FlagEntry("Malaysia", "\uD83C\uDDF2\uD83C\uDDFE", GameRegion.ASIA),
    FlagEntry("Mexico", "\uD83C\uDDF2\uD83C\uDDFD", GameRegion.NORTH_AMERICA),
    FlagEntry("Mongolia", "\uD83C\uDDF2\uD83C\uDDF3", GameRegion.ASIA),
    FlagEntry("Morocco", "\uD83C\uDDF2\uD83C\uDDE6", GameRegion.AFRICA),
    FlagEntry("Nepal", "\uD83C\uDDF3\uD83C\uDDF5", GameRegion.ASIA),
    FlagEntry("Netherlands", "\uD83C\uDDF3\uD83C\uDDF1", GameRegion.EUROPE),
    FlagEntry("New Zealand", "\uD83C\uDDF3\uD83C\uDDFF", GameRegion.OCEANIA),
    FlagEntry("Nigeria", "\uD83C\uDDF3\uD83C\uDDEC", GameRegion.AFRICA),
    FlagEntry("Norway", "\uD83C\uDDF3\uD83C\uDDF4", GameRegion.EUROPE),
    FlagEntry("Pakistan", "\uD83C\uDDF5\uD83C\uDDF0", GameRegion.ASIA),
    FlagEntry("Palau", "\uD83C\uDDF5\uD83C\uDDFC", GameRegion.OCEANIA),
    FlagEntry("Panama", "\uD83C\uDDF5\uD83C\uDDE6", GameRegion.NORTH_AMERICA),
    FlagEntry("Papua New Guinea", "\uD83C\uDDF5\uD83C\uDDEC", GameRegion.OCEANIA),
    FlagEntry("Paraguay", "\uD83C\uDDF5\uD83C\uDDFE", GameRegion.SOUTH_AMERICA),
    FlagEntry("Peru", "\uD83C\uDDF5\uD83C\uDDEA", GameRegion.SOUTH_AMERICA),
    FlagEntry("Philippines", "\uD83C\uDDF5\uD83C\uDDED", GameRegion.ASIA),
    FlagEntry("Poland", "\uD83C\uDDF5\uD83C\uDDF1", GameRegion.EUROPE),
    FlagEntry("Portugal", "\uD83C\uDDF5\uD83C\uDDF9", GameRegion.EUROPE),
    FlagEntry("Qatar", "\uD83C\uDDF6\uD83C\uDDE6", GameRegion.ASIA),
    FlagEntry("Romania", "\uD83C\uDDF7\uD83C\uDDF4", GameRegion.EUROPE),
    FlagEntry("Samoa", "\uD83C\uDDFC\uD83C\uDDF8", GameRegion.OCEANIA),
    FlagEntry("Saudi Arabia", "\uD83C\uDDF8\uD83C\uDDE6", GameRegion.ASIA),
    FlagEntry("Senegal", "\uD83C\uDDF8\uD83C\uDDF3", GameRegion.AFRICA),
    FlagEntry("Serbia", "\uD83C\uDDF7\uD83C\uDDF8", GameRegion.EUROPE),
    FlagEntry("Singapore", "\uD83C\uDDF8\uD83C\uDDEC", GameRegion.ASIA),
    FlagEntry("Solomon Islands", "\uD83C\uDDF8\uD83C\uDDE7", GameRegion.OCEANIA),
    FlagEntry("South Africa", "\uD83C\uDDFF\uD83C\uDDE6", GameRegion.AFRICA),
    FlagEntry("South Korea", "\uD83C\uDDF0\uD83C\uDDF7", GameRegion.ASIA),
    FlagEntry("Spain", "\uD83C\uDDEA\uD83C\uDDF8", GameRegion.EUROPE),
    FlagEntry("Sri Lanka", "\uD83C\uDDF1\uD83C\uDDF0", GameRegion.ASIA),
    FlagEntry("Suriname", "\uD83C\uDDF8\uD83C\uDDF7", GameRegion.SOUTH_AMERICA),
    FlagEntry("Sweden", "\uD83C\uDDF8\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Switzerland", "\uD83C\uDDE8\uD83C\uDDED", GameRegion.EUROPE),
    FlagEntry("Tanzania", "\uD83C\uDDF9\uD83C\uDDFF", GameRegion.AFRICA),
    FlagEntry("Thailand", "\uD83C\uDDF9\uD83C\uDDED", GameRegion.ASIA),
    FlagEntry("Tonga", "\uD83C\uDDF9\uD83C\uDDF4", GameRegion.OCEANIA),
    FlagEntry("Trinidad and Tobago", "\uD83C\uDDF9\uD83C\uDDF9", GameRegion.NORTH_AMERICA),
    FlagEntry("Tunisia", "\uD83C\uDDF9\uD83C\uDDF3", GameRegion.AFRICA),
    FlagEntry("Turkey", "\uD83C\uDDF9\uD83C\uDDF7", GameRegion.EUROPE),
    FlagEntry("Uganda", "\uD83C\uDDFA\uD83C\uDDEC", GameRegion.AFRICA),
    FlagEntry("Ukraine", "\uD83C\uDDFA\uD83C\uDDE6", GameRegion.EUROPE),
    FlagEntry("United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7", GameRegion.EUROPE),
    FlagEntry("United States", "\uD83C\uDDFA\uD83C\uDDF8", GameRegion.NORTH_AMERICA),
    FlagEntry("Uruguay", "\uD83C\uDDFA\uD83C\uDDFE", GameRegion.SOUTH_AMERICA),
    FlagEntry("Vanuatu", "\uD83C\uDDFB\uD83C\uDDFA", GameRegion.OCEANIA),
    FlagEntry("Venezuela", "\uD83C\uDDFB\uD83C\uDDEA", GameRegion.SOUTH_AMERICA),
    FlagEntry("Vietnam", "\uD83C\uDDFB\uD83C\uDDF3", GameRegion.ASIA),
    FlagEntry("Zambia", "\uD83C\uDDFF\uD83C\uDDF2", GameRegion.AFRICA),
    FlagEntry("Zimbabwe", "\uD83C\uDDFF\uD83C\uDDFC", GameRegion.AFRICA)
)
