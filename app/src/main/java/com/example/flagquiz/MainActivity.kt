package com.example.flagquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flagquiz.ui.screen.FlagQuizGameScreen
import com.example.flagquiz.ui.screen.FlagQuizHomeScreen
import com.example.flagquiz.ui.screen.FlagQuizSettingsScreen
import com.example.flagquiz.ui.theme.FlagQuizTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"

        setContent {
            FlagQuizTheme {
                val flagQuizViewModel: FlagQuizViewModel = viewModel()
                val uiState by flagQuizViewModel.uiState.collectAsState()

                if (uiState.currentScreen == AppScreen.SETTINGS) {
                    BackHandler(onBack = flagQuizViewModel::returnHome)
                }

                when (uiState.currentScreen) {
                    AppScreen.GAME -> {
                        FlagQuizGameScreen(
                            uiState = uiState,
                            onAnswerSelected = flagQuizViewModel::submitAnswer,
                            onNextFlag = flagQuizViewModel::loadNextQuestion,
                            onExitGame = flagQuizViewModel::returnHome,
                            onPlayAgain = flagQuizViewModel::restartCurrentRegion
                        )
                    }

                    AppScreen.SETTINGS -> {
                        FlagQuizSettingsScreen(
                            appVersion = appVersion,
                            onBack = flagQuizViewModel::returnHome,
                            onResetScores = flagQuizViewModel::resetScores
                        )
                    }

                    AppScreen.HOME -> {
                        FlagQuizHomeScreen(
                            savedScores = uiState.savedScores,
                            regionCounts = uiState.regionCounts,
                            onStartGame = flagQuizViewModel::startGame,
                            onOpenSettings = flagQuizViewModel::openSettings
                        )
                    }
                }
            }
        }
    }
}
