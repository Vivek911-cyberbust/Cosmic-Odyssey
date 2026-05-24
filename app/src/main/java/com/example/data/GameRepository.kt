package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepository(private val gameDao: GameDao) {

    val topHighScores: Flow<List<HighScoreEntity>> = gameDao.getTopTenHighScores()

    val playerStats: Flow<PlayerStatsEntity> = gameDao.getPlayerStats().map { it ?: PlayerStatsEntity() }

    suspend fun getPlayerStatsDirect(): PlayerStatsEntity {
        return gameDao.getPlayerStatsDirect() ?: PlayerStatsEntity()
    }

    suspend fun savePlayerStats(stats: PlayerStatsEntity) {
        gameDao.insertPlayerStats(stats)
    }

    suspend fun addCoins(amount: Int) {
        val current = getPlayerStatsDirect()
        val updated = current.copy(coins = current.coins + amount)
        gameDao.insertPlayerStats(updated)
    }

    suspend fun unlockShip(shipId: String, cost: Int): Boolean {
        val current = getPlayerStatsDirect()
        if (current.coins >= cost) {
            val updatedUnlocked = if (current.unlockedShips.split(",").contains(shipId)) {
                current.unlockedShips
            } else {
                "${current.unlockedShips},$shipId"
            }
            val updated = current.copy(
                coins = current.coins - cost,
                unlockedShips = updatedUnlocked,
                selectedShip = shipId
            )
            gameDao.insertPlayerStats(updated)
            return true
        }
        return false
    }

    suspend fun selectShip(shipId: String) {
        val current = getPlayerStatsDirect()
        if (current.unlockedShips.split(",").contains(shipId)) {
            val updated = current.copy(selectedShip = shipId)
            gameDao.insertPlayerStats(updated)
        }
    }

    suspend fun saveHighScoreAndStats(playerName: String, score: Int, coinsCollected: Int): Boolean {
        // Insert score entry
        gameDao.insertHighScore(
            HighScoreEntity(
                playerName = playerName,
                score = score,
                coinsCollected = coinsCollected
            )
        )

        // Update overall stats
        val current = getPlayerStatsDirect()
        val isNewMax = score > current.maxScore
        val updated = current.copy(
            coins = current.coins + coinsCollected,
            maxScore = if (isNewMax) score else current.maxScore
        )
        gameDao.insertPlayerStats(updated)
        return isNewMax
    }
}
