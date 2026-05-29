package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenState

@Composable
fun RunSummaryScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val reason = viewModel.wakeUpReasonString
    val isVictory = reason.contains("Nurse's Office")
    val isAbnormal = reason.contains("Shocked Awake")

    val (headerText, accentColor) = when {
        isVictory -> Pair("🏥 REACHED THE NURSE'S OFFICE 🏥", Color(0xFF4CAF50))
        isAbnormal -> Pair("🚪 ABANDONED CORRIDOR RUN", Color(0xFF00FFFF))
        else -> Pair("😱 CORNERED & BEATEN UP BY BULLIES...", Color(0xFFFF0055))
    }

    val narrative = when {
        isVictory -> "You successfully stood up to the school bullies and negotiated all 4 floors to reach the safety of the Nurse's Office! Your resilience, confident boundaries, and healthy mind keep your self-esteem unshakable in the hallways."
        isAbnormal -> "You exited your corridor escape run abruptly. Take a deep breath to recompose your mind for next time."
        else -> "The school bullies cornered you and sapped all of your willpower! Do not worry; every day is a brand new chance to be resilient. With stronger boundaries and better strategy, you will push through to the Nurse's Office next time!"
    }

    val finalScore = (viewModel.currentFloor * 1000) + (viewModel.statsMobsDefeated * 250) + (viewModel.playerLevel * 300) + (viewModel.dreamShards * 10)

    val sumBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B0916),
            Color(0xFF1E1030),
            Color(0xFF060309)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(sumBg)
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header icon
            if (isVictory) {
                Icon(
                    Icons.Default.Favorite, 
                    contentDescription = null, 
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Icon(
                    Icons.Default.Warning, 
                    contentDescription = null, 
                    tint = Color(0xFFFF0055),
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text Header
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = accentColor,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Narrative Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(16.dp)
            ) {
                Text(
                    text = narrative,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.LightGray,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Justify
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Run Stats Grid Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "RUN CHRONICLE LOG",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Divider(color = Color.White.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Anxiety Realm Reached", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                        Text("Floor ${viewModel.currentFloor}", fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Anxieties Banished", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                        Text("${viewModel.statsMobsDefeated}", fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daytime Level Attained", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                        Text("Level ${viewModel.playerLevel}", fontFamily = FontFamily.Monospace, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Confidence Points Gained", fontFamily = FontFamily.Monospace, color = Color.LightGray)
                        Text("${viewModel.dreamShards}", fontFamily = FontFamily.Monospace, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }

                    Divider(color = Color.White.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("FINAL RECOVERY SCORE", fontFamily = FontFamily.Monospace, color = accentColor, fontWeight = FontWeight.Bold)
                        Text("$finalScore XP", fontFamily = FontFamily.Monospace, color = accentColor, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Button to return to title Screen
            Button(
                onClick = { viewModel.setScreen(ScreenState.TITLE) },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(52.dp)
                    .testTag("summary_close_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "RETURN TO CONSCIOUSNESS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
