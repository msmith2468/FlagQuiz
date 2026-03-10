package com.example.flagquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.flagquiz.ui.theme.FlagQuizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlagQuizTheme {
                FlagQuizApp()
            }
        }
    }
}

@Composable
fun FlagQuizApp() {
    var score by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }
    var currentQuestion by remember { mutableStateOf(generateQuestion()) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize()) {
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
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(
                        text = "Round $round",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Score $score",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
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
                    val isSelected = selectedAnswer == option
                    val isCorrect = option == currentQuestion.correctCountry
                    val buttonColors = when {
                        selectedAnswer == null -> ButtonDefaults.buttonColors()
                        isCorrect -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                        isSelected -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                        else -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedAnswer == null) {
                                selectedAnswer = option
                                if (isCorrect) {
                                    score += 1
                                }
                            }
                        },
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

                val resultText = when (selectedAnswer) {
                    null -> "Pick the country that matches the flag."
                    currentQuestion.correctCountry -> "Correct!"
                    else -> "Not quite. It was ${currentQuestion.correctCountry}."
                }
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        currentQuestion = generateQuestion(currentQuestion.correctCountry)
                        selectedAnswer = null
                        round += 1
                    },
                    enabled = selectedAnswer != null
                ) {
                    Text(text = "Next Flag")
                }
            }
        }
    }
}

private data class FlagEntry(
    val country: String,
    val flagEmoji: String
)

private data class FlagQuestion(
    val flagEmoji: String,
    val correctCountry: String,
    val options: List<String>
)

private val flagEntries = listOf(
    FlagEntry("Canada", "\uD83C\uDDE8\uD83C\uDDE6"),
    FlagEntry("Japan", "\uD83C\uDDEF\uD83C\uDDF5"),
    FlagEntry("Brazil", "\uD83C\uDDE7\uD83C\uDDF7"),
    FlagEntry("Germany", "\uD83C\uDDE9\uD83C\uDDEA"),
    FlagEntry("Italy", "\uD83C\uDDEE\uD83C\uDDF9"),
    FlagEntry("India", "\uD83C\uDDEE\uD83C\uDDF3"),
    FlagEntry("Mexico", "\uD83C\uDDF2\uD83C\uDDFD"),
    FlagEntry("South Korea", "\uD83C\uDDF0\uD83C\uDDF7"),
    FlagEntry("Australia", "\uD83C\uDDE6\uD83C\uDDFA"),
    FlagEntry("France", "\uD83C\uDDEB\uD83C\uDDF7")
)

private fun generateQuestion(previousCountry: String? = null): FlagQuestion {
    val availableFlags = flagEntries.filterNot { it.country == previousCountry }
    val correctEntry = availableFlags.random()
    val distractors = flagEntries
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

@Preview(showBackground = true)
@Composable
fun FlagQuizPreview() {
    FlagQuizTheme {
        FlagQuizApp()
    }
}
