package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.ScreenState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()
                
                // Native android toast binding for custom game responses
                val context = LocalContext.current
                LaunchedEffect(gameViewModel.toastMessage) {
                    gameViewModel.toastMessage?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        gameViewModel.toastMessage = null
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Crossfade animation between dream world states under 300ms
                    Crossfade(
                        targetState = gameViewModel.screenState,
                        animationSpec = tween(durationMillis = 280),
                        label = "screen_transitions"
                    ) { state ->
                        when (state) {
                            ScreenState.TITLE -> {
                                TitleScreen(viewModel = gameViewModel)
                            }
                            ScreenState.EXPLORATION -> {
                                MainGameScreen(viewModel = gameViewModel)
                            }
                            ScreenState.COMBAT -> {
                                CombatScreen(viewModel = gameViewModel)
                            }
                            ScreenState.RUN_SUMMARY -> {
                                RunSummaryScreen(viewModel = gameViewModel)
                            }
                            else -> {
                                TitleScreen(viewModel = gameViewModel)
                            }
                        }
                    }

                    // Global reward/narrative dialog handler
                    gameViewModel.gameMessage?.let { message ->
                        AlertDialog(
                            onDismissRequest = { gameViewModel.dismissMessage() },
                            title = {
                                Text(
                                    text = "💭 DREAM ECHOES",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            },
                            text = {
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = { gameViewModel.dismissMessage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("UNDERSTOOD", fontFamily = FontFamily.Monospace)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            textContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
