package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 0,
    val unlockedShips: String = "default",
    val selectedShip: String = "default",
    val maxScore: Int = 0
)
