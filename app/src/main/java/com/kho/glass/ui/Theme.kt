package com.kho.glass.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

enum class ThemeMode(val title: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Sáng"),
    DARK("Tối"),
    CUSTOM("Tùy chỉnh")
}

enum class BottomTabStyle(val title: String) {
    STANDARD("Kiểu hiện tại"),
    ROUNDED("Kiểu bo tròn")
}

data class AppAppearance(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val backgroundHex: String = "#F3F8FC",
    val buttonHex: String = "#0284C7",
    val secondaryHex: String = "#E8F2FA",
    val textHex: String = "#263238",
    val customTextEnabled: Boolean = false,
    val bottomTabStyle: BottomTabStyle = BottomTabStyle.STANDARD,
    val bottomTabHeightDp: Float = 60f,
    val bottomTabSpacingDp: Float = 10f,
    val glassEffectEnabled: Boolean = true
)

@Composable
fun KhoTheme(
    appearance: AppAppearance,
    content: @Composable () -> Unit
) {
    val dark = when (appearance.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.CUSTOM -> false
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = colorFromHex(appearance.buttonHex) ?: Color(0xFF0284C7)
    val secondaryAccent = if (
        appearance.secondaryHex.equals("#E8F2FA", ignoreCase = true) ||
        appearance.secondaryHex.equals("#E8E1F2", ignoreCase = true)
    ) {
        if (dark) Color(0xFF1E293B) else Color(0xFFE8F2FA)
    } else {
        colorFromHex(appearance.secondaryHex) ?: Color(0xFFE8F2FA)
    }
    val base = if (dark) darkColorScheme(
        primary = Color(0xFF7CCBFF),
        secondary = secondaryAccent,
        surfaceVariant = Color(0xFF25303C),
        surface = Color(0xFF101820),
        background = Color(0xFF0B1118)
    ) else lightColorScheme(
        primary = Color(0xFF0284C7),
        secondary = secondaryAccent,
        surfaceVariant = Color(0xFFE8E1F2),
        surface = Color(0xFFFAFCFE),
        background = Color(0xFFF3F8FC)
    )
    val customBackground = if (appearance.mode == ThemeMode.CUSTOM) {
        colorFromHex(appearance.backgroundHex)
    } else {
        null
    }
    val effectiveBackground = customBackground?.compositeOver(base.background) ?: base.background
    val textColor = if (appearance.customTextEnabled) {
        colorFromHex(appearance.textHex) ?: contrastingText(effectiveBackground)
    } else {
        contrastingText(effectiveBackground)
    }
    val scheme = base.copy(
        primary = accent,
        onPrimary = if (appearance.customTextEnabled) textColor else contrastingText(accent),
        secondary = secondaryAccent,
        onSurface = textColor,
        onSurfaceVariant = textColor,
        onSecondaryContainer = if (appearance.customTextEnabled) {
            textColor
        } else {
            contrastingText(secondaryAccent)
        },
        secondaryContainer = secondaryAccent,
        tertiary = textColor,
        surfaceVariant = secondaryAccent,
        surface = customBackground ?: base.surface,
        background = customBackground ?: base.background
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun contrastingText(background: Color): Color {
    fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }

    val luminance = 0.2126 * linear(background.red) +
        0.7152 * linear(background.green) +
        0.0722 * linear(background.blue)
    return if (luminance > 0.179) Color.Black else Color.White
}

fun colorFromHex(value: String): Color? =
    try {
        Color(android.graphics.Color.parseColor(value.trim()))
    } catch (_: IllegalArgumentException) {
        null
    }

fun normalizeHex(value: String): String? {
    val normalized = value.trim().let { if (it.startsWith("#")) it else "#$it" }
    return if (colorFromHex(normalized) != null && normalized.length in setOf(7, 9)) {
        normalized.uppercase()
    } else {
        null
    }
}
