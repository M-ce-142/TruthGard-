package com.example.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    val title: String,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Splash : Screen("splash", "Splash", R.string.nav_splash, Icons.Default.Security)
    object Dashboard : Screen("dashboard", "Dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    object VerifyNews : Screen("verify_news", "Verify Text", R.string.nav_verify_text, Icons.Default.VerifiedUser)
    object OcrVerify : Screen("ocr_verify", "Scan Image", R.string.nav_scan_image, Icons.Default.ImageSearch)
    object LinkVerify : Screen("link_verify", "Check Link", R.string.nav_check_link, Icons.Default.Link)
    object MessageVerify : Screen("message_verify", "Check Message", R.string.nav_check_message, Icons.AutoMirrored.Filled.Message)
    object AiAssistant : Screen("ai_assistant", "GuardBot", R.string.nav_guardbot, Icons.Default.Psychology)
    object LiveNews : Screen("live_news", "Live Alerts", R.string.nav_live_alerts, Icons.AutoMirrored.Filled.Article)
    object History : Screen("history", "History", R.string.nav_history, Icons.Default.History)
    object Settings : Screen("settings", "Settings", R.string.nav_settings, Icons.Default.Settings)
    object ComingSoon : Screen("coming_soon", "Coming Soon", R.string.nav_coming_soon, Icons.Default.Info)
}

val mainBottomNavItems = listOf(
    Screen.Dashboard,
    Screen.VerifyNews,
    Screen.OcrVerify,
    Screen.AiAssistant,
    Screen.History
)
