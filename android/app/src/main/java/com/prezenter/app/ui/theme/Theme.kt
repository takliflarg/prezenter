package com.prezenter.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrezenterBlue = Color(0xFF2F6FED)

private val LightColors = lightColorScheme(primary = PrezenterBlue)
private val DarkColors = darkColorScheme(primary = PrezenterBlue)

@Composable
fun PrezenterTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
