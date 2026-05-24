package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.HighScoreEntity
import com.example.data.PlayerStatsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenState {
    HOME,
    PLAYING,
    GAME_OVER,
    SHOP,
    LEADERBOARD
}

data class ShipSkin(
    val id: String,
    val name: String,
    val cost: Int,
    val perk: String,
    val description: String,
    val primaryColor: Long, // Hex color value
    val secondaryColor: Long,
    val laserCount: Int,
    val speedMultiplier: Float,
    val startingShield: Boolean
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
    }

    // High Scores from database
    val topHighScores: StateFlow<List<HighScoreEntity>> = repository.topHighScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Player Stats from database
    val playerStats: StateFlow<PlayerStatsEntity> = repository.playerStats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerStatsEntity()
        )

    // Navigation and screen routing
    private val _currentScreen = MutableStateFlow(ScreenState.HOME)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // Current game statistics for active / just finished session
    private val _currentScore = MutableStateFlow(0)
    val currentScore: StateFlow<Int> = _currentScore.asStateFlow()

    private val _sessionCoins = MutableStateFlow(0)
    val sessionCoins: StateFlow<Int> = _sessionCoins.asStateFlow()

    private val _isNewHighScore = MutableStateFlow(false)
    val isNewHighScore: StateFlow<Boolean> = _isNewHighScore.asStateFlow()

    // Available Ship Skins list
    val shipSkins = listOf(
        ShipSkin(
            id = "default",
            name = "Rogue Vanguard",
            cost = 0,
            perk = "Standard Blasters",
            description = "A swift recon fighter balanced for navigating asteroid clusters.",
            primaryColor = 0xFF00E5FF, // Cyan
            secondaryColor = 0xFF2979FF, // Royal Blue
            laserCount = 1,
            speedMultiplier = 1.0f,
            startingShield = false
        ),
        ShipSkin(
            id = "phoenix",
            name = "Phoenix Wing",
            cost = 150,
            perk = "Double Laser Barrage",
            description = "Rebuilt from scorched starfighter wreckage. Fires dual high-frequency plasma canons.",
            primaryColor = 0xFFFF5722, // Fiery Orange
            secondaryColor = 0xFFFFC107, // Sun Yellow
            laserCount = 2,
            speedMultiplier = 1.05f,
            startingShield = false
        ),
        ShipSkin(
            id = "specter",
            name = "Void Specter",
            cost = 350,
            perk = "Sub-Shield Core",
            description = "Utilizes dark matter propulsion. Spawns with a kinetic energy shield ready to deflect hits.",
            primaryColor = 0xFFE040FB, // Vibrant Purple
            secondaryColor = 0xFF7C4DFF, // Indigo
            laserCount = 1,
            speedMultiplier = 1.15f,
            startingShield = true
        ),
        ShipSkin(
            id = "solar_flare",
            name = "Solar Flare",
            cost = 600,
            perk = "Triple Laser Storm",
            description = "Forged in solar orbits. Fills the void with wide triple laser scatter blasts.",
            primaryColor = 0xFFFFEB3B, // Solar Yellow
            secondaryColor = 0xFFF44336, // Solar Flare Red
            laserCount = 3,
            speedMultiplier = 1.25f,
            startingShield = true
        )
    )

    fun navigateTo(state: ScreenState) {
        _currentScreen.value = state
    }

    fun selectShip(shipId: String) {
        viewModelScope.launch {
            repository.selectShip(shipId)
        }
    }

    fun purchaseAndSelectShip(ship: ShipSkin, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.unlockShip(ship.id, ship.cost)
            if (success) {
                onSuccess()
            } else {
                onFailure("Not enough cosmic credits! Keep playing to collect more.")
            }
        }
    }

    fun startGame() {
        _currentScore.value = 0
        _sessionCoins.value = 0
        _isNewHighScore.value = false
        _currentScreen.value = ScreenState.PLAYING
    }

    fun finishGame(finalScore: Int, coinsEarned: Int, playerName: String) {
        _currentScore.value = finalScore
        _sessionCoins.value = coinsEarned
        
        viewModelScope.launch {
            val nameToSave = playerName.ifBlank { "Pilot" }
            val isNewMax = repository.saveHighScoreAndStats(nameToSave, finalScore, coinsEarned)
            _isNewHighScore.value = isNewMax
            _currentScreen.value = ScreenState.GAME_OVER
        }
    }

    fun claimDailyCredits() {
        viewModelScope.launch {
            repository.addCoins(50)
        }
    }
}
