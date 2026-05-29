package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.data.model.ScoreHistory
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TitleScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val scores by viewModel.scoresFlow.collectAsState()
    var showHistory by remember { mutableStateOf(false) }

    val starryBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D0B1C),
            Color(0xFF1E1430),
            Color(0xFF10091D)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(starryBg)
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        if (!showHistory) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Main Title Hero Header
                Text(
                    text = "🌟 DREAM 🌟",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFDD0),
                        letterSpacing = 4.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "CRAWLER",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF69B4),
                        letterSpacing = 8.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Anxieties Lurk in the Toybox Nightmare",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 40.dp)
                )

                // Menu Actions
                Button(
                    onClick = { viewModel.startNewGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF69B4)),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(54.dp)
                        .testTag("new_game_button")
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "FALL ASLEEP (NEW RUN)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (viewModel.hasSavedGame) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.resumeGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A5ACD)),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(54.dp)
                            .testTag("resume_game_button")
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "RESUME DREAM",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showHistory = true },
                    border = BorderStroke(2.dp, Color(0xFF00FFFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FFFF)),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(54.dp)
                        .testTag("history_button")
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00FFFF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PAST AWAKENING LOGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Child sleeping vector decoration
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E1430).copy(alpha = 0.6f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "MEMENTO COLLECTED",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray),
                            )
                            Text(
                                "Morning Clock: Safe",
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        } else {
            // History of Scores list overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF00FFFF), RoundedCornerShape(16.dp))
                    .background(Color(0xFF090312).copy(alpha = 0.95f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🌙 THE CHRONICLES",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = Color(0xFF00FFFF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    IconButton(
                        onClick = { viewModel.deleteHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs", tint = Color.Red)
                    }
                }

                Text(
                    text = "A record of past runs and how you woke up",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Divider(color = Color(0xFF00FFFF).copy(alpha = 0.5f), thickness = 1.dp)

                if (scores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "You haven't slept and woken up yet.\nStart your first journey to find peace!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scores) { score ->
                            ScoreItemCard(score)
                        }
                    }
                }

                Button(
                    onClick = { showHistory = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "CLOSE LOGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreItemCard(score: ScoreHistory) {
    val dateStr = remember(score.datePlayed) {
        val df = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        df.format(Date(score.datePlayed))
    }

    val baseBorder = if (score.wakeUpReason.contains("Morning Light")) Color(0xFFFFD700) else Color(0xFFFF0055)
    val statusBg = if (score.wakeUpReason.contains("Morning Light")) Color(0xFFFFD700).copy(alpha = 0.12f) else Color(0xFFFF0055).copy(alpha = 0.12f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, baseBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = statusBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Score: ${score.score}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (score.wakeUpReason.contains("Morning Light")) Color(0xFFFFD700) else Color(0xFFFF0055))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Floor ${score.floorReached}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Wake-up: \"${score.wakeUpReason}\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (score.wakeUpReason.contains("Morning")) Color(0xFF90EE90) else Color(0xFFFFC0CB),
                    fontFamily = FontFamily.Monospace
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "👾 Defeated: ${score.mobsDefeated}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                )
                Text(
                    text = "🏆 Level: ${score.finalLevel}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray, fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}
