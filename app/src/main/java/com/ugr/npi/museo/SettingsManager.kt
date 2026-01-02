package com.ugr.npi.museo

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_FONT_Scale = "font_scale"
    private const val KEY_THEME_MODE = "theme_mode" // 0: Standard, 1: Colorblind, 2: Dark

    fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setLanguage(context: Context, languageCode: String) {
        getPreferences(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
        updateLocale(context, languageCode)
    }

    fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE, "es") ?: "es"
    }

    fun getThemeResId(context: Context): Int {
        val mode = getThemeMode(context) // 0: Standard, 1: Dark (via NightMode), 2: Deuteranopia
        return when (mode) {
             2 -> R.style.Theme_MuseoNPI_Deuteranopia
             else -> R.style.Theme_MuseoNPI
        }
    }

    fun setFontScale(context: Context, scale: Float) {
        getPreferences(context).edit().putFloat(KEY_FONT_Scale, scale).apply()
        // Note: Applying font scale dynamically usually requires recreating the activity or applying context wrapper
    }

    fun getFontScale(context: Context): Float {
        return getPreferences(context).getFloat(KEY_FONT_Scale, 1.0f)
    }

    fun setThemeMode(context: Context, mode: Int) {
        saveThemeMode(context, mode)
        applyTheme(mode)
    }

    fun saveThemeMode(context: Context, mode: Int) {
        getPreferences(context).edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, 0) // 0: Standard/Light
    }

    fun applyTheme(mode: Int) {
        when (mode) {
            0 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            1 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) // Dark
            2 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) // Colorblind (Custom Theme, Force Light base)
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    // Helper to get Context with updated config (locale + font)
    fun applyContext(context: Context): Context {
        val languageCode = getLanguage(context)
        val fontScale = getFontScale(context)
        
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.fontScale = fontScale
        
        return context.createConfigurationContext(config)
    }

    private fun updateLocale(context: Context, languageCode: String) {
        // ... (kept for compatibility or reference, but applyContext is better for ContextWrapper)
    }
}
