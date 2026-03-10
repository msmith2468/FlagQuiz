package com.example.flagquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flagquiz.ui.screen.FlagQuizGameScreen
import com.example.flagquiz.ui.screen.FlagQuizHomeScreen
import com.example.flagquiz.ui.theme.FlagQuizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlagQuizTheme {
                val flagQuizViewModel: FlagQuizViewModel = viewModel()
                val uiState by flagQuizViewModel.uiState.collectAsState()

                if (uiState.isGameStarted) {
                    FlagQuizGameScreen(
                        uiState = uiState,
                        onAnswerSelected = flagQuizViewModel::submitAnswer,
                        onNextFlag = flagQuizViewModel::loadNextQuestion,
                        onExitGame = flagQuizViewModel::returnHome
                    )
                } else {
                    FlagQuizHomeScreen(onStartGame = flagQuizViewModel::startGame)
                }
            }
        }
    }
}
