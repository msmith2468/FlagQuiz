package com.example.flagquiz.ui.screen

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flagquiz.FlagQuizUiState
import kotlin.math.roundToInt

private val CorrectAnswerGreen = Color(0xFF2E7D32)

@Composable
fun FlagQuizGameScreen(
    uiState: FlagQuizUiState,
    onAnswerSelected: (String) -> Unit,
    onNextFlag: () -> Unit,
    onExitGame: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val currentQuestion = uiState.currentQuestion ?: return
    val scorePercentage = if (uiState.totalQuestions == 0) {
        0
    } else {
        (uiState.score.toFloat() / uiState.totalQuestions.toFloat() * 100).roundToInt()
    }
    val resultText = when (uiState.selectedAnswer) {
        null -> "Pick the country that matches the flag."
        currentQuestion.correctCountry -> "Correct!"
        else -> "Not quite. It was ${currentQuestion.correctCountry}."
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Flag Quiz",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.selectedRegion.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = if (uiState.isGameComplete) "Finished" else "Flag ${uiState.currentQuestionIndex + 1} of ${uiState.totalQuestions}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Score ${uiState.score}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isGameComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Which country does this flag belong to?",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = currentQuestion.flagEmoji,
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                currentQuestion.options.forEach { option ->
                    val isSelected = uiState.selectedAnswer == option
                    val isCorrect = option == currentQuestion.correctCountry
                    val buttonColors = when {
                        uiState.selectedAnswer == null -> ButtonDefaults.buttonColors()
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
                        Text(
                            text = option,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                        Text(text = if (uiState.currentQuestionIndex == uiState.totalQuestions - 1) "Finish" else "Next Flag")
                    }
                }
            }
        }
    }
}
