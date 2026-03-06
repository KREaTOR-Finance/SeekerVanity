package com.kreation.vanity

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeonGreen = Color(0xFF39FF14)
private val NearBlack = Color(0xFF050505)
private val CardDark = Color(0xFF0F0F0F)

private val VanityDarkColors = darkColorScheme(
    primary = NeonGreen,
    background = NearBlack,
    surface = CardDark,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun VanityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VanityDarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
