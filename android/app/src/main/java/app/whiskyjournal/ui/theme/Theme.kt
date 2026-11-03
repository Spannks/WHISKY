package app.whiskyjournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFF8A4B0F)
private val AmberLight = Color(0xFFF2B36B)
private val Peat = Color(0xFF3E4A3D)

private val LightColors = lightColorScheme(
    primary = Amber,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2D1600),
    secondary = Peat,
    secondaryContainer = Color(0xFFD9E7D3),
    background = Color(0xFFFFFBF7),
    surface = Color(0xFFFFFBF7),
)

private val DarkColors = darkColorScheme(
    primary = AmberLight,
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3A00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFBDCBB7),
    secondaryContainer = Color(0xFF3E4A3D),
    background = Color(0xFF1A1410),
    surface = Color(0xFF1A1410),
)

@Composable
fun WhiskyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
