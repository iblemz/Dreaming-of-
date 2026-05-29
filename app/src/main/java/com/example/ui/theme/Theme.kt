package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WarmGoldAccent,
    onPrimary = Color.Black,
    secondary = WarmGoldLight,
    onSecondary = Color.Black,
    tertiary = CoralHotAccent,
    background = ObsidianBlack,
    onBackground = IvoryWhiteText,
    surface = DarkSlateCard,
    onSurface = IvoryWhiteText,
    error = CoralHotAccent,
    onError = Color.Black,
    outline = SlateMutedLine
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
