package com.example.ui

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

enum class PowerUpType {
    SHIELD,       // Kinetic Blue barrier
    MAGNET,       // Golden tractor beam pulling in coins
    HYPER_DRIVE,  // Invulnerable super boost, hyper fire
    LASER_BLAST   // Multi-directional scatter barrage
}

data class BackgroundStar(
    var x: Float,
    var y: Float,
    val speed: Float,
    val size: Float,
    val brightness: Float
) {
    companion object {
        fun createRandom(): BackgroundStar {
            return BackgroundStar(
                x = Random.nextFloat() * 1000f,
                y = Random.nextFloat() * 1600f,
                speed = 1f + Random.nextFloat() * 5f,
                size = 1.5f + Random.nextFloat() * 4f,
                brightness = 0.4f + Random.nextFloat() * 0.6f
            )
        }
    }
}

data class Asteroid(
    val id: Int,
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val radius: Float,
    var rotation: Float = 0f,
    val rotationSpeed: Float = (Random.nextFloat() - 0.5f) * 4f,
    var hp: Int,
    val maxHp: Int,
    val points: Int,
    val craterOffsets: List<CraterOffset> = generateCraters(radius)
) {
    companion object {
        private fun generateCraters(radius: Float): List<CraterOffset> {
            val list = mutableListOf<CraterOffset>()
            val count = (radius / 15f).toInt().coerceIn(2, 6)
            for (i in 0 until count) {
                // Keep craters within the asteroid surface
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                val dist = Random.nextFloat() * (radius * 0.5f)
                list.add(
                    CraterOffset(
                        dx = dist * kotlin.math.cos(angle),
                        dy = dist * kotlin.math.sin(angle),
                        r = 4f + Random.nextFloat() * (radius * 0.25f)
                    )
                )
            }
            return list
        }
    }
}

data class CraterOffset(val dx: Float, val dy: Float, val r: Float)

data class LaserProjectile(
    var x: Float,
    var y: Float,
    val speedY: Float,
    val angle: Float = 0f,
    val damage: Int = 1,
    val color: Long = 0xFF00FF00
)

data class CosmicCoin(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    val radius: Float = 14f,
    val isRare: Boolean = Random.nextFloat() < 0.12f,
    var pulse: Float = Random.nextFloat() * 2f * Math.PI.toFloat()
)

data class PowerUpItem(
    val type: PowerUpType,
    var x: Float,
    var y: Float,
    val speedY: Float = 4f,
    val radius: Float = 24f,
    var pulse: Float = 0f
)

data class ParticleEffect(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    var alpha: Float = 1.0f,
    val decay: Float = 0.015f + Random.nextFloat() * 0.02f
)
