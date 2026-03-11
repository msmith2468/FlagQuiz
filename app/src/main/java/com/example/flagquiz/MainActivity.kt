package com.example.flagquiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.flagquiz.navigation.AppScreen
import com.example.flagquiz.ui.screen.FlagQuizGameScreen
import com.example.flagquiz.ui.screen.FlagQuizHomeScreen
import com.example.flagquiz.ui.screen.FlagQuizSettingsScreen
import com.example.flagquiz.ui.screen.RegionBadgeCache
import com.example.flagquiz.ui.theme.FlagQuizTheme
import com.example.flagquiz.ui.viewmodel.FlagQuizViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val isSplashReady = AtomicBoolean(false)
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !isSplashReady.get() }

        enableEdgeToEdge()
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"

        lifecycleScope.launch(Dispatchers.Default) {
            runCatching {
                RegionBadgeCache.preloadAll(applicationContext)
            }
            isSplashReady.set(true)
        }

        setContent {
            FlagQuizTheme {
                val flagQuizViewModel: FlagQuizViewModel = viewModel()
                val uiState by flagQuizViewModel.uiState.collectAsState()

                if (uiState.currentScreen == AppScreen.SETTINGS) {
                    BackHandler(onBack = flagQuizViewModel::returnHome)
                }

                when (uiState.currentScreen) {
                    AppScreen.GAME -> FlagQuizGameScreen(
                        uiState = uiState,
                        onAnswerSelected = flagQuizViewModel::submitAnswer,
                        onNextFlag = flagQuizViewModel::loadNextQuestion,
                        onExitGame = flagQuizViewModel::returnHome,
                        onPlayAgain = flagQuizViewModel::restartCurrentRegion
                    )

                    AppScreen.SETTINGS -> FlagQuizSettingsScreen(
                        appVersion = appVersion,
                        onBack = flagQuizViewModel::returnHome,
                        onResetScores = flagQuizViewModel::resetScores
                    )

                    AppScreen.HOME -> FlagQuizHomeScreen(
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
