package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Dark Theme (Exact palette requested)
// ==========================================
// Main background: #080D1B
val DarkMainBackground = Color(0xFF080D1B)
// Secondary background: #0F172A
val DarkSecondaryBackground = Color(0xFF0F172A)
// Card surfaces: #151F33
val DarkCardSurface = Color(0xFF151F33)
// Primary accent: #3B82F6
val DarkPrimaryAccent = Color(0xFF3B82F6)
// Secondary accent: #8B5CF6
val DarkSecondaryAccent = Color(0xFF8B5CF6)
// Primary text: #F8FAFC
val DarkPrimaryText = Color(0xFFF8FAFC)
// Secondary text: #A7B4C8
val DarkSecondaryText = Color(0xFFA7B4C8)
// Borders: #29364D
val DarkBorder = Color(0xFF29364D)

// ==========================================
// Light Theme (Exact palette requested)
// ==========================================
// Main background: #F6F8FC
val LightMainBackground = Color(0xFFF6F8FC)
// Secondary background: #FFFFFF
val LightSecondaryBackground = Color(0xFFFFFFFF)
// Card surfaces: #FFFFFF
val LightCardSurface = Color(0xFFFFFFFF)
// Primary accent: #2563EB
val LightPrimaryAccent = Color(0xFF2563EB)
// Secondary accent: #7C3AED
val LightSecondaryAccent = Color(0xFF7C3AED)
// Primary text: #172033
val LightPrimaryText = Color(0xFF172033)
// Secondary text: #526078
val LightSecondaryText = Color(0xFF526078)
// Borders: #E2E8F0
val LightBorder = Color(0xFFE2E8F0)

// ==========================================
// Semantic Verdict Colors (Accessible for both themes)
// ==========================================
val VerdictRealGreen = Color(0xFF10B981)
val VerdictRealGreenDark = Color(0xFF22C55E)
val VerdictRealGreenBgDark = Color(0xFF062D1C)
val VerdictRealGreenBgLight = Color(0xFFECFDF5)

val VerdictFakeRed = Color(0xFFEF4444)
val VerdictFakeRedDark = Color(0xFFF87171)
val VerdictFakeRedBgDark = Color(0xFF350E12)
val VerdictFakeRedBgLight = Color(0xFFFEF2F2)

val VerdictMisleadingOrange = Color(0xFFF59E0B)
val VerdictMisleadingOrangeBgDark = Color(0xFF382006)
val VerdictMisleadingOrangeBgLight = Color(0xFFFFFBEB)

val VerdictScamOrange = Color(0xFFF97316)
val VerdictScamOrangeBgDark = Color(0xFF381507)
val VerdictScamOrangeBgLight = Color(0xFFFFF7ED)

val VerdictUnverifiedGray = Color(0xFF94A3B8)
val VerdictUnverifiedGrayBgDark = Color(0xFF1E293B)
val VerdictUnverifiedGrayBgLight = Color(0xFFF1F5F9)

// Cyber branding accents
val CyberCyan = Color(0xFF06B6D4)
val CyberGlow = Color(0xFF60A5FA)

// Backward compatibility bindings
val DeepBackground = DarkMainBackground
val SurfaceDark = DarkSecondaryBackground
val SurfaceCard = DarkCardSurface
val SurfaceCardElevated = DarkBorder
val PrimaryPurple = DarkSecondaryAccent
val SecondaryCyan = DarkPrimaryAccent
val AccentViolet = DarkSecondaryAccent
val AccentBlue = DarkPrimaryAccent

val RealGreen = VerdictRealGreen
val RealGreenBg = VerdictRealGreenBgDark
val FakeRed = VerdictFakeRed
val FakeRedBg = VerdictFakeRedBgDark
val ScamOrange = VerdictScamOrange
val ScamOrangeBg = VerdictScamOrangeBgDark
val MisleadingOrange = VerdictMisleadingOrange
val MisleadingOrangeBg = VerdictMisleadingOrangeBgDark
val UnverifiedGray = VerdictUnverifiedGray
val UnverifiedGrayBg = VerdictUnverifiedGrayBgDark

val TextPrimary = DarkPrimaryText
val TextSecondary = DarkSecondaryText
val TextMuted = DarkSecondaryText
