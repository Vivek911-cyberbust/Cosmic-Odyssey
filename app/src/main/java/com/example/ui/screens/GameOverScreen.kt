package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GameViewModel
import com.example.ui.ScreenState

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    onRestartGame: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val score by viewModel.currentScore.collectAsState()
    val coinsCollected by viewModel.sessionCoins.collectAsState()
    val isNewHighScore by viewModel.isNewHighScore.collectAsState()

    // Pulsing size animation for high score notification
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)) // Deep Obsidian background
    ) {
        // Atmosphere Glow: Top-centered radial gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1A2130).copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = 1600f
                    )
                )
        )
        // Bottom atmosphere fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0B0E14))
                    )
                )
        )

        // Red starburst background aura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Red.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        radius = 1100f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Signal warning alert header
            Text(
                text = "HULL INTEGRITY COMPROMISED",
                color = Color.Red,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            VerticalSpacer(height = 6.dp)

            Text(
                text = "MISSION OVER",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 38.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(
                        color = Color.Red.copy(alpha = 0.8f),
                        blurRadius = 25f
                    )
                )
            )

            VerticalSpacer(height = 24.dp)

            // Celebration Highlight if New Personal Record
            if (isNewHighScore) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD54F).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.4f)),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👑 NEW SECTOR HIGH SCORE! 👑",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFFFFD54F),
                            textAlign = TextAlign.Center,
                            style = LocalTextStyle.current.copy(
                                shadow = Shadow(color = Color.Yellow, blurRadius = 15f)
                            )
                        )
                        Text(
                            text = "A legendary performance, Pilot!",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                VerticalSpacer(height = 24.dp)
            }

            // Game Statistics Display Pod
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FLIGHT ANALYTICS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFD0BCFF),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    VerticalSpacer(height = 20.dp)

                    // Score Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = score.toString(),
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("game_over_score")
                        )
                    }

                    VerticalSpacer(height = 12.dp)

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    VerticalSpacer(height = 12.dp)

                    // Gold Coins Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CREDITS COLLECTED",
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "+$coinsCollected ⭐",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFFD54F),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("game_over_coins")
                        )
                    }
                }
            }

            VerticalSpacer(height = 40.dp)

            // Primary Navigation Options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retry button (Fly Again)
                Button(
                    onClick = onRestartGame,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("retry_run_button")
                ) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = Color(0xFF381E72))
                    HorizontalSpacer(width = 8.dp)
                    Text(
                        text = "LAUNCH NEW MISSION",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Return Home base button
                Button(
                    onClick = onHome,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A4458),
                        contentColor = Color(0xFFE3E2E6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("return_home_button")
                ) {
                    Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = Color(0xFFE3E2E6))
                    HorizontalSpacer(width = 8.dp)
                    Text(
                        text = "RETURN TO SPACE BASE",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
