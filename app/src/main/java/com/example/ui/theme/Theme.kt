package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.local.AppThemeMode
import com.example.data.model.Verdict

// ==========================================
// Dark Color Scheme
// ==========================================
val DarkColorScheme = darkColorScheme(
    primary = DarkPrimaryAccent,             // #3B82F6
    onPrimary = Color.White,
    primaryContainer = DarkSecondaryBackground, // #0F172A
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkSecondaryAccent,         // #8B5CF6
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = CyberCyan,
    onTertiary = Color.Black,
    background = DarkMainBackground,         // #080D1B
    onBackground = DarkPrimaryText,          // #F8FAFC
    surface = DarkCardSurface,               // #151F33
    onSurface = DarkPrimaryText,             // #F8FAFC
    surfaceVariant = DarkSecondaryBackground, // #0F172A
    onSurfaceVariant = DarkSecondaryText,    // #A7B4C8
    outline = DarkBorder,                    // #29364D
    outlineVariant = Color(0xFF1E293B),
    error = VerdictFakeRedDark,
    onError = Color.White,
    errorContainer = VerdictFakeRedBgDark,
    onErrorContainer = VerdictFakeRedDark
)

// ==========================================
// Light Color Scheme
// ==========================================
val LightColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,            // #2563EB
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = LightSecondaryAccent,        // #7C3AED
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = LightMainBackground,        // #F6F8FC
    onBackground = LightPrimaryText,         // #172033
    surface = LightCardSurface,              // #FFFFFF
    onSurface = LightPrimaryText,            // #172033
    surfaceVariant = Color(0xFFF1F5F9),      // Secondary clean background
    onSurfaceVariant = LightSecondaryText,   // #526078
    outline = LightBorder,                   // #E2E8F0
    outlineVariant = Color(0xFFCBD5E1),
    error = VerdictFakeRed,
    onError = Color.White,
    errorContainer = VerdictFakeRedBgLight,
    onErrorContainer = VerdictFakeRed
)

// Extension to query whether the currently active scheme is dark
val ColorScheme.isAppDark: Boolean
    @Composable
    @ReadOnlyComposable
    get() = background == DarkMainBackground

// Extension to get verdict-specific color adapted for the active theme
@Composable
@ReadOnlyComposable
fun ColorScheme.verdictColor(verdict: Verdict): Color {
    val isDark = isAppDark
    return when (verdict) {
        Verdict.REAL -> if (isDark) VerdictRealGreenDark else VerdictRealGreen
        Verdict.FAKE -> if (isDark) VerdictFakeRedDark else VerdictFakeRed
        Verdict.MISLEADING -> VerdictMisleadingOrange
        Verdict.POTENTIAL_SCAM -> VerdictScamOrange
        Verdict.NOT_ENOUGH_EVIDENCE -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    }
}

// Extension to get verdict-specific background color adapted for the active theme
@Composable
@ReadOnlyComposable
fun ColorScheme.verdictBgColor(verdict: Verdict): Color {
    val isDark = isAppDark
    return when (verdict) {
        Verdict.REAL -> if (isDark) VerdictRealGreenBgDark else VerdictRealGreenBgLight
        Verdict.FAKE -> if (isDark) VerdictFakeRedBgDark else VerdictFakeRedBgLight
        Verdict.MISLEADING -> if (isDark) VerdictMisleadingOrangeBgDark else VerdictMisleadingOrangeBgLight
        Verdict.POTENTIAL_SCAM -> if (isDark) VerdictScamOrangeBgDark else VerdictScamOrangeBgLight
        Verdict.NOT_ENOUGH_EVIDENCE -> if (isDark) VerdictUnverifiedGrayBgDark else VerdictUnverifiedGrayBgLight
    }
}

// Adaptive cyber gradient for cards and headers
@Composable
@ReadOnlyComposable
fun ColorScheme.cyberGradient(): Brush {
    return if (isAppDark) {
        Brush.horizontalGradient(
            colors = listOf(DarkPrimaryAccent, DarkSecondaryAccent)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(LightPrimaryAccent, LightSecondaryAccent)
        )
    }
}

// Adaptive background gradient for splash and hero views
@Composable
@ReadOnlyComposable
fun ColorScheme.appBackgroundBrush(): Brush {
    return if (isAppDark) {
        Brush.verticalGradient(
            colors = listOf(
                DarkMainBackground,
                DarkSecondaryBackground,
                DarkMainBackground
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LightMainBackground,
                Color(0xFFEEF2F9),
                LightMainBackground
            )
        )
    }
}

@Composable
fun TruthGuardTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
