package org.mtopol.klju.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.mtopol.klju.Mark

/** Top bar color in both themes; matches the launcher icon background. */
val Burgundy = Color(0xFF7A1F2B)

private val LightColors = lightColorScheme(
    primary = Burgundy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADB),
    onPrimaryContainer = Color(0xFF40000F),
    secondary = Color(0xFF775656),
    tertiary = Color(0xFF755A2F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB2B6),
    onPrimary = Color(0xFF5E1120),
    primaryContainer = Color(0xFF5E1A25),
    onPrimaryContainer = Color(0xFFFFDADB),
    secondary = Color(0xFFE6BDBC),
    tertiary = Color(0xFFE5C18D),
)

@Composable
fun KljuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}

@Composable
fun Mark.color(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        Mark.CHECK -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
        Mark.CROSS -> if (dark) Color(0xFFEF9A9A) else Color(0xFFC62828)
        Mark.EYE -> if (dark) Color(0xFF90CAF9) else Color(0xFF1565C0)
        Mark.CIRCLE -> MaterialTheme.colorScheme.onSurface
        Mark.QUESTION -> if (dark) Color(0xFFFFCC80) else Color(0xFFE65100)
    }
}
