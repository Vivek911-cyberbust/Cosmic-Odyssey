package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerStatsEntity
import com.example.ui.*
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.*
import kotlin.random.Random

// Virtual Dimensions
private const val VIRTUAL_WIDTH = 1000f
private const val VIRTUAL_HEIGHT = 1600f

@Composable
fun GameplayCanvas(
    viewModel: GameViewModel,
    playerStats: PlayerStatsEntity,
    onFinishGame: (score: Int, coins: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedShipId = playerStats.selectedShip
    val activeShip = viewModel.shipSkins.find { it.id == selectedShipId } ?: viewModel.shipSkins.first()

    // Screen Dimensions in real pixels
    var canvasWidth by remember { mutableFloatStateOf(1000f) }
    var canvasHeight by remember { mutableFloatStateOf(1600f) }

    // Ratios to map virtual positions to physics
    val scaleX = canvasWidth / VIRTUAL_WIDTH
    val scaleY = canvasHeight / VIRTUAL_HEIGHT

    // Local Physics State
    var playerX by remember { mutableFloatStateOf(VIRTUAL_WIDTH / 2f) }
    var targetPlayerX by remember { mutableFloatStateOf(VIRTUAL_WIDTH / 2f) }
    val playerY = 1350f
    val playerRadius = 45f

    // Gameplay timers & states
    var score by remember { mutableIntStateOf(0) }
    var coinsCollectedInSession by remember { mutableIntStateOf(0) }
    var distanceTravelled by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var isSoundEnabled by remember { mutableStateOf(true) }

    // Game loops
    var lastFrameTime = remember { 0L }
    var difficultyFactor by remember { mutableFloatStateOf(1.0f) }

    // Active power-up states
    var shieldHp by remember { mutableIntStateOf(if (activeShip.startingShield) 1 else 0) }
    var isShieldActiveFromPowerUp by remember { mutableStateOf(false) }
    var magnetTimer by remember { mutableLongStateOf(0L) }
    var hyperDriveTimer by remember { mutableLongStateOf(0L) }
    var laserBlastTimer by remember { mutableLongStateOf(0L) }

    // Laser weapon cooldown
    var lastLaserTime by remember { mutableLongStateOf(0L) }

    // Juicy Screenshake state
    var shakeIntensity by remember { mutableFloatStateOf(0f) }

    // Elements
    val stars = remember { List(100) { BackgroundStar.createRandom() } }
    val asteroids = remember { mutableStateListOf<Asteroid>() }
    val projectiles = remember { mutableStateListOf<LaserProjectile>() }
    val coinsItems = remember { mutableStateListOf<CosmicCoin>() }
    val powerUpItems = remember { mutableStateListOf<PowerUpItem>() }
    val particles = remember { mutableStateListOf<ParticleEffect>() }

    // Unique IDs for Asteroids
    var asteroidIdCounter by remember { mutableIntStateOf(0) }

    // Spawning controls
    var lastAsteroidSpawnTime by remember { mutableLongStateOf(0L) }
    var lastCoinSpawnTime by remember { mutableLongStateOf(0L) }
    var lastPowerUpSpawnTime by remember { mutableLongStateOf(0L) }

    // Explosion spawner helper
    fun spawnExplosion(x: Float, y: Float, color: Color, count: Int = 12, multiplier: Float = 1f) {
        val countToSpawn = (count * multiplier).toInt()
        for (i in 0 until countToSpawn) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = 2f + Random.nextFloat() * 12f
            particles.add(
                ParticleEffect(
                    x = x,
                    y = y,
                    speedX = speed * cos(angle),
                    speedY = speed * sin(angle),
                    color = color,
                    size = 4f + Random.nextFloat() * 10f,
                    decay = 0.012f + Random.nextFloat() * 0.02f
                )
            )
        }
    }

    // Reset loop when game enters playing screen
    LaunchedEffect(Unit) {
        // Starters
        playerX = VIRTUAL_WIDTH / 2f
        targetPlayerX = VIRTUAL_WIDTH / 2f
        score = 0
        coinsCollectedInSession = 0
        distanceTravelled = 0f
        difficultyFactor = 1.0f
        shieldHp = if (activeShip.startingShield) 1 else 0
        isShieldActiveFromPowerUp = false
        magnetTimer = 0
        hyperDriveTimer = 0
        laserBlastTimer = 0
        lastFrameTime = System.currentTimeMillis()

        // Clear structures
        asteroids.clear()
        projectiles.clear()
        coinsItems.clear()
        powerUpItems.clear()
        particles.clear()
    }

    // Physics Engine Game-Loop
    LaunchedEffect(isPaused) {
        while (!isPaused && isActive) {
            withFrameMillis { time ->
                if (lastFrameTime == 0L) {
                    lastFrameTime = time
                }
                val rawDelta = time - lastFrameTime
                val dt = rawDelta.coerceIn(5, 50) // protect against frame drops
                lastFrameTime = time

                val timeNow = System.currentTimeMillis()

                // 1. Progress Difficulty & Distance
                distanceTravelled += 0.2f * dt
                difficultyFactor = 1.0f + (distanceTravelled / 2000f) // difficulty scales slowly up to ~3x
                score = (distanceTravelled / 10f).toInt() + (coinsCollectedInSession * 25)

                // 2. Linear Interpolate player movement (smooth gliding ease)
                playerX += (targetPlayerX - playerX) * 0.28f
                // Clamp within bounds
                playerX = playerX.coerceIn(playerRadius + 20f, VIRTUAL_WIDTH - playerRadius - 20f)

                // Decay Screen Shake
                if (shakeIntensity > 0.1f) {
                    shakeIntensity *= 0.90f
                } else {
                    shakeIntensity = 0f
                }

                // 3. Decrement powerup timers
                if (magnetTimer > 0) magnetTimer = (magnetTimer - dt).coerceAtLeast(0)
                if (hyperDriveTimer > 0) hyperDriveTimer = (hyperDriveTimer - dt).coerceAtLeast(0)
                if (laserBlastTimer > 0) laserBlastTimer = (laserBlastTimer - dt).coerceAtLeast(0)

                // 4. Star parallax background scroll
                stars.forEach { star ->
                    val multiplier = if (hyperDriveTimer > 0) 6f else 1f
                    star.y += star.speed * (dt / 16f) * multiplier
                    if (star.y > VIRTUAL_HEIGHT) {
                        star.y = -20f
                        star.x = Random.nextFloat() * VIRTUAL_WIDTH
                    }
                }

                // 5. Automatic Laser Firings (Weapon Cooldown)
                val isInHyper = hyperDriveTimer > 0
                val fireInterval = if (isInHyper) 120L else 320L
                if (timeNow - lastLaserTime >= fireInterval) {
                    lastLaserTime = timeNow

                    val activeLaserColor = activeShip.primaryColor
                    val projSpeed = -22f

                    // Powerup or Skin determined modes
                    val laserCount = if (laserBlastTimer > 0) 3 else activeShip.laserCount

                    when (laserCount) {
                        1 -> {
                            projectiles.add(
                                LaserProjectile(
                                    x = playerX,
                                    y = playerY - 40f,
                                    speedY = projSpeed,
                                    color = activeLaserColor
                                )
                            )
                        }
                        2 -> {
                            // Left and right wing firing points
                            projectiles.add(
                                LaserProjectile(
                                    x = playerX - 35f,
                                    y = playerY - 10f,
                                    speedY = projSpeed,
                                    color = activeLaserColor
                                )
                            )
                            projectiles.add(
                                LaserProjectile(
                                    x = playerX + 35f,
                                    y = playerY - 10f,
                                    speedY = projSpeed,
                                    color = activeLaserColor
                                )
                            )
                        }
                        3 -> {
                            // Triple scatter fire (Power Up or Solar Flare style)
                            projectiles.add(
                                LaserProjectile(
                                    x = playerX,
                                    y = playerY - 40f,
                                    speedY = projSpeed,
                                    angle = 0f,
                                    color = activeLaserColor
                                )
                            )
                            projectiles.add(
                                LaserProjectile(
                                    x = playerX - 15f,
                                    y = playerY - 30f,
                                    speedY = projSpeed - 1f,
                                    angle = -12f, // Left spread
                                    color = activeLaserColor
                                )
                            )
                            projectiles.add(
                                LaserProjectile(
                                    x = playerX + 15f,
                                    y = playerY - 30f,
                                    speedY = projSpeed - 1f,
                                    angle = 12f, // Right spread
                                    color = activeLaserColor
                                )
                            )
                        }
                    }

                    // Extra twin laser support for solar flare + powerup (quad fire!)
                    if (laserCount == 3 && activeShip.id == "solar_flare") {
                        // Wide wing blasters
                        projectiles.add(
                            LaserProjectile(
                                x = playerX - 50f,
                                y = playerY - 5f,
                                speedY = projSpeed,
                                angle = -20f,
                                color = activeLaserColor
                            )
                        )
                        projectiles.add(
                            LaserProjectile(
                                x = playerX + 50f,
                                y = playerY - 5f,
                                speedY = projSpeed,
                                angle = 20f,
                                color = activeLaserColor
                            )
                        )
                    }
                }

                // 6. Spawning Hazards & Rewards
                // Asteroid spawner (Every 1800ms / difficultyFactor)
                val spawnRate = (1800f / sqrt(difficultyFactor)).toLong().coerceIn(400L, 2000L)
                if (timeNow - lastAsteroidSpawnTime >= spawnRate) {
                    lastAsteroidSpawnTime = timeNow

                    // Decide randomized properties
                    val radius = 35f + Random.nextFloat() * 55f
                    val spawnX = radius + Random.nextFloat() * (VIRTUAL_WIDTH - 2f * radius)
                    val speedY = (3.5f + Random.nextFloat() * 6.5f) * difficultyFactor
                    val speedX = (Random.nextFloat() - 0.5f) * 3f * difficultyFactor
                    val hp = if (radius > 70f) 3 else if (radius > 50f) 2 else 1

                    asteroids.add(
                        Asteroid(
                            id = asteroidIdCounter++,
                            x = spawnX,
                            y = -100f,
                            speedX = speedX,
                            speedY = speedY,
                            radius = radius,
                            hp = hp,
                            maxHp = hp,
                            points = hp * 10
                        )
                    )
                }

                // Coin spawner (Every 2500ms)
                if (timeNow - lastCoinSpawnTime >= 2400L) {
                    lastCoinSpawnTime = timeNow
                    val coinRadius = 15f
                    val coinX = coinRadius + Random.nextFloat() * (VIRTUAL_WIDTH - 2f * coinRadius)
                    coinsItems.add(
                        CosmicCoin(
                            x = coinX,
                            y = -50f,
                            speedX = (Random.nextFloat() - 0.5f) * 1.5f,
                            speedY = 4f + Random.nextFloat() * 3f
                        )
                    )
                }

                // Power up spawner (Every 12 - 18 seconds)
                if (timeNow - lastPowerUpSpawnTime >= 14000L) {
                    lastPowerUpSpawnTime = timeNow
                    // Skip if too many are on-screen
                    if (powerUpItems.size < 2) {
                        val types = PowerUpType.values()
                        val type = types[Random.nextInt(types.size)]
                        val powerUpX = 50f + Random.nextFloat() * (VIRTUAL_WIDTH - 100f)
                        powerUpItems.add(
                            PowerUpItem(
                                type = type,
                                x = powerUpX,
                                y = -50f
                            )
                        )
                    }
                }

                // 7. Physics Updates & Movements
                // Projectiles
                val projIterator = projectiles.iterator()
                while (projIterator.hasNext()) {
                    val p = projIterator.next()
                    val angleRad = p.angle * Math.PI.toFloat() / 180f
                    p.x += p.speedY * sin(angleRad) * (dt / 16f)
                    p.y += p.speedY * cos(angleRad) * (dt / 16f)

                    if (p.y < -50f || p.x < -50f || p.x > VIRTUAL_WIDTH + 50f) {
                        projIterator.remove()
                    }
                }

                // Asteroids physics
                val astIterator = asteroids.iterator()
                while (astIterator.hasNext()) {
                    val a = astIterator.next()
                    a.x += a.speedX * (dt / 16f)
                    a.y += a.speedY * (dt / 16f)
                    a.rotation += a.rotationSpeed * (dt / 16f)

                    // Wrap elements horizontally or boundaries bounce
                    if (a.x < a.radius || a.x > VIRTUAL_WIDTH - a.radius) {
                        a.x = a.x.coerceIn(a.radius, VIRTUAL_WIDTH - a.radius)
                        // Reverse X speed on wall hit
                    }

                    if (a.y > VIRTUAL_HEIGHT + a.radius + 50f) {
                        astIterator.remove()
                    }
                }

                // Coins Physics
                val coinIterator = coinsItems.iterator()
                val magnetActive = magnetTimer > 0 || hyperDriveTimer > 0
                while (coinIterator.hasNext()) {
                    val c = coinIterator.next()
                    c.pulse += 0.08f * (dt / 16f)

                    if (magnetActive) {
                        // Pull coin directly towards the ship
                        val dx = playerX - c.x
                        val dy = playerY - c.y
                        val dist = sqrt(dx * dx + dy * dy)
                        val pullRange = if (hyperDriveTimer > 0) 500f else 280f

                        if (dist < pullRange) {
                            val magnetStrength = 14f * (1f - dist / pullRange)
                            c.speedX += (dx / dist) * magnetStrength
                            c.speedY += (dy / dist) * magnetStrength
                        }
                    }

                    c.x += c.speedX * (dt / 16f)
                    c.y += c.speedY * (dt / 16f)

                    // Clamp speed
                    val totalSpeed = sqrt(c.speedX * c.speedX + c.speedY * c.speedY)
                    if (totalSpeed > 24f) {
                        c.speedX = (c.speedX / totalSpeed) * 24f
                        c.speedY = (c.speedY / totalSpeed) * 24f
                    }

                    if (c.y > VIRTUAL_HEIGHT + 50f) {
                        coinIterator.remove()
                    }
                }

                // Power-Ups Physics
                val pwIterator = powerUpItems.iterator()
                while (pwIterator.hasNext()) {
                    val p = pwIterator.next()
                    p.pulse += 0.05f * (dt / 16f)
                    p.y += p.speedY * (dt / 16f)

                    if (p.y > VIRTUAL_HEIGHT + 50f) {
                        pwIterator.remove()
                    }
                }

                // Decay and movement of particle sparks
                val partIterator = particles.iterator()
                while (partIterator.hasNext()) {
                    val p = partIterator.next()
                    p.x += p.speedX * (dt / 16f)
                    p.y += p.speedY * (dt / 16f)
                    p.alpha -= p.decay * (dt / 16f)

                    if (p.alpha <= 0.01f) {
                        partIterator.remove()
                    }
                }

                // 8. COLLISION HANDLING
                // Laser Projectile hit on Asteroid
                val laserHits = mutableListOf<LaserProjectile>()
                val deadAsteroids = mutableListOf<Asteroid>()

                for (p in projectiles) {
                    for (a in asteroids) {
                        val dx = p.x - a.x
                        val dy = p.y - a.y
                        val distSq = dx * dx + dy * dy
                        val hitRad = a.radius + 15f // Expand slightly for hitting buffer

                        if (distSq < hitRad * hitRad) {
                            laserHits.add(p)
                            a.hp -= p.damage
                            
                            // Visual sparkles on impact
                            spawnExplosion(p.x, p.y, Color(p.color), count = 4, multiplier = 0.5f)

                            if (a.hp <= 0) {
                                deadAsteroids.add(a)
                            }
                            shakeIntensity = (shakeIntensity + 2f).coerceAtMost(8f)
                            break // Laser spent, check next projectile
                        }
                    }
                }
                projectiles.removeAll(laserHits)

                for (a in deadAsteroids) {
                    asteroids.remove(a)
                    // Large visual explosion
                    spawnExplosion(a.x, a.y, Color.Gray, count = 16, multiplier = a.radius / 30f)
                    spawnExplosion(a.x, a.y, Color(activeShip.primaryColor), count = 8, multiplier = a.radius / 45f)

                    // Grant score
                    distanceTravelled += a.points

                    // Spawn virtual Coins from debris! (~1-3 coins)
                    val coinsToSpawn = Random.nextInt(1, 4)
                    for (i in 0 until coinsToSpawn) {
                        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                        val speed = 2f + Random.nextFloat() * 4f
                        coinsItems.add(
                            CosmicCoin(
                                x = a.x,
                                y = a.y,
                                speedX = speed * cos(angle),
                                speedY = a.speedY * 0.5f + speed * sin(angle)
                            )
                        )
                    }
                }

                // Player Collisions with Coins
                val collectedCoins = mutableListOf<CosmicCoin>()
                for (c in coinsItems) {
                    val dx = playerX - c.x
                    val dy = playerY - c.y
                    val distSq = dx * dx + dy * dy
                    val collisionLimit = playerRadius + c.radius + 10f

                    if (distSq < collisionLimit * collisionLimit) {
                        collectedCoins.add(c)
                        coinsCollectedInSession += if (c.isRare) 5 else 1

                        // High fidelity coin absorption flash particle
                        spawnExplosion(c.x, c.y, Color.Yellow, count = 6, multiplier = 0.6f)
                    }
                }
                coinsItems.removeAll(collectedCoins)

                // Player Collisions with Powerups
                val collectedPowerUps = mutableListOf<PowerUpItem>()
                for (p in powerUpItems) {
                    val dx = playerX - p.x
                    val dy = playerY - p.y
                    val distSq = dx * dx + dy * dy
                    val collisionLimit = playerRadius + p.radius + 15f

                    if (distSq < collisionLimit * collisionLimit) {
                        collectedPowerUps.add(p)

                        // Apply the power up
                        when (p.type) {
                            PowerUpType.SHIELD -> {
                                shieldHp = (shieldHp + 1).coerceAtMost(3)
                                isShieldActiveFromPowerUp = true
                                spawnExplosion(p.x, p.y, Color(0xFF00E5FF), count = 18)
                            }
                            PowerUpType.MAGNET -> {
                                magnetTimer = 10000L // 10 seconds of magnet core
                                spawnExplosion(p.x, p.y, Color(0xFFFFD54F), count = 18)
                            }
                            PowerUpType.HYPER_DRIVE -> {
                                hyperDriveTimer = 6000L // 6 seconds of invulnerable hyper-speed
                                shieldHp = (shieldHp + 1).coerceAtMost(3)
                                spawnExplosion(p.x, p.y, Color(0xFFFF5722), count = 30)
                                shakeIntensity = 15f // Intense screenshake!
                            }
                            PowerUpType.LASER_BLAST -> {
                                laserBlastTimer = 8000L // 8 seconds of triple fire
                                spawnExplosion(p.x, p.y, Color(0xFFE040FB), count = 18)
                            }
                        }
                    }
                }
                powerUpItems.removeAll(collectedPowerUps)

                // Player Collisions with Asteroid (HAZARD!)
                if (hyperDriveTimer <= 0L) { // Player is completely invulnerable in Hyper Drive!
                    val hitAsteroids = mutableListOf<Asteroid>()
                    for (a in asteroids) {
                        val dx = playerX - a.x
                        val dy = playerY - a.y
                        val distSq = dx * dx + dy * dy
                        val collisionLimit = playerRadius + a.radius - 12f // slightly smaller buffer than visual radius for comfortable gameplay

                        if (distSq < collisionLimit * collisionLimit) {
                            hitAsteroids.add(a)

                            // Hit logic
                            if (shieldHp > 0) {
                                shieldHp--
                                shakeIntensity = 12f
                                spawnExplosion(playerX, playerY, Color(0xFF00B0FF), count = 25)
                            } else {
                                // NO SHIELD - DISASTER / GAME OVER!
                                isPaused = true
                                spawnExplosion(playerX, playerY, Color.Red, count = 45, multiplier = 2f)
                                spawnExplosion(playerX, playerY, Color(activeShip.primaryColor), count = 20)
                                shakeIntensity = 30f // Ultimate screen shake

                                // Finish the game session
                                onFinishGame(score, coinsCollectedInSession)
                            }
                        }
                    }
                    asteroids.removeAll(hitAsteroids)
                }
            }
        }
    }

    // Main Gameplay Screen UI Layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070514)) // Cosmic Deep Obsidian Background
    ) {
        // Core Interactive Gameplay Rendering Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("game_render_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Steer relative to drag magnitude
                        targetPlayerX += dragAmount.x / scaleX
                        targetPlayerX = targetPlayerX.coerceIn(50f, VIRTUAL_WIDTH - 50f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Snap or move smoothly to tapped location
                        targetPlayerX = offset.x / scaleX
                        targetPlayerX = targetPlayerX.coerceIn(50f, VIRTUAL_WIDTH - 50f)
                    }
                }
        ) {
            // Track dimensions dynamically on draw
            canvasWidth = size.width
            canvasHeight = size.height

            val randomShakeX = if (shakeIntensity > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity * 2f else 0f
            val randomShakeY = if (shakeIntensity > 0f) (Random.nextFloat() - 0.5f) * shakeIntensity * 2f else 0f

            // 1. Draw parallax stars scrolling background
            stars.forEach { star ->
                val drawX = star.x * scaleX
                val drawY = star.y * scaleY
                drawCircle(
                    color = Color.White.copy(alpha = star.brightness),
                    radius = star.size * scaleX * 0.5f,
                    center = Offset(drawX, drawY)
                )
            }

            // Apply screenshake transform to all foreground entities!
            withTransform({
                translate(left = randomShakeX, top = randomShakeY)
            }) {
                // 2. Draw falling energy Coins
                coinsItems.forEach { coin ->
                    val x = coin.x * scaleX
                    val y = coin.y * scaleY
                    val pulseRadius = coin.radius * scaleX * (1f + 0.15f * sin(coin.pulse))

                    val color = if (coin.isRare) {
                        Color(0xFFFFB300) // Deep Rare Amber Golden
                    } else {
                        Color(0xFFFFEE58) // Radiant Yellow
                    }

                    // Shimmer glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, color.copy(alpha = 0f)),
                            center = Offset(x, y),
                            radius = pulseRadius * 2f
                        ),
                        radius = pulseRadius * 2f,
                        center = Offset(x, y)
                    )

                    // Real solid coin core
                    drawCircle(
                        color = color,
                        radius = pulseRadius,
                        center = Offset(x, y)
                    )

                    // Inner coin details
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = pulseRadius * 0.5f,
                        center = Offset(x, y),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 3. Draw falling Power-Ups
                powerUpItems.forEach { power ->
                    val x = power.x * scaleX
                    val y = power.y * scaleY
                    val r = power.radius * scaleX
                    val corePulse = abs(sin(power.pulse * 2.5f))

                    val (color, symbolLetter) = when (power.type) {
                        PowerUpType.SHIELD -> Color(0xFF00E5FF) to "S"
                        PowerUpType.MAGNET -> Color(0xFFFFD54F) to "M"
                        PowerUpType.HYPER_DRIVE -> Color(0xFFFF5722) to "H"
                        PowerUpType.LASER_BLAST -> Color(0xFFE040FB) to "L"
                    }

                    // Outer glowing cloud
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0f)),
                            center = Offset(x, y),
                            radius = r * (1.8f + corePulse * 0.3f)
                        ),
                        radius = r * (1.8f + corePulse * 0.3f),
                        center = Offset(x, y)
                    )

                    // Ring
                    drawCircle(
                        color = color,
                        radius = r * (1f + corePulse * 0.15f),
                        center = Offset(x, y),
                        style = Stroke(width = 2.5f.dp.toPx())
                    )

                    // Inner bright core
                    drawCircle(
                        color = color.copy(alpha = 0.25f),
                        radius = r,
                        center = Offset(x, y)
                    )

                    // Center symbol
                    drawCircle(
                        color = Color.White,
                        radius = r * 0.45f,
                        center = Offset(x, y)
                    )
                }

                // 4. Draw Laser projectiles
                projectiles.forEach { proj ->
                    val x = proj.x * scaleX
                    val y = proj.y * scaleY
                    val laserColor = Color(proj.color)

                    // Laser line/cone visual representation
                    drawLine(
                        color = laserColor,
                        start = Offset(x, y + 25f * scaleY),
                        end = Offset(x, y - 25f * scaleY),
                        strokeWidth = 6f * scaleX,
                        cap = StrokeCap.Round
                    )

                    // Core intense white center spark
                    drawLine(
                        color = Color.White,
                        start = Offset(x, y + 15f * scaleY),
                        end = Offset(x, y - 15f * scaleY),
                        strokeWidth = 2.5f * scaleX,
                        cap = StrokeCap.Round
                    )
                }

                // 5. Draw physics Asteroids
                asteroids.forEach { rock ->
                    val x = rock.x * scaleX
                    val y = rock.y * scaleY
                    val r = rock.radius * scaleX

                    rotate(degrees = rock.rotation, pivot = Offset(x, y)) {
                        // Create deep rocky radial gradient shader
                        val rockGradient = Brush.radialGradient(
                            colors = listOf(Color(0xFF424242), Color(0xFF212121)),
                            center = Offset(x - r * 0.2f, y - r * 0.2f),
                            radius = r
                        )

                        // Asteroid body
                        drawCircle(
                            brush = rockGradient,
                            radius = r,
                            center = Offset(x, y)
                        )

                        // Crater indentations
                        rock.craterOffsets.forEach { crater ->
                            val cx = x + crater.dx * scaleX
                            val cy = y + crater.dy * scaleY
                            val cr = crater.r * scaleX

                            drawCircle(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF101010), Color(0xFF303030)),
                                    start = Offset(cx - cr, cy - cr),
                                    end = Offset(cx + cr, cy + cr)
                                ),
                                radius = cr,
                                center = Offset(cx, cy)
                            )
                        }

                        // Outline/Crust highlight
                        drawCircle(
                            color = Color(0xFF888888).copy(alpha = 0.4f),
                            radius = r,
                            center = Offset(x, y),
                            style = Stroke(width = 2f * scaleX)
                        )
                    }

                    // Draw HP bar for large multi-hit asteroids
                    if (rock.maxHp > 1 && rock.hp < rock.maxHp) {
                        val barWidth = r * 1.4f
                        val barHeight = 6f * scaleY
                        val barX = x - barWidth / 2f
                        val barY = y - r - 15f * scaleY

                        // Gray Background
                        drawRect(
                            color = Color.DarkGray,
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth, barHeight)
                        )
                        // Green Fill
                        val hpPercent = rock.hp.toFloat() / rock.maxHp.toFloat()
                        drawRect(
                            color = Color.Green,
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth * hpPercent, barHeight)
                        )
                    }
                }

                // 6. Draw particle sparks (Visual explosions)
                particles.forEach { part ->
                    val pX = part.x * scaleX
                    val pY = part.y * scaleY
                    val size = part.size * scaleX
                    drawCircle(
                        color = part.color.copy(alpha = part.alpha),
                        radius = size * 0.5f,
                        center = Offset(pX, pY)
                    )
                }

                // 7. Draw Player spaceship vector style!
                val pX = playerX * scaleX
                val pY = playerY * scaleY
                val pr = playerRadius * scaleX

                // A. Thruster Jet Flame flicker
                val flameFlicker = 0.85f + Random.nextFloat() * 0.3f
                val flameHeight = 85f * scaleY * flameFlicker
                val enginePath = Path().apply {
                    moveTo(pX, pY + pr)
                    lineTo(pX - 15f * scaleX, pY + pr + 30f * scaleY)
                    lineTo(pX, pY + pr + flameHeight)
                    lineTo(pX + 15f * scaleX, pY + pr + 30f * scaleY)
                    close()
                }

                // Flame colors
                val primaryShipColor = Color(activeShip.primaryColor)
                val secondaryShipColor = Color(activeShip.secondaryColor)

                drawPath(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryShipColor, Color.Transparent),
                        startY = pY + pr,
                        endY = pY + pr + flameHeight
                    ),
                    path = enginePath
                )

                // Alternate inner hot white center
                val innerFlamePath = Path().apply {
                    moveTo(pX, pY + pr)
                    lineTo(pX - 8f * scaleX, pY + pr + 15f * scaleY)
                    lineTo(pX, pY + pr + flameHeight * 0.55f)
                    lineTo(pX + 8f * scaleX, pY + pr + 15f * scaleY)
                    close()
                }
                drawPath(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        startY = pY + pr,
                        endY = pY + pr + flameHeight * 0.55f
                    ),
                    path = innerFlamePath
                )

                // B. Futuristic Spaceship Chassis Path (Swept wing fighter)
                val shipPath = Path().apply {
                    // Nose cone tip
                    moveTo(pX, pY - pr)
                    // Left Sweeping wing edge
                    lineTo(pX - pr * 0.85f, pY + pr * 0.75f)
                    // Left wing exhaust notch
                    lineTo(pX - pr * 0.5f, pY + pr * 0.5f)
                    // Core back thruster
                    lineTo(pX, pY + pr * 0.85f)
                    // Right wing exhaust notch
                    lineTo(pX + pr * 0.5f, pY + pr * 0.5f)
                    // Right Sweeping wing edge
                    lineTo(pX + pr * 0.85f, pY + pr * 0.75f)
                    close()
                }

                drawPath(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryShipColor, secondaryShipColor),
                        start = Offset(pX, pY - pr),
                        end = Offset(pX, pY + pr)
                    ),
                    path = shipPath
                )

                // C. Visual cockpit visor detail
                val canopyPath = Path().apply {
                    moveTo(pX, pY - pr * 0.4f)
                    lineTo(pX - pr * 0.2f, pY + pr * 0.1f)
                    lineTo(pX, pY + pr * 0.4f)
                    lineTo(pX + pr * 0.2f, pY + pr * 0.1f)
                    close()
                }
                drawPath(
                    color = Color.White.copy(alpha = 0.88f),
                    path = canopyPath
                )

                // Metallic reflex lines
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(pX - pr * 0.4f, pY + pr * 0.2f),
                    end = Offset(pX + pr * 0.4f, pY + pr * 0.2f),
                    strokeWidth = 2f * scaleX
                )

                // D. Active SHIELD glow bubble (If player is shielded)
                if (shieldHp > 0) {
                    val shieldRadius = pr * 1.55f
                    val shieldPulse = 1f + 0.04f * sin(System.currentTimeMillis() / 150f)
                    val activeShieldR = shieldRadius * shieldPulse

                    // Translucent energy shield gradient
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00B0FF).copy(alpha = 0.0f),
                                Color(0xFF00E5FF).copy(alpha = 0.10f),
                                Color(0xFF00E5FF).copy(alpha = 0.48f)
                            ),
                            center = Offset(pX, pY),
                            radius = activeShieldR
                        ),
                        radius = activeShieldR,
                        center = Offset(pX, pY)
                    )

                    // Electric border stroke ring
                    drawCircle(
                        color = Color(0xFFE0F7FA),
                        radius = activeShieldR,
                        center = Offset(pX, pY),
                        style = Stroke(width = 3.5f.dp.toPx())
                    )
                }

                // E. Active MAGNET conical visual core pulls
                if (magnetTimer > 0) {
                    val pulse = abs(sin(System.currentTimeMillis() / 200f))
                    val pullR = pr * (2.0f + pulse * 0.6f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFFEE58).copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(pX, pY),
                            radius = pullR
                        ),
                        radius = pullR,
                        center = Offset(pX, pY)
                    )
                }

                // F. Active Hyper Drive energy particles aura
                if (hyperDriveTimer > 0) {
                    val corePulse = abs(sin(System.currentTimeMillis() / 100f))
                    val hyperR = pr * (2.8f + corePulse * 0.4f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFFF5722).copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(pX, pY),
                            radius = hyperR
                        ),
                        radius = hyperR,
                        center = Offset(pX, pY)
                    )
                    // Extra orange electric aura frame rings
                    drawCircle(
                        color = Color(0xFFFFCC80),
                        radius = pr * (1.6f + corePulse * 0.15f),
                        center = Offset(pX, pY),
                        style = Stroke(
                            width = 2.5f.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )
                }
            }
        }

        // On-Screen HUD Overlay (Scores, multipliers, health, powerups)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            // Row 1: Score & Coins Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glow Score Widget
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SCORE: ",
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = score.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("on_screen_score")
                        )
                    }
                }

                // Coins Collected Tracker Widget
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ ",
                            fontSize = 14.sp
                        )
                        HorizontalSpacer(width = 4.dp)
                        Text(
                            text = coinsCollectedInSession.toString(),
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("on_screen_coins")
                        )
                    }
                }
            }

            VerticalSpacer(height = 8.dp)

            // Row 2: Shields / HP Gauge & Active Modifier Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // HP / Shield Hearts indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHIELDS: ",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalSpacer(width = 4.dp)
                    // Draw Shield battery ticks
                    for (i in 0 until 3) {
                        val active = i < shieldHp
                        val tickColor = if (active) Color(0xFF00E5FF) else Color.DarkGray.copy(alpha = 0.4f)
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(width = 16.dp, height = 10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(tickColor)
                        )
                    }
                }

                // Speed / Multiplier indication
                Text(
                    text = "X${String.format("%.1f", difficultyFactor)} SPD",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            VerticalSpacer(height = 12.dp)

            // Row 3: Active PowerUp indicators as slim slider status bars
            AnimatedVisibility(visible = magnetTimer > 0) {
                PowerUpProgressBar(
                    label = "COIN MAGNET",
                    timer = magnetTimer,
                    maxDuration = 10000f,
                    barColor = Color(0xFFFFD54F)
                )
            }

            AnimatedVisibility(visible = laserBlastTimer > 0) {
                PowerUpProgressBar(
                    label = "TRIPLE BLASTERS",
                    timer = laserBlastTimer,
                    maxDuration = 8000f,
                    barColor = Color(0xFFE040FB)
                )
            }

            AnimatedVisibility(visible = hyperDriveTimer > 0) {
                PowerUpProgressBar(
                    label = "HYPER DRIVE INVULNERABILITY",
                    timer = hyperDriveTimer,
                    maxDuration = 6000f,
                    barColor = Color(0xFFFF5722)
                )
            }
        }

        // Joystick overlay or touch hint helper (appears brief at bottom)
        if (distanceTravelled < 150f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Card(
                    modifier = Modifier.align(Alignment.Center),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "◀  DRAG LEFT / RIGHT TO STEER  ▶",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // On-Screen Pause Button Float
        IconButton(
            onClick = { isPaused = !isPaused },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(16.dp)
                .size(48.dp)
                .testTag("pause_game_button")
                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "Pause",
                tint = Color.White
            )
        }

        // Interactive Paused Screen Overlay dialog
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .pointerInput(Unit) { /* Eat events */ }
            ) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                        .widthIn(max = 450.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    border = BorderStroke(1.5.dp, Color(0xFFD0BCFF).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SYSTEMS FLIGHT PAUSED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            fontSize = 18.sp,
                            color = Color(0xFFD0BCFF)
                        )
                        VerticalSpacer(height = 16.dp)

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        VerticalSpacer(height = 20.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = "CURRENT SCORE\n$score",
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                            Text(
                                text = "CREDITS COINS\n⭐ $coinsCollectedInSession",
                                textAlign = TextAlign.Center,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }

                        VerticalSpacer(height = 28.dp)

                        Button(
                            onClick = { isPaused = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("resume_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            HorizontalSpacer(width = 8.dp)
                            Text(
                                text = "RESUME FLIGHT",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        VerticalSpacer(height = 12.dp)

                        OutlinedButton(
                            onClick = {
                                isPaused = false
                                onFinishGame(score, coinsCollectedInSession)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "ABORT CURRENT MISSION",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PowerUpProgressBar(
    label: String,
    timer: Long,
    maxDuration: Float,
    barColor: Color
) {
    val faction = (timer.toFloat() / maxDuration).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = barColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${String.format("%.1f", timer / 1000f)}s",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        VerticalSpacer(height = 2.dp)
        LinearProgressIndicator(
            progress = { faction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = Color.DarkGray.copy(alpha = 0.3f),
        )
    }
}

@Composable
fun HorizontalSpacer(width: androidx.compose.ui.unit.Dp) {
    Spacer(modifier = Modifier.width(width))
}

@Composable
fun VerticalSpacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(modifier = Modifier.height(height))
}
