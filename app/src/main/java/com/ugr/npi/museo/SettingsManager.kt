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

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Updates the selected language and persists it.
     * @param languageCode "es" for Spanish, "en" for English.
     */
    fun setLanguage(context: Context, languageCode: String) {
        getPreferences(context).edit().putString(KEY_LANGUAGE, languageCode).apply()
        // Locale update is handled via applyContext in the Activity lifecycle
    }

    fun getLanguage(context: Context): String {
        return getPreferences(context).getString(KEY_LANGUAGE, "es") ?: "es"
    }

    /**
     * Returns the Style Resource ID to be used in setTheme() calls.
     * Special handling for Colorblind mode (Deuteranopia) which requires a specific style
     * that overrides standard Night logic.
     */
    fun getThemeResId(context: Context): Int {
        val mode = getThemeMode(context) // 0: Standard, 1: Dark (via NightMode), 2: Deuteranopia
        return when (mode) {
             2 -> R.style.Theme_MuseoNPI_Deuteranopia
             else -> R.style.Theme_MuseoNPI
        }
    }

    fun setFontScale(context: Context, scale: Float) {
        getPreferences(context).edit().putFloat(KEY_FONT_Scale, scale).apply()
    }

    fun getFontScale(context: Context): Float {
        return getPreferences(context).getFloat(KEY_FONT_Scale, 1.0f)
    }

    fun setThemeMode(context: Context, mode: Int) {
        saveThemeMode(context, mode)
        applyTheme(mode)
    }

    private fun saveThemeMode(context: Context, mode: Int) {
        getPreferences(context).edit().putInt(KEY_THEME_MODE, mode).apply()
    }

    fun getThemeMode(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME_MODE, 0) // 0: Standard/Light
    }

    /**
     * Applies the Night Mode directly using AppCompatDelegate.
     * @param mode 0: Standard (Light), 1: Dark, 2: Colorblind (forced Light base)
     */
    fun applyTheme(mode: Int) {
        when (mode) {
            0 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            1 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) 
            2 -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) // Deuteranopia uses Light base + custom colors
            else -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    /**
     * Creates a new Context with the updated Configuration (Locale + Font Scale).
     * Must be called in attachBaseContext() o similar to ensure resources are loaded correctly.
     */
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

    // Helper for legacy support if needed
    private fun updateLocale(context: Context, languageCode: String) {}
}
