package com.example.flagquiz

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GameRegion(val title: String, val subtitle: String) {
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
    val isGameStarted: Boolean = false,
    val selectedRegion: GameRegion = GameRegion.WORLD,
    val score: Int = 0,
    val round: Int = 0,
    val currentQuestion: FlagQuestion? = null,
    val selectedAnswer: String? = null
)

class FlagQuizViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FlagQuizUiState())
    val uiState: StateFlow<FlagQuizUiState> = _uiState.asStateFlow()

    fun startGame(region: GameRegion) {
        _uiState.value = FlagQuizUiState(
            isGameStarted = true,
            selectedRegion = region,
            score = 0,
            round = 1,
            currentQuestion = generateQuestion(region = region),
            selectedAnswer = null
        )
    }

    fun submitAnswer(answer: String) {
        val currentState = _uiState.value
        val currentQuestion = currentState.currentQuestion ?: return
        if (currentState.selectedAnswer != null) return

        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                score = if (answer == currentQuestion.correctCountry) it.score + 1 else it.score
            )
        }
    }

    fun loadNextQuestion() {
        val currentState = _uiState.value
        val currentQuestion = currentState.currentQuestion ?: return
        if (currentState.selectedAnswer == null) return

        _uiState.update {
            it.copy(
                round = it.round + 1,
                currentQuestion = generateQuestion(
                    region = it.selectedRegion,
                    previousCountry = currentQuestion.correctCountry
                ),
                selectedAnswer = null
            )
        }
    }

    fun returnHome() {
        _uiState.value = FlagQuizUiState()
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
    FlagEntry("Belgium", "\uD83C\uDDE7\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Bolivia", "\uD83C\uDDE7\uD83C\uDDF4", GameRegion.SOUTH_AMERICA),
    FlagEntry("Brazil", "\uD83C\uDDE7\uD83C\uDDF7", GameRegion.SOUTH_AMERICA),
    FlagEntry("Cameroon", "\uD83C\uDDE8\uD83C\uDDF2", GameRegion.AFRICA),
    FlagEntry("Canada", "\uD83C\uDDE8\uD83C\uDDE6", GameRegion.NORTH_AMERICA),
    FlagEntry("Chile", "\uD83C\uDDE8\uD83C\uDDF1", GameRegion.SOUTH_AMERICA),
    FlagEntry("China", "\uD83C\uDDE8\uD83C\uDDF3", GameRegion.ASIA),
    FlagEntry("Colombia", "\uD83C\uDDE8\uD83C\uDDF4", GameRegion.SOUTH_AMERICA),
    FlagEntry("Costa Rica", "\uD83C\uDDE8\uD83C\uDDF7", GameRegion.NORTH_AMERICA),
    FlagEntry("Cuba", "\uD83C\uDDE8\uD83C\uDDFA", GameRegion.NORTH_AMERICA),
    FlagEntry("Denmark", "\uD83C\uDDE9\uD83C\uDDF0", GameRegion.EUROPE),
    FlagEntry("Dominican Republic", "\uD83C\uDDE9\uD83C\uDDF4", GameRegion.NORTH_AMERICA),
    FlagEntry("Egypt", "\uD83C\uDDEA\uD83C\uDDEC", GameRegion.AFRICA),
    FlagEntry("Fiji", "\uD83C\uDDEB\uD83C\uDDEF", GameRegion.OCEANIA),
    FlagEntry("Finland", "\uD83C\uDDEB\uD83C\uDDEE", GameRegion.EUROPE),
    FlagEntry("France", "\uD83C\uDDEB\uD83C\uDDF7", GameRegion.EUROPE),
    FlagEntry("Germany", "\uD83C\uDDE9\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Ghana", "\uD83C\uDDEC\uD83C\uDDED", GameRegion.AFRICA),
    FlagEntry("Greece", "\uD83C\uDDEC\uD83C\uDDF7", GameRegion.EUROPE),
    FlagEntry("India", "\uD83C\uDDEE\uD83C\uDDF3", GameRegion.ASIA),
    FlagEntry("Indonesia", "\uD83C\uDDEE\uD83C\uDDE9", GameRegion.ASIA),
    FlagEntry("Ireland", "\uD83C\uDDEE\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Italy", "\uD83C\uDDEE\uD83C\uDDF9", GameRegion.EUROPE),
    FlagEntry("Jamaica", "\uD83C\uDDEF\uD83C\uDDF2", GameRegion.NORTH_AMERICA),
    FlagEntry("Japan", "\uD83C\uDDEF\uD83C\uDDF5", GameRegion.ASIA),
    FlagEntry("Kenya", "\uD83C\uDDF0\uD83C\uDDEA", GameRegion.AFRICA),
    FlagEntry("Malaysia", "\uD83C\uDDF2\uD83C\uDDFE", GameRegion.ASIA),
    FlagEntry("Mexico", "\uD83C\uDDF2\uD83C\uDDFD", GameRegion.NORTH_AMERICA),
    FlagEntry("Morocco", "\uD83C\uDDF2\uD83C\uDDE6", GameRegion.AFRICA),
    FlagEntry("Netherlands", "\uD83C\uDDF3\uD83C\uDDF1", GameRegion.EUROPE),
    FlagEntry("New Zealand", "\uD83C\uDDF3\uD83C\uDDFF", GameRegion.OCEANIA),
    FlagEntry("Nigeria", "\uD83C\uDDF3\uD83C\uDDEC", GameRegion.AFRICA),
    FlagEntry("Norway", "\uD83C\uDDF3\uD83C\uDDF4", GameRegion.EUROPE),
    FlagEntry("Panama", "\uD83C\uDDF5\uD83C\uDDE6", GameRegion.NORTH_AMERICA),
    FlagEntry("Papua New Guinea", "\uD83C\uDDF5\uD83C\uDDEC", GameRegion.OCEANIA),
    FlagEntry("Peru", "\uD83C\uDDF5\uD83C\uDDEA", GameRegion.SOUTH_AMERICA),
    FlagEntry("Philippines", "\uD83C\uDDF5\uD83C\uDDED", GameRegion.ASIA),
    FlagEntry("Poland", "\uD83C\uDDF5\uD83C\uDDF1", GameRegion.EUROPE),
    FlagEntry("Portugal", "\uD83C\uDDF5\uD83C\uDDF9", GameRegion.EUROPE),
    FlagEntry("Samoa", "\uD83C\uDDFC\uD83C\uDDF8", GameRegion.OCEANIA),
    FlagEntry("Saudi Arabia", "\uD83C\uDDF8\uD83C\uDDE6", GameRegion.ASIA),
    FlagEntry("Senegal", "\uD83C\uDDF8\uD83C\uDDF3", GameRegion.AFRICA),
    FlagEntry("South Africa", "\uD83C\uDDFF\uD83C\uDDE6", GameRegion.AFRICA),
    FlagEntry("South Korea", "\uD83C\uDDF0\uD83C\uDDF7", GameRegion.ASIA),
    FlagEntry("Spain", "\uD83C\uDDEA\uD83C\uDDF8", GameRegion.EUROPE),
    FlagEntry("Sweden", "\uD83C\uDDF8\uD83C\uDDEA", GameRegion.EUROPE),
    FlagEntry("Switzerland", "\uD83C\uDDE8\uD83C\uDDED", GameRegion.EUROPE),
    FlagEntry("Thailand", "\uD83C\uDDF9\uD83C\uDDED", GameRegion.ASIA),
    FlagEntry("Tonga", "\uD83C\uDDF9\uD83C\uDDF4", GameRegion.OCEANIA),
    FlagEntry("Tunisia", "\uD83C\uDDF9\uD83C\uDDF3", GameRegion.AFRICA),
    FlagEntry("United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7", GameRegion.EUROPE),
    FlagEntry("United States", "\uD83C\uDDFA\uD83C\uDDF8", GameRegion.NORTH_AMERICA),
    FlagEntry("Uruguay", "\uD83C\uDDFA\uD83C\uDDFE", GameRegion.SOUTH_AMERICA),
    FlagEntry("Venezuela", "\uD83C\uDDFB\uD83C\uDDEA", GameRegion.SOUTH_AMERICA),
    FlagEntry("Vietnam", "\uD83C\uDDFB\uD83C\uDDF3", GameRegion.ASIA)
)

private fun generateQuestion(region: GameRegion, previousCountry: String? = null): FlagQuestion {
    val regionFlags = if (region == GameRegion.WORLD) flagEntries else flagEntries.filter { it.region == region }
    val availableFlags = regionFlags.filterNot { it.country == previousCountry }
    val correctEntry = availableFlags.random()
    val distractors = regionFlags
        .filterNot { it.country == correctEntry.country }
        .shuffled()
        .take(3)
        .map { it.country }

    return FlagQuestion(
        flagEmoji = correctEntry.flagEmoji,
        correctCountry = correctEntry.country,
        options = (distractors + correctEntry.country).shuffled()
    )
}
