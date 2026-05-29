package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChestInstance
import com.example.data.model.ItemType
import com.example.data.model.LootItem
import com.example.game.DungeonGenerator
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenState
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun MainGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    var showInventoryOverlay by remember { mutableStateOf(false) }
    var tickTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Run tick animation timer for wobbles
    LaunchedEffect(Unit) {
        while (true) {
            tickTime = System.currentTimeMillis()
            delay(50)
        }
    }

    val elegantDarkBg = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            Color(0xFF15191E),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(elegantDarkBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER PANEL (Score, floor, pinch awake)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLOOR ${viewModel.currentFloor}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFFFD700),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Level ${viewModel.playerLevel} Student",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1F1235))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "confidence: ${viewModel.dreamShards}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.checkAbandonCurrentGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("abandon_button"),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("GIVE UP ESCAPE 🚪", fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. 3D HALLWAY VIEWPORT (Spacious core 3D gameplay graphics)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, getDynamicIntensityColor(viewModel.nightmareIntensity), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                RetroViewport(
                    grid = viewModel.dungeonGrid,
                    playerX = viewModel.playerX,
                    playerY = viewModel.playerY,
                    playerDir = viewModel.playerDir,
                    mobs = viewModel.levelMobs,
                    chests = viewModel.levelChests,
                    nightmareIntensity = viewModel.nightmareIntensity,
                    tickTime = tickTime,
                    modifier = Modifier.fillMaxSize()
                )

                // Flash overlay on high intensity values
                if (viewModel.nightmareIntensity >= 0.75f) {
                    val alertPulse = (1.0f + sin((tickTime / 100.0))).toFloat() * 0.12f
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Red.copy(alpha = alertPulse.coerceAtLeast(0f)))
                    )
                }

                // Directions Overlay Label
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = viewModel.dirLabel(viewModel.playerDir),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 3. NIGHTMARE INTENSITY METER
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHOOLYARD ANXIETY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = getDynamicIntensityColor(viewModel.nightmareIntensity)
                        )
                    )
                    Text(
                        text = getIntensityDescription(viewModel.nightmareIntensity),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Beautiful gradient meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(viewModel.nightmareIntensity)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF00FFCC), // Calming sky lights
                                        Color(0xFFFFFF00), // Warnings yellow
                                        Color(0xFFFF0055)  // Red nightmares
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. MAIN ACTION CONTROL PANEL AREA (Mini-map & Game Controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Side - Mini Map Widget Card
                Box(
                    modifier = Modifier
                        .width(135.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    MiniMap(
                        grid = viewModel.dungeonGrid,
                        visited = viewModel.dungeonVisited,
                        playerX = viewModel.playerX,
                        playerY = viewModel.playerY,
                        playerDir = viewModel.playerDir,
                        chests = viewModel.levelChests,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Right Side - Controller Panel
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Interaction Hot Row (Chest, Stairs, Inventory)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeChest = viewModel.canInteractChest()
                        val isStandingOnExit = viewModel.checkStandingOnExit()

                        if (activeChest != null) {
                            Button(
                                onClick = { viewModel.openChest(activeChest) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("open_chest_button"),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("OPEN 🎁", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color.Black, fontSize = 11.sp)
                            }
                        } else if (isStandingOnExit) {
                            Button(
                                onClick = { viewModel.descendStairs() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("descend_stairs_button"),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DESCEND 🪜", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black, color = Color.Black, fontSize = 11.sp)
                            }
                        } else {
                            // Dummy spacer/status filler
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.04f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "HP: ${viewModel.playerHp}/${viewModel.playerTotalMaxHp()}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF90EE90)
                                    )
                                )
                            }
                        }

                        // Bag Button
                        Button(
                            onClick = { showInventoryOverlay = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("bag_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BAG 🎒", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    // Classical Directional D-PAD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.turnLeft() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .testTag("turn_left_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Turn Left", tint = Color.White)
                        }

                        IconButton(
                            onClick = { viewModel.moveForward() },
                            modifier = Modifier
                                .size(54.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .testTag("move_forward_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Forward", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = { viewModel.turnRight() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                                .testTag("turn_right_button")
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Turn Right", tint = Color.White)
                        }
                    }
                }
            }
        }

        // 5. SLIDEOUT INVENTORY OVERLAY
        AnimatedVisibility(
            visible = showInventoryOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut()
        ) {
            InventoryOverlay(
                viewModel = viewModel,
                onClose = { showInventoryOverlay = false }
            )
        }
    }
}

// Center Localized Mini Map Visual on Canvas
@Composable
fun MiniMap(
    grid: List<List<Int>>,
    visited: List<List<Boolean>>,
    playerX: Int,
    playerY: Int,
    playerDir: Int,
    chests: List<ChestInstance>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Define grid scale based on a 7x7 localized window
        val radius = 3
        val cellSize = w / (radius * 2 + 1)

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val gx = playerX + dx
                val gy = playerY + dy

                val screenX = (dx + radius) * cellSize
                val screenY = (dy + radius) * cellSize

                if (gy >= 0 && gy < grid.size && gx >= 0 && gx < grid[gy].size) {
                    val isVisited = visited[gy][gx]
                    if (isVisited) {
                        val tile = grid[gy][gx]
                        val color = when (tile) {
                            DungeonGenerator.TILE_WALL -> Color(0xFF333333) // Wall
                            DungeonGenerator.TILE_START -> Color(0xFF6A5ACD) // Start violet
                            DungeonGenerator.TILE_EXIT -> Color(0xFF00FFFF)  // Turquoise Exit
                            else -> Color(0xFF1E1430) // standard Floor path color
                        }

                        drawRect(
                            color = color,
                            topLeft = Offset(screenX, screenY),
                            size = Size(cellSize - 1f, cellSize - 1f)
                        )

                        // If a chest is placed here and not open
                        val chest = chests.find { it.x == gx && it.y == gy }
                        if (chest != null && !chest.isOpen) {
                            drawCircle(
                                color = Color(0xFFFFD700),
                                radius = cellSize * 0.2f,
                                center = Offset(screenX + cellSize/2, screenY + cellSize/2)
                            )
                        }
                    } else {
                        // Unrevealed Fog
                        drawRect(
                            color = Color(0xFF07020E),
                            topLeft = Offset(screenX, screenY),
                            size = Size(cellSize - 1f, cellSize - 1f)
                        )
                    }
                } else {
                    // Out of bounds walls
                    drawRect(
                        color = Color(0xFF111111),
                        topLeft = Offset(screenX, screenY),
                        size = Size(cellSize - 1f, cellSize - 1f)
                    )
                }

                // If center of map, draw player arrow direction!
                if (dx == 0 && dy == 0) {
                    val pDirChar = when (playerDir) {
                        0 -> "▲"
                        1 -> "►"
                        2 -> "▼"
                        3 -> "◄"
                        else -> "●"
                    }
                    // Simple drawing of players flashing circle or arrow
                    drawCircle(
                        color = Color(0xFFFF69B4),
                        radius = cellSize * 0.35f,
                        center = Offset(screenX + cellSize/2, screenY + cellSize/2)
                    )
                    // Core white locator bulb
                    drawCircle(
                        color = Color.White,
                        radius = cellSize * 0.15f,
                        center = Offset(screenX + cellSize/2, screenY + cellSize/2)
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryOverlay(
    viewModel: GameViewModel,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(12.dp)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🎒 SCHOOL BACKPACK",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_bag_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close bag", tint = Color.White)
                }
            }

            Divider(color = Color.White.copy(alpha = 0.15f))

            // Characters status panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Level ${viewModel.playerLevel} Child", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("XP: ${viewModel.playerXp}/${viewModel.playerLevel * 60}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ATK: ${viewModel.playerAtkRating()}", color = Color(0xFFFF69B4), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("DEF: ${viewModel.playerDefRating()}", color = Color(0xFF00FFCC), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // EQUIPPED SLOTS ROW
            Text(
                "EQUIPPED STATUS",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EquipSlotCard("WEAPON", viewModel.equippedWeapon, onClick = { item -> viewModel.unequipItem(item) })
                EquipSlotCard("PAJAMAS", viewModel.equippedArmor, onClick = { item -> viewModel.unequipItem(item) })
                EquipSlotCard("MEMENTO", viewModel.equippedAccessory, onClick = { item -> viewModel.unequipItem(item) })
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BAG ITEMS
            Text(
                "CONTAINED ITEMS",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray)
            )
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (viewModel.inventory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items found. Open chests to find toys!", fontFamily = FontFamily.Monospace, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(viewModel.inventory) { item ->
                            LootItemRow(
                                item = item,
                                onClick = {
                                    if (item.type == ItemType.CONSUMABLE) {
                                        viewModel.useConsumable(item)
                                    } else {
                                        viewModel.equipItem(item)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Close actions
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RESUME CRAWL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RowScope.EquipSlotCard(
    slotName: String,
    item: LootItem?,
    onClick: (LootItem) -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(72.dp)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .clickable { item?.let { onClick(it) } },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(slotName, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
            if (item != null) {
                Text(
                    text = item.name,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = getRarityColor(item.rarity),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text("Unequip x", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = Color.LightGray)
            } else {
                Text("Empty", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun LootItemRow(
    item: LootItem,
    onClick: () -> Unit
) {
    val bonusStr = if (item.atkBonus > 0) " +${item.atkBonus} ATK"
        else if (item.defBonus > 0) " +${item.defBonus} DEF"
        else if (item.maxHpBonus > 0) " +${item.maxHpBonus} MAX HP"
        else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, getRarityColor(item.rarity).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${item.name} ${if (item.count > 1) "(${item.count})" else ""}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = getRarityColor(item.rarity)
            )
            Text(
                text = item.effectDescription.ifEmpty { "Equipment Item" },
                fontFamily = FontFamily.Monospace,
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bonusStr.isNotEmpty()) {
                Text(
                    text = bonusStr,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF00FFCC),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = if (item.type == ItemType.CONSUMABLE) "USE" else "EQUIP",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFFF69B4),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getRarityColor(rarity: com.example.data.model.Rarity): Color {
    return when (rarity) {
        com.example.data.model.Rarity.COMMON -> Color.White
        com.example.data.model.Rarity.RARE -> Color(0xFF1E90FF)     // Dodger Blue
        com.example.data.model.Rarity.DREAMY -> Color(0xFFFFD700)   // Gold
    }
}

private fun getDynamicIntensityColor(intensity: Float): Color {
    return when {
         intensity < 0.35f -> Color(0xFF00FFCC)
         intensity < 0.70f -> Color(0xFFFFFF00)
         else -> Color(0xFFFF0055)
    }
}

private fun getIntensityDescription(intensity: Float): String {
    return when {
        intensity < 0.15f -> "🟢 CALM CORRIDORS"
        intensity < 0.35f -> "💭 MINOR SNICKERS"
        intensity < 0.55f -> "📻 BULLY CLIQUE"
        intensity < 0.75f -> "🚨 HIGH TENSION"
        else -> "😱 CORNERED ANXIETY!"
    }
}
