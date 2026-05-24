package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GameViewModel
import com.example.ui.ScreenState
import com.example.ui.screens.GameOverScreen
import com.example.ui.screens.GameplayCanvas
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: GameViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()
        val playerStats by viewModel.playerStats.collectAsState()

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          // Use Box with full bleed, inside elements handle detailed padding
          Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
              ScreenState.HOME -> {
                HomeScreen(
                  viewModel = viewModel,
                  playerStats = playerStats,
                  onLaunchGame = { viewModel.startGame() }
                )
              }
              ScreenState.PLAYING -> {
                GameplayCanvas(
                  viewModel = viewModel,
                  playerStats = playerStats,
                  onFinishGame = { score, coins ->
                    // Set pilot profile name before saving score
                    val profileName = playerStats.unlockedShips.split(",").lastOrNull()?.uppercase() ?: "PILOT"
                    viewModel.finishGame(score, coins, profileName)
                  }
                )
              }
              ScreenState.GAME_OVER -> {
                GameOverScreen(
                  viewModel = viewModel,
                  onRestartGame = { viewModel.startGame() },
                  onHome = { viewModel.navigateTo(ScreenState.HOME) }
                )
              }
              ScreenState.SHOP -> {
                ShopScreen(
                  viewModel = viewModel,
                  playerStats = playerStats,
                  onBack = { viewModel.navigateTo(ScreenState.HOME) }
                )
              }
              ScreenState.LEADERBOARD -> {
                LeaderboardScreen(
                  viewModel = viewModel,
                  onBack = { viewModel.navigateTo(ScreenState.HOME) }
                )
              }
            }
          }
        }
      }
    }
  }
}
