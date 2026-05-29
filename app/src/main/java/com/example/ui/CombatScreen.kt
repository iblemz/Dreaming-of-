package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ItemType
import com.example.data.model.LootItem
import com.example.game.DungeonGenerator
import com.example.viewmodel.GameViewModel

@Composable
fun CombatScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val mob = viewModel.activeMob ?: return
    val logs = viewModel.combatLogs
    val lazyListState = rememberLazyListState()

    // Auto-scroll combat logs to bottom
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    // Elegant Dark combat slate background
    val combatBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFF15191E),
            MaterialTheme.colorScheme.background
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(combatBg)
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. TOP STATUS PILL - ENEMIES
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "⚠️ BULLY CONFRONTATION ⚠️",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF0055)
                )
            )
            Text(
                text = mob.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Enemy HP bar
            val hpPct = (mob.hp.toFloat() / mob.maxHp).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Text(
                    text = "HP ",
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hpPct)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF0055), Color(0xFFFF69B4))
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${mob.hp}/${mob.maxHp}",
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // 2. CENTRAL RENDERER PORT - Visual represent of Mob
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFFFF0055).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // We reuse the RetroViewport component but passing a simple faux mini map to only draw the monster!
            val combatFakeGrid = listOf(
                listOf(0, 0, 0),
                listOf(0, 1, 0),
                listOf(0, 0, 0)
            )
            RetroViewport(
                grid = combatFakeGrid,
                playerX = 1,
                playerY = 1,
                playerDir = 0,
                mobs = listOf(mob.copy(x = 1, y = 0)), // draw target nearby
                chests = emptyList(),
                nightmareIntensity = viewModel.nightmareIntensity,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic intensity gauge indicator overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    "Nightmare: ${(viewModel.nightmareIntensity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFF69B4),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // 3. RETRO TERMINAL COMBAT LOGS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090212))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = if (log.startsWith("⚔️") || log.contains("You strike")) Color(0xFF00FFCC)
                            else if (log.contains("Bubble") || log.contains("Created")) Color(0xFF00FFFF)
                            else if (log.startsWith("👾")) Color(0xFFFF00AA)
                            else Color.LightGray
                        )
                    )
                }
            }
        }

        // 4. PLAYER STATUS PANEL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "👶 COURAGEOUS SPIRIT (lvl ${viewModel.playerLevel})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDCDCDC)
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ATK: ${viewModel.playerAtkRating()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
                        )
                        Text(
                            "DEF: ${viewModel.playerDefRating()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
                        )
                        Text(
                            "Shards: ${viewModel.dreamShards}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color(0xFFFFD700))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Player HP
            val pMaxHp = viewModel.playerTotalMaxHp()
            val pLifePct = (viewModel.playerHp.toFloat() / pMaxHp).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "HP ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90EE90),
                    fontSize = 11.sp,
                    modifier = Modifier.width(24.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pLifePct)
                            .fillMaxHeight()
                            .background(Color(0xFF32CD32))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${viewModel.playerHp}/$pMaxHp",
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Player DP
            val pDpPct = (viewModel.playerDp.toFloat() / viewModel.playerMaxDp).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = "DP ",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00BFFF),
                    fontSize = 11.sp,
                    modifier = Modifier.width(24.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pDpPct)
                            .fillMaxHeight()
                            .background(Color(0xFF1E90FF))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${viewModel.playerDp}/${viewModel.playerMaxDp}",
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 5. INTERACTIVE ACTION PANEL
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            if (!viewModel.isPlayerTurn) {
                // Enemy's turn banner overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF0055).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFFF0055).copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFFFF0055), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${mob.name} is preparing its dread...",
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF0055),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                when (viewModel.combatSubMenu) {
                    "MAIN" -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.selectCombatAction("ATTACK") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF69B4)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("combat_attack_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ATTACK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Text(viewModel.equippedWeapon?.name ?: "Pebble Slingshot", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                Button(
                                    onClick = { viewModel.combatSubMenu = "ITEMS" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF32CD32)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("combat_items_menu_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("USE ITEM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.combatSubMenu = "SKILLS" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("combat_skills_menu_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("IMAGINE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.selectCombatAction("RUN") },
                                    border = BorderStroke(2.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("combat_run_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("RUN DOOR 🚪", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    "SKILLS" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Skill 1
                                Button(
                                    onClick = { viewModel.selectCombatAction("SKILL_PROJECTILE") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4682B4)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🫧 Bubble Shield", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Heal 25 HP | cost 5 dp", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }

                                // Skill 2
                                Button(
                                    onClick = { viewModel.selectCombatAction("SKILL_STRIKE") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2691E)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔦 Flash Burst", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Dealt 36+ dmg | cost 8 dp", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.combatSubMenu = "MAIN" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BACK", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    "ITEMS" -> {
                        val consumables = viewModel.inventory.filter { it.type == ItemType.CONSUMABLE && it.count > 0 }
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (consumables.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Your pajama pockets are empty!",
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(consumables) { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .clickable {
                                                    if (viewModel.isPlayerTurn) {
                                                        viewModel.useConsumable(item)
                                                        viewModel.combatSubMenu = "MAIN"
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    "${item.name} (${item.count})",
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                                Text(
                                                    item.effectDescription,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.combatSubMenu = "MAIN" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BACK", fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }
    }
}
