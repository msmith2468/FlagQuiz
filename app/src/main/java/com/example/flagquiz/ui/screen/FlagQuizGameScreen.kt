package com.example.flagquiz.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flagquiz.data.FlagDataSource
import com.example.flagquiz.domain.model.GameMode
import com.example.flagquiz.ui.state.FlagQuizUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val CorrectAnswerGreen = Color(0xFF2E7D32)
private const val OutlineAutoZoomStart = 0.78f
private const val OutlineAutoZoomEnd = 1.8f
private const val OutlineMinZoom = 0.65f
private const val OutlineMaxZoom = 2.8f

@Composable
fun FlagQuizGameScreen(
    uiState: FlagQuizUiState,
    onAnswerSelected: (String) -> Unit,
    onNextFlag: () -> Unit,
    onExitGame: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val currentQuestion = uiState.currentQuestion ?: return
    val flagLookup = remember { FlagDataSource.flagEntries.associate { it.country to it.flagEmoji } }
    val outlineZoom = remember(currentQuestion.correctCountry, uiState.selectedGameMode) {
        Animatable(OutlineAutoZoomStart)
    }
    val coroutineScope = rememberCoroutineScope()
    var isManualZoom by remember(currentQuestion.correctCountry, uiState.selectedGameMode) {
        mutableStateOf(false)
    }

    LaunchedEffect(currentQuestion.correctCountry, uiState.selectedGameMode) {
        isManualZoom = false
        outlineZoom.updateBounds(lowerBound = OutlineMinZoom, upperBound = OutlineMaxZoom)
        outlineZoom.snapTo(OutlineAutoZoomStart)
        outlineZoom.animateTo(
            targetValue = OutlineAutoZoomEnd,
            animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing)
        )
    }

    val scorePercentage = if (uiState.totalQuestions == 0) {
        0
    } else {
        (uiState.score.toFloat() / uiState.totalQuestions.toFloat() * 100).roundToInt()
    }
    val helperText = when (uiState.selectedGameMode) {
        GameMode.FLAG -> "Pick the country that matches the flag."
        GameMode.OUTLINE -> "Pick the country that matches the highlighted outline."
    }
    val resultText = when (uiState.selectedAnswer) {
        null -> helperText
        currentQuestion.correctCountry -> "Correct!"
        else -> "Not quite. It was ${currentQuestion.correctCountry}."
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Flag Quiz",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${uiState.selectedRegion.title} · ${uiState.selectedGameMode.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isGameComplete) "Finished" else "Q ${uiState.currentQuestionIndex + 1}/${uiState.totalQuestions}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Score ${uiState.score}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isGameComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Round Complete",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You got ${uiState.score} out of ${uiState.totalQuestions} correct.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Final score: $scorePercentage%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    OutlinedButton(onClick = onExitGame) {
                        Text(text = "Main Menu")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(onClick = onPlayAgain) {
                        Text(text = "Play Again")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.selectedGameMode == GameMode.FLAG) {
                            Text(
                                text = "Which country does this flag belong to?",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        when (uiState.selectedGameMode) {
                            GameMode.FLAG -> Text(
                                text = currentQuestion.flagEmoji,
                                style = MaterialTheme.typography.displayLarge
                            )
                            GameMode.OUTLINE -> {
                                CountryOutlineCard(
                                    countryName = currentQuestion.correctCountry,
                                    zoomFactor = outlineZoom.value
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isManualZoom) "Zoom" else "Auto zoom",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = outlineZoom.value,
                                    onValueChange = {
                                        if (!isManualZoom) {
                                            isManualZoom = true
                                            coroutineScope.launch { outlineZoom.stop() }
                                        }
                                        coroutineScope.launch {
                                            outlineZoom.snapTo(it.coerceIn(OutlineMinZoom, OutlineMaxZoom))
                                        }
                                    },
                                    valueRange = OutlineMinZoom..OutlineMaxZoom,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.selectedGameMode == GameMode.OUTLINE) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        userScrollEnabled = false
                    ) {
                        items(currentQuestion.options) { option ->
                            AnswerButton(
                                option = option,
                                optionFlag = flagLookup[option].orEmpty(),
                                isOutlineMode = true,
                                selectedAnswer = uiState.selectedAnswer,
                                correctCountry = currentQuestion.correctCountry,
                                onAnswerSelected = onAnswerSelected
                            )
                        }
                    }
                } else {
                    currentQuestion.options.forEach { option ->
                        AnswerButton(
                            option = option,
                            optionFlag = "",
                            isOutlineMode = false,
                            selectedAnswer = uiState.selectedAnswer,
                            correctCountry = currentQuestion.correctCountry,
                            onAnswerSelected = onAnswerSelected
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    OutlinedButton(onClick = onExitGame) {
                        Text(text = "Main Menu")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onNextFlag,
                        enabled = uiState.selectedAnswer != null
                    ) {
                        Text(text = if (uiState.currentQuestionIndex == uiState.totalQuestions - 1) "Finish" else "Next Question")
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerButton(
    option: String,
    optionFlag: String,
    isOutlineMode: Boolean,
    selectedAnswer: String?,
    correctCountry: String,
    onAnswerSelected: (String) -> Unit
) {
    val isSelected = selectedAnswer == option
    val isCorrect = option == correctCountry
    val buttonColors = when {
        selectedAnswer == null -> ButtonDefaults.buttonColors()
        isCorrect -> ButtonDefaults.buttonColors(
            containerColor = CorrectAnswerGreen,
            contentColor = Color.White,
            disabledContainerColor = CorrectAnswerGreen,
            disabledContentColor = Color.White
        )
        isSelected -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            disabledContainerColor = MaterialTheme.colorScheme.error,
            disabledContentColor = MaterialTheme.colorScheme.onError
        )
        else -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    Button(
        onClick = { onAnswerSelected(option) },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = buttonColors
    ) {
        if (isOutlineMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (optionFlag.isNotBlank()) {
                    Text(
                        text = optionFlag,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Text(
                    text = option,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Text(
                text = option,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
