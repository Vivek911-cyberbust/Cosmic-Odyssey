package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerStatsEntity
import com.example.ui.GameViewModel
import com.example.ui.ScreenState

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    playerStats: PlayerStatsEntity,
    onLaunchGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shipSkins = viewModel.shipSkins
    val selectedShip = shipSkins.find { it.id == playerStats.selectedShip } ?: shipSkins.first()

    // Breathing neon pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "NeonGlow")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // User Pilot Name configuration
    var pilotNameInput by remember { mutableStateOf("Pilot") }

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

        // Decorative moving gradient stars glow background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(selectedShip.primaryColor).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Head: Credits and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All-Time Max High Score Reference
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "HIGH SCORE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color(0xFFD0BCFF),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = playerStats.maxScore.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Balance Coins Virtual Credit Tracker
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⭐ ", fontSize = 14.sp)
                        HorizontalSpacer(width = 4.dp)
                        Text(
                            text = playerStats.coins.toString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("currency_balance")
                        )
                    }
                }
            }

            // Central: Majestic Retrowave Title & Custom Visual Spaceship Preview
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                // Title
                Text(
                    text = "COSMIC",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 12.sp,
                    fontSize = 24.sp,
                    color = Color.LightGray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "ODYSSEY",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontSize = 46.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = Shadow(
                            color = Color(selectedShip.primaryColor).copy(alpha = alphaGlow),
                            blurRadius = 35f
                        )
                    )
                )

                VerticalSpacer(height = 8.dp)
                Text(
                    text = "ENDLESS SCI-FI VOID FLYER",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(selectedShip.primaryColor).copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                VerticalSpacer(height = 28.dp)

                // Render current active ship specifications inside a futuristic pod card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    border = BorderStroke(1.dp, Color(selectedShip.primaryColor).copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.widthIn(max = 350.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Visual ship colored circle marker
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(selectedShip.primaryColor),
                                            Color(selectedShip.secondaryColor)
                                        )
                                    )
                                )
                        )
                        VerticalSpacer(height = 8.dp)
                        Text(
                            text = selectedShip.name.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = selectedShip.perk,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold
                        )
                        VerticalSpacer(height = 6.dp)
                        Text(
                            text = selectedShip.description,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Lower Menu Option Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pilot Name Input Card (Compact & Glowing)
                OutlinedTextField(
                    value = pilotNameInput,
                    onValueChange = { if (it.length <= 12) pilotNameInput = it },
                    label = { Text("COSMIC PILOT CALLSIGN", color = Color(0xFF938F99), fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pilot_name_textfield"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFD0BCFF),
                        unfocusedBorderColor = Color(0xFF4A4458)
                    )
                )

                VerticalSpacer(height = 16.dp)

                // Launch Primary Game Loop Action button
                Button(
                    onClick = {
                        // Notify VM of player callsign and start gameplay
                        onLaunchGame()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("launch_mission_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, tint = Color(0xFF381E72))
                    HorizontalSpacer(width = 10.dp)
                    Text(
                        text = "LAUNCH STELLAR MISSION",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontSize = 14.sp
                    )
                }

                VerticalSpacer(height = 12.dp)

                // Hangar (Shop) and Leaderboard Columns side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Ship Skin Hangar Shop
                    Button(
                        onClick = { viewModel.navigateTo(ScreenState.SHOP) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("hangar_shop_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A4458),
                            contentColor = Color(0xFFE3E2E6)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        HorizontalSpacer(width = 6.dp)
                        Text(
                            text = "HANGAR",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Historical Leaderboard Registry
                    Button(
                        onClick = { viewModel.navigateTo(ScreenState.LEADERBOARD) },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("leaderboard_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A4458),
                            contentColor = Color(0xFFE3E2E6)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                        HorizontalSpacer(width = 6.dp)
                        Text(
                            text = "HIGH SCORES",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
