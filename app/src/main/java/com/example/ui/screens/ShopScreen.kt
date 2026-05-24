package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerStatsEntity
import com.example.ui.GameViewModel
import com.example.ui.ScreenState
import com.example.ui.ShipSkin

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    viewModel: GameViewModel,
    playerStats: PlayerStatsEntity,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shipSkins = viewModel.shipSkins
    val unlockedList = playerStats.unlockedShips.split(",")
    val selectedId = playerStats.selectedShip

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "FLEET HANGAR",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("shop_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(text = "⭐ ", fontSize = 14.sp)
                        HorizontalSpacer(width = 4.dp)
                        Text(
                            text = playerStats.coins.toString(),
                            color = Color(0xFFFFD54F),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.testTag("shop_currency")
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C1B1F),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0B0E14),
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Text(
                        text = "EXCHANGE COSMIC CREDITS FOR EXPERIMENTAL STARSHIP COMBAT DESIGNS",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }

                items(shipSkins) { skin ->
                    val isUnlocked = unlockedList.contains(skin.id)
                    val isSelected = selectedId == skin.id

                    ShipSkinCard(
                        skin = skin,
                        isUnlocked = isUnlocked,
                        isSelected = isSelected,
                        onSelect = {
                            if (isUnlocked) {
                                viewModel.selectShip(skin.id)
                                Toast.makeText(context, "${skin.name} selected for flight", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.purchaseAndSelectShip(
                                    ship = skin,
                                    onSuccess = {
                                        Toast.makeText(context, "Successfully purchased and selected ${skin.name}!", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = { errorString ->
                                        Toast.makeText(context, errorString, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ShipSkinCard(
    skin: ShipSkin,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val brushGradient = Brush.radialGradient(
        colors = listOf(Color(skin.primaryColor).copy(alpha = 0.15f), Color.Transparent),
        radius = 500f
    )

    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, Color(0xFFD0BCFF))
    } else {
        BorderStroke(1.dp, Color(0xFF4A4458).copy(alpha = 0.4f))
    }

    val cardColor = Color(0xFF1C1B1F)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderStroke, RoundedCornerShape(16.dp))
            .testTag("ship_card_${skin.id}")
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brushGradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Color orb
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(skin.primaryColor), Color(skin.secondaryColor))
                                    )
                                )
                        )
                        HorizontalSpacer(width = 12.dp)
                        Column {
                            Text(
                                text = skin.name,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            Text(
                                text = skin.perk.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                color = Color(skin.primaryColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Balance Price or Active states icon
                    Box {
                        if (isSelected) {
                            Badge(
                                containerColor = Color(0xFFD0BCFF),
                                contentColor = Color(0xFF381E72),
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = Color(0xFF381E72)
                                    )
                                    HorizontalSpacer(width = 2.dp)
                                    Text(
                                        text = "ACTIVE",
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        } else if (isUnlocked) {
                            Badge(
                                containerColor = Color.DarkGray,
                                contentColor = Color.White,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = Color.White
                                    )
                                    HorizontalSpacer(width = 3.dp)
                                    Text(
                                        text = "SELECT",
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        } else {
                            Badge(
                                containerColor = Color(0xFFFFB300),
                                contentColor = Color.Black,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = Color.Black
                                    )
                                    HorizontalSpacer(width = 3.dp)
                                    Text(
                                        text = "${skin.cost} CR",
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                VerticalSpacer(height = 12.dp)

                Text(
                    text = skin.description,
                    color = Color.LightGray.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                VerticalSpacer(height = 14.dp)

                // Stats summary divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatSummary(label = "LASERS", value = "x${skin.laserCount}")
                    StatSummary(label = "PROPULSION", value = "x${String.format("%.2f", skin.speedMultiplier)}")
                    StatSummary(label = "SHIELD FORCE", value = if (skin.startingShield) "YES" else "NONE")
                }
            }
        }
    }
}

@Composable
fun StatSummary(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
