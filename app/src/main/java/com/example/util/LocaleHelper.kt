package com.example.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import java.util.Locale

object LocaleHelper {

    fun getLocale(languageCode: String): Locale {
        return when (languageCode.lowercase()) {
            "hi" -> Locale.forLanguageTag("hi")
            "gu" -> Locale.forLanguageTag("gu")
            else -> Locale.forLanguageTag("en")
        }
    }

    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(android.app.LocaleManager::class.java)
                localeManager?.applicationLocales = android.os.LocaleList.forLanguageTags(languageCode)
            } catch (e: Exception) {
                // Ignore if not supported on runtime environment
            }
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        config.setLocales(localeList)

        return context.createConfigurationContext(config)
    }

    fun wrapContextForCompose(base: Context, languageCode: String): Context {
        val configContext = applyLocale(base, languageCode)
        return object : ContextWrapper(base), ActivityResultRegistryOwner {
            override fun getResources(): Resources = configContext.resources
            override fun getAssets(): AssetManager = configContext.assets
            override val activityResultRegistry: ActivityResultRegistry
                get() = (base as? ActivityResultRegistryOwner)?.activityResultRegistry
                    ?: (configContext as? ActivityResultRegistryOwner)?.activityResultRegistry
                    ?: error("Base context does not provide ActivityResultRegistry")
        }
    }

    fun getLocalizedConfiguration(baseConfig: Configuration, languageCode: String): Configuration {
        val locale = getLocale(languageCode)
        val config = Configuration(baseConfig)
        config.setLocale(locale)
        val localeList = LocaleList(locale)
        config.setLocales(localeList)
        return config
    }
}
