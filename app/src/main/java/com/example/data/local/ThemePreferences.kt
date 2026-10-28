package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

enum class AppThemeMode(val title: String, val description: String) {
    SYSTEM("System Default", "Follows your device system appearance"),
    LIGHT("Light", "Clean and crisp high-contrast light theme"),
    DARK("Dark", "Cybersecurity deep navy & glowing accents")
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("truthguard_theme_prefs", Context.MODE_PRIVATE)

    var themeMode: AppThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
            return try {
                AppThemeMode.valueOf(name)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    companion object {
        private const val KEY_THEME_MODE = "key_app_theme_mode"
    }
}
