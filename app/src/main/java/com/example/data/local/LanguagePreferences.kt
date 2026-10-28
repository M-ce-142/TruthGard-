package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class LanguagePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("truthguard_language_prefs", Context.MODE_PRIVATE)

    var languageCode: String
        get() = prefs.getString(KEY_LANGUAGE_CODE, "en") ?: "en"
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE_CODE, value).apply()
        }

    companion object {
        private const val KEY_LANGUAGE_CODE = "key_app_language_code"
        const val LANG_ENGLISH = "en"
        const val LANG_HINDI = "hi"
        const val LANG_GUJARATI = "gu"
    }
}
