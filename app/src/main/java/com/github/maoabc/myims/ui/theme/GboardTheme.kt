package com.github.maoabc.myims.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class KeyboardColors(
    val keyboardBackground: Color,
    val keyBackground: Color,
    val keyPressedBackground: Color,
    val keyActionBackground: Color,
    val keyPrimaryActionBackground: Color,
    val keyText: Color,
    val keySecondaryText: Color,
    val keyActionText: Color,
    val keyPrimaryActionText: Color,
    val candidateBackground: Color,
    val candidateText: Color
)

private val LightKeyboardColors = KeyboardColors(
    keyboardBackground = Color(0xFFECEFF1),
    keyBackground = Color(0xFFFFFFFF),
    keyPressedBackground = Color(0xFFCFD8DC),
    keyActionBackground = Color(0xFFD6DCDE),
    keyPrimaryActionBackground = Color(0xFF1A73E8),
    keyText = Color(0xFF202124),
    keySecondaryText = Color(0xFF757575),
    keyActionText = Color(0xFF3C4043),
    keyPrimaryActionText = Color(0xFFFFFFFF),
    candidateBackground = Color(0xFFE8ECEF),
    candidateText = Color(0xFF1A73E8)
)

private val DarkKeyboardColors = KeyboardColors(
    keyboardBackground = Color(0xFF1E2124),
    keyBackground = Color(0xFF2D3135),
    keyPressedBackground = Color(0xFF42474C),
    keyActionBackground = Color(0xFF24272A),
    keyPrimaryActionBackground = Color(0xFF8AB4F8),
    keyText = Color(0xFFE8EAED),
    keySecondaryText = Color(0xFF9AA0A6),
    keyActionText = Color(0xFFE8EAED),
    keyPrimaryActionText = Color(0xFF202124),
    candidateBackground = Color(0xFF282C30),
    candidateText = Color(0xFF8AB4F8)
)

val LocalKeyboardColors = staticCompositionLocalOf { LightKeyboardColors }

@Composable
fun GboardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val keyboardColors = if (darkTheme) DarkKeyboardColors else LightKeyboardColors
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(
        LocalKeyboardColors provides keyboardColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
